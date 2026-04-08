package com.gkim.im.android.feature.navigation

import androidx.activity.ComponentActivity
import android.net.Uri
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.media.MediaPickerController
import com.gkim.im.android.core.media.MediaPickerControllerFactory
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.rendering.MarkdownDocumentParser
import com.gkim.im.android.core.util.formatChatTimestamp
import com.gkim.im.android.data.remote.realtime.RealtimeChatClient
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.ContactsRepository
import com.gkim.im.android.data.repository.DefaultAigcRepository
import com.gkim.im.android.data.repository.DefaultContactsRepository
import com.gkim.im.android.data.repository.DefaultFeedRepository
import com.gkim.im.android.data.repository.FeedRepository
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.MessagingIntegrationPhase
import com.gkim.im.android.data.repository.MessagingIntegrationState
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.data.repository.presetProviders
import com.gkim.im.android.data.repository.seedContacts
import com.gkim.im.android.data.repository.seedConversations
import com.gkim.im.android.data.repository.seedPosts
import com.gkim.im.android.data.repository.seedPrompts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GkimRootAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rootShellDefaultsToChineseAndLightThemeOnFirstRun() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("gkim-theme-Light").fetchSemanticsNode()
        composeRule.onNodeWithText("消息").fetchSemanticsNode()
        composeRule.onNodeWithText("联系人").fetchSemanticsNode()
        composeRule.onNodeWithText("空间").fetchSemanticsNode()
    }

    @Test
    fun bottomNavigationSwitchesAcrossPrimaryTabs() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("bottom-nav").fetchSemanticsNode()
        composeRule.onNodeWithText("联系人").performClick()
        composeRule.onNodeWithTag("contacts-screen").fetchSemanticsNode()
        composeRule.onNodeWithText("空间").performClick()
        composeRule.onNodeWithTag("space-screen").fetchSemanticsNode()
    }

    @Test
    fun messagesScreenShowsEmptyStateWhenNoConversationsExist() {
        setApp(UiTestAppContainer(conversations = emptyList()))

        composeRule.onNodeWithTag("messages-empty").fetchSemanticsNode()
    }

    @Test
    fun messagesScreenStartsAtRecentConversationsWithoutUnreadSummaryPanel() {
        setApp(UiTestAppContainer())

        val headingBounds = composeRule.onNodeWithText("最近对话").fetchSemanticsNode().boundsInRoot
        val firstRowBounds = composeRule.onNodeWithTag("conversation-row-room-leo").fetchSemanticsNode().boundsInRoot

        assertTrue("headingTop=${headingBounds.top}", headingBounds.top < 160f)
        assertTrue(headingBounds.bottom <= firstRowBounds.top)
        assertTrue(!nodeExists("messages-unread-summary"))
        assertTrue(textNodeMissing("Signal Lattice"))
        assertTrue(textNodeMissing("Recent conversations, unread momentum, and a direct path into AIGC-assisted chats."))
        composeRule.onNodeWithTag("messages-list").fetchSemanticsNode()
    }

    @Test
    fun messagesConversationRowsHideUnreadBubbleBadgesButKeepCoreMetadata() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-name-room-leo", useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag("conversation-preview-room-leo", useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag("conversation-time-room-leo", useUnmergedTree = true).fetchSemanticsNode()
        val unreadBadgeExists = runCatching {
            composeRule.onNodeWithTag("conversation-unread-room-leo", useUnmergedTree = true).fetchSemanticsNode()
            true
        }.getOrDefault(false)
        assertTrue(!unreadBadgeExists)
    }

    @Test
    fun spaceScreenShowsUnreadSummaryAsSupportingContext() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("空间").performClick()

        val summaryBounds = composeRule.onNodeWithTag("space-unread-summary").fetchSemanticsNode().boundsInRoot
        val feedBounds = composeRule.onNodeWithTag("space-feed").fetchSemanticsNode().boundsInRoot

        assertTrue(summaryBounds.bottom <= feedBounds.top)
    }

    @Test
    fun spaceScreenMergesWorkshopDiscoveryIntoUnifiedFeed() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("空间").performClick()

        composeRule.onNodeWithTag("space-filter-for-you").fetchSemanticsNode()
        composeRule.onNodeWithTag("space-filter-prompting").fetchSemanticsNode()
        assertTrue(textNodeMissing("工作台"))
        composeRule.onNodeWithTag("space-feed-item-post-post-1").fetchSemanticsNode()
        composeRule.onNodeWithTag("space-feed").performScrollToNode(hasTestTag("space-feed-item-prompt-prompt-1"))
        composeRule.onNodeWithTag("space-feed-item-prompt-prompt-1").fetchSemanticsNode()

        composeRule.onNodeWithTag("space-filter-prompting").performClick()

        composeRule.onNodeWithTag("space-feed-item-prompt-prompt-1").fetchSemanticsNode()
        assertTrue(!nodeExists("space-feed-item-post-post-1"))
    }

    @Test
    fun spacePromptCardsApplyTemplatesIntoStudioChat() {
        val container = UiTestAppContainer()
        setApp(container)

        composeRule.onNodeWithText("空间").performClick()
        composeRule.onNodeWithTag("space-filter-prompting").performClick()
        composeRule.onNodeWithTag("space-apply-prompt-prompt-1").performClick()
        composeRule.waitUntil(5_000) {
            container.aigcRepository.draftRequest.value.prompt.startsWith("Create an editorial portrait")
        }

        composeRule.onNodeWithTag("chat-screen").fetchSemanticsNode()
        composeRule.onNodeWithText("Create an editorial portrait", substring = true).fetchSemanticsNode()
    }

    @Test
    fun chatScreenUsesCompactHeaderAndBackNavigation() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        composeRule.onNodeWithTag("chat-top-bar").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-back-button").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-contact-name").assertTextContains("Leo Vance")
        assertTrue(textNodeMissing("Active Room"))
        assertTrue(textNodeMissing("Back"))
        assertTrue(textNodeMissing("Workshop"))
        assertTrue(textNodeMissing("工作台"))

        composeRule.onNodeWithTag("chat-back-button").performClick()
        composeRule.onNodeWithTag("messages-screen").fetchSemanticsNode()
    }

    @Test
    fun openingConversationRequestsLiveHistoryLoad() {
        val messagingRepository = RecordingMessagingRepository()
        setApp(UiTestAppContainer(messagingRepository = messagingRepository))

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        composeRule.waitUntil(5_000) {
            messagingRepository.loadedConversationIds.contains("room-leo")
        }
    }

    @Test
    fun chatScreenUsesComposerRowForDefaultSendFlow() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        composeRule.onNodeWithTag("chat-plus-button").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-composer-input").performTextReplacement("Ship the revised orbit build.")
        composeRule.onNodeWithTag("chat-send-button").performClick()

        composeRule.onNodeWithText("Ship the revised orbit build.").fetchSemanticsNode()
        assertTrue(textNodeMissing("AIGC ACTIONS"))
    }

    @Test
    fun chatSecondaryMenuShowsAigcAndMediaActions() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()
        composeRule.onNodeWithTag("chat-plus-button").performClick()

        composeRule.onNodeWithTag("chat-secondary-menu").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-action-pick-image").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-action-pick-video").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-action-text-to-image").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-action-image-to-image").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-action-video-to-video").fetchSemanticsNode()
    }

    @Test
    fun chatSecondaryMenuActionsMapToExpectedChatBehavior() {
        val container = UiTestAppContainer()
        setApp(container, fakeMediaPickerControllerFactory())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()
        composeRule.onNodeWithTag("chat-composer-input").performTextReplacement("Turn this orbit frame into a cinematic cut.")

        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.onNodeWithTag("chat-action-pick-image").performClick()
        composeRule.onNodeWithText("Image ready").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-action-image-to-image").performClick()
        composeRule.waitUntil(5_000) {
            val latestTask = container.aigcRepository.history.value.firstOrNull()
            latestTask?.mode == AigcMode.ImageToImage && latestTask.input?.type == AttachmentType.Image
        }
        composeRule.onNodeWithText("ImageToImage · Turn this orbit frame into a cinematic cut.").fetchSemanticsNode()

        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.onNodeWithTag("chat-action-pick-video").performClick()
        composeRule.onNodeWithText("Video ready").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-action-video-to-video").performClick()
        composeRule.waitUntil(5_000) {
            val latestTask = container.aigcRepository.history.value.firstOrNull()
            latestTask?.mode == AigcMode.VideoToVideo && latestTask.input?.type == AttachmentType.Video
        }
        composeRule.onNodeWithText("VideoToVideo · Turn this orbit frame into a cinematic cut.").fetchSemanticsNode()

        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.onNodeWithTag("chat-action-text-to-image").performClick()
        composeRule.waitUntil(5_000) {
            val latestTask = container.aigcRepository.history.value.firstOrNull()
            latestTask?.mode == AigcMode.TextToImage && latestTask.input == null
        }
        composeRule.onNodeWithText("TextToImage · Turn this orbit frame into a cinematic cut.").fetchSemanticsNode()
    }

    @Test
    fun chatComposerStaysAnchoredToBottomWhenLatestGenerationAppears() {
        val container = UiTestAppContainer()
        setApp(container, fakeMediaPickerControllerFactory())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()
        composeRule.onNodeWithTag("chat-composer-input").performTextReplacement("Anchor the composer and render the latest frame.")
        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.onNodeWithTag("chat-action-text-to-image").performClick()

        composeRule.waitUntil(5_000) { container.aigcRepository.history.value.isNotEmpty() }
        composeRule.onNodeWithText("LATEST GENERATION").fetchSemanticsNode()

        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val composerBounds = composeRule.onNodeWithTag("chat-composer-row").fetchSemanticsNode().boundsInRoot
        val latestGenerationBounds = composeRule.onNodeWithText("LATEST GENERATION").fetchSemanticsNode().boundsInRoot
        val bottomGap = rootBounds.bottom - composerBounds.bottom

        assertTrue("bottomGap=$bottomGap", bottomGap <= 64f)
        assertTrue(
            "latestGenerationTop=${latestGenerationBounds.top} composerTop=${composerBounds.top}",
            latestGenerationBounds.top < composerBounds.top,
        )
    }

    @Test
    fun incomingMetadataPinsTimestampInsideBubbleFooterWhileKeepingSenderAboveBubble() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        val avatarBounds = composeRule.onNodeWithTag("chat-message-avatar-m-1").fetchSemanticsNode().boundsInRoot
        val senderBounds = composeRule.onNodeWithTag("chat-message-sender-m-1").fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeRule.onNodeWithTag("chat-message-body-m-1").fetchSemanticsNode().boundsInRoot
        val timeNode = composeRule.onNodeWithTag("chat-message-time-m-1")
        val timeBounds = timeNode.fetchSemanticsNode().boundsInRoot
        val bubbleBounds = composeRule.onNodeWithTag("chat-message-bubble-m-1").fetchSemanticsNode().boundsInRoot
        val footerGap = timeBounds.top - bodyBounds.bottom

        timeNode.assertTextContains(formatChatTimestamp("2026-04-06T13:42:00Z"))

        assertTrue(incomingAvatarAndSenderStayAheadOfBubble(avatarBounds.right, senderBounds.bottom, bubbleBounds.left, bubbleBounds.top))
        assertTrue(timeBounds.top >= bodyBounds.bottom)
        assertTrue("footerGap=$footerGap", footerGap <= 16f)
        assertTrue("bubbleRight=${bubbleBounds.right} timeRight=${timeBounds.right}", bubbleBounds.right - timeBounds.right <= 20f)
        assertTrue("bubbleBottom=${bubbleBounds.bottom} timeBottom=${timeBounds.bottom}", bubbleBounds.bottom - timeBounds.bottom <= 18f)
        assertTrue(timeBounds.left >= bubbleBounds.left)
        assertTrue(timeBounds.bottom <= bubbleBounds.bottom)
    }

    @Test
    fun chatTimelineKeepsIncomingAndSystemAttributionButDropsOutgoingSelfMarkers() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        composeRule.onNodeWithTag("chat-message-avatar-m-1").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-sender-m-1").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-bubble-m-1").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-bubble-m-2").fetchSemanticsNode()

        composeRule.onNodeWithTag("chat-message-sender-m-1").assertTextContains("Leo Vance")
        assertTrue(!nodeExists("chat-message-avatar-m-2"))
        assertTrue(!nodeExists("chat-message-sender-m-2"))
        assertTrue(textNodeMissing("You"))

        val incomingAvatar = composeRule.onNodeWithTag("chat-message-avatar-m-1").fetchSemanticsNode().boundsInRoot
        val incomingSender = composeRule.onNodeWithTag("chat-message-sender-m-1").fetchSemanticsNode().boundsInRoot
        val incomingBubble = composeRule.onNodeWithTag("chat-message-bubble-m-1").fetchSemanticsNode().boundsInRoot

        assertTrue(incomingAvatar.right <= incomingBubble.left)
        assertTrue(incomingSender.bottom <= incomingBubble.top)
    }

    @Test
    fun chatTimelineKeepsAttachmentAndTimestampForSystemMessages() {
        val container = UiTestAppContainer()
        setApp(container, fakeMediaPickerControllerFactory())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()
        composeRule.onNodeWithTag("chat-composer-input").performTextReplacement("Render the orbit feed as a polished hero frame.")
        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.onNodeWithTag("chat-action-text-to-image").performClick()

        composeRule.waitUntil(5_000) { container.aigcRepository.history.value.isNotEmpty() }

        val generatedMessageId = "aigc-${container.aigcRepository.history.value.first().id}"
        composeRule.onNodeWithTag("chat-message-avatar-$generatedMessageId").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-sender-$generatedMessageId").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-bubble-$generatedMessageId").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-attachment-$generatedMessageId").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-time-$generatedMessageId").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-sender-$generatedMessageId").assertTextContains("Aether System")
    }

    @Test
    fun chatTimelinePinsOutgoingTimestampToBubbleFooterWithoutChangingFormat() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        val bubbleBounds = composeRule.onNodeWithTag("chat-message-bubble-m-2").fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeRule.onNodeWithTag("chat-message-body-m-2").fetchSemanticsNode().boundsInRoot
        val timeNode = composeRule.onNodeWithTag("chat-message-time-m-2")
        val timeBounds = timeNode.fetchSemanticsNode().boundsInRoot

        timeNode.assertTextContains(formatChatTimestamp("2026-04-06T13:44:00Z"))
        val footerGap = timeBounds.top - bodyBounds.bottom

        assertTrue(timeBounds.top >= bodyBounds.bottom)
        assertTrue(timeBounds.right <= bubbleBounds.right)
        assertTrue(timeBounds.bottom <= bubbleBounds.bottom)
        assertTrue(timeBounds.left > bodyBounds.left)
        assertTrue("footerGap=$footerGap", footerGap <= 12f)
    }

    @Test
    fun shortOutgoingTextBubbleHugsContentWidthInsteadOfRowWidth() {
        setApp(UiTestAppContainer(conversations = adaptiveWidthConversations()))

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        val rowBounds = composeRule.onNodeWithTag("chat-message-row-m-short").fetchSemanticsNode().boundsInRoot
        val bubbleBounds = composeRule.onNodeWithTag("chat-message-bubble-m-short").fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeRule.onNodeWithTag("chat-message-body-m-short").fetchSemanticsNode().boundsInRoot
        val timeNode = composeRule.onNodeWithTag("chat-message-time-m-short")

        timeNode.assertTextContains(formatChatTimestamp("2026-04-06T13:45:00Z"))

        val availableWidth = rowBounds.right - rowBounds.left
        val bubbleWidth = bubbleBounds.right - bubbleBounds.left
        val bodyWidth = bodyBounds.right - bodyBounds.left

        assertTrue("bubbleWidth=$bubbleWidth availableWidth=$availableWidth", bubbleWidth < availableWidth * 0.55f)
        assertTrue("bubbleWidth=$bubbleWidth bodyWidth=$bodyWidth", bubbleWidth < bodyWidth + 140f)
    }

    @Test
    fun longOutgoingAndAttachmentRowsKeepReadableWidthAndStableFooter() {
        setApp(UiTestAppContainer(conversations = adaptiveWidthConversations()))

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        val shortBubbleBounds = composeRule.onNodeWithTag("chat-message-bubble-m-short").fetchSemanticsNode().boundsInRoot
        val longRowBounds = composeRule.onNodeWithTag("chat-message-row-m-long").fetchSemanticsNode().boundsInRoot
        val longBubbleBounds = composeRule.onNodeWithTag("chat-message-bubble-m-long").fetchSemanticsNode().boundsInRoot
        val longBodyBounds = composeRule.onNodeWithTag("chat-message-body-m-long").fetchSemanticsNode().boundsInRoot
        val longTimeBounds = composeRule.onNodeWithTag("chat-message-time-m-long").fetchSemanticsNode().boundsInRoot
        val attachmentBubbleBounds = composeRule.onNodeWithTag("chat-message-bubble-m-attachment").fetchSemanticsNode().boundsInRoot
        val attachmentBounds = composeRule.onNodeWithTag("chat-message-attachment-m-attachment").fetchSemanticsNode().boundsInRoot
        val attachmentTimeBounds = composeRule.onNodeWithTag("chat-message-time-m-attachment").fetchSemanticsNode().boundsInRoot

        val shortBubbleWidth = shortBubbleBounds.right - shortBubbleBounds.left
        val longBubbleWidth = longBubbleBounds.right - longBubbleBounds.left
        val longAvailableWidth = longRowBounds.right - longRowBounds.left
        val attachmentBubbleWidth = attachmentBubbleBounds.right - attachmentBubbleBounds.left
        val attachmentWidth = attachmentBounds.right - attachmentBounds.left

        assertTrue("shortBubbleWidth=$shortBubbleWidth longBubbleWidth=$longBubbleWidth", longBubbleWidth > shortBubbleWidth + 120f)
        assertTrue("longBubbleWidth=$longBubbleWidth longAvailableWidth=$longAvailableWidth", longBubbleWidth < longAvailableWidth * 0.9f)
        assertTrue(longTimeBounds.top >= longBodyBounds.bottom)
        assertTrue(longTimeBounds.right <= longBubbleBounds.right)
        assertTrue(longTimeBounds.bottom <= longBubbleBounds.bottom)

        assertTrue("attachmentBubbleWidth=$attachmentBubbleWidth shortBubbleWidth=$shortBubbleWidth", attachmentBubbleWidth > shortBubbleWidth + 120f)
        assertTrue("attachmentWidth=$attachmentWidth", attachmentWidth > 220f)
        assertTrue(attachmentBounds.right <= attachmentBubbleBounds.right)
        assertTrue(attachmentTimeBounds.right <= attachmentBubbleBounds.right)
        assertTrue(attachmentTimeBounds.bottom <= attachmentBubbleBounds.bottom)
    }

    @Test
    fun contactsScreenUsesSingleDropdownSortControl() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("联系人").performClick()

        composeRule.onNodeWithTag("contact-sort-dropdown").fetchSemanticsNode()
        assertTrue(!nodeExists("contact-sort-Nickname"))
        assertTrue(!nodeExists("contact-sort-AddedAscending"))
        assertTrue(!nodeExists("contact-sort-AddedDescending"))
    }

    @Test
    fun contactSortingChangesRenderedRowOrder() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("联系人").performClick()
        composeRule.onNodeWithTag("contact-sort-dropdown").performClick()
        composeRule.onNodeWithTag("contact-sort-option-AddedDescending").performClick()
        composeRule.waitUntil(5_000) { nodeExists("contact-row-clara-wu") && nodeExists("contact-row-aria-thorne") }

        val claraTop = composeRule.onNodeWithTag("contact-row-clara-wu").fetchSemanticsNode().boundsInRoot.top
        val ariaTop = composeRule.onNodeWithTag("contact-row-aria-thorne").fetchSemanticsNode().boundsInRoot.top

        assertTrue(claraTop < ariaTop)
    }

    @Test
    fun settingsMenuPresentsFocusedEntriesAndAccountActionsSurface() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("设置").performClick()

        composeRule.onNodeWithTag("settings-menu-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-menu-appearance").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-menu-ai-provider").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-menu-im-validation").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-menu-account").fetchSemanticsNode()
        assertTrue(!nodeExists("settings-base-url"))
        assertTrue(!nodeExists("settings-im-http-base-url"))

        composeRule.onNodeWithTag("settings-menu-account").performClick()

        composeRule.onNodeWithTag("settings-detail-account").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-account-login").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-account-register").fetchSemanticsNode()
    }

    @Test
    fun settingsInteractionsUpdateProviderConfiguration() {
        val container = UiTestAppContainer()
        setApp(container)

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithTag("settings-menu-ai-provider").performClick()
        composeRule.onNodeWithTag("settings-detail-ai-provider").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-provider-custom").performClick()
        composeRule.waitUntil(5_000) { container.aigcRepository.activeProviderId.value == "custom" }

        composeRule.onNodeWithTag("settings-base-url").performTextReplacement("https://gateway.example.com/v1")
        composeRule.onNodeWithTag("settings-model").performTextReplacement("gpt-image-1")
        composeRule.onNodeWithTag("settings-api-key").performTextReplacement("secret-token")
        composeRule.waitUntil(5_000) {
            container.aigcRepository.customProvider.value.baseUrl == "https://gateway.example.com/v1" &&
                container.aigcRepository.customProvider.value.model == "gpt-image-1"
        }

        assertEquals("custom", container.aigcRepository.activeProviderId.value)
        assertEquals("https://gateway.example.com/v1", container.aigcRepository.customProvider.value.baseUrl)
        assertEquals("gpt-image-1", container.aigcRepository.customProvider.value.model)
    }

    @Test
    fun settingsScreenAppliesLanguageAndThemePreferencesAcrossShell() {
        val container = UiTestAppContainer()
        setApp(container)

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithTag("settings-menu-appearance").performClick()
        composeRule.onNodeWithTag("settings-detail-appearance").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-language-chinese").performClick()
        composeRule.onNodeWithTag("settings-theme-light").performClick()
        composeRule.waitUntil(5_000) {
            container.preferencesStore.currentLanguage == com.gkim.im.android.core.model.AppLanguage.Chinese &&
                container.preferencesStore.currentThemeMode == com.gkim.im.android.core.model.AppThemeMode.Light
        }

        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(5_000) { nodeExists("settings-menu-screen") }
        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(5_000) { nodeExists("messages-screen") }

        composeRule.onNodeWithTag("gkim-theme-Light").fetchSemanticsNode()
        composeRule.onNodeWithText("消息").fetchSemanticsNode()
    }

    @Test
    fun settingsScreenExposesImBackendValidationControlsAndStatus() {
        val container = UiTestAppContainer()
        setApp(container)

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithTag("settings-menu-im-validation").performClick()
        composeRule.onNodeWithTag("settings-detail-im-validation").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-im-http-base-url").performTextReplacement("https://forward.example.com/")
        composeRule.onNodeWithTag("settings-im-websocket-url").performTextReplacement("wss://forward.example.com/ws")
        composeRule.onNodeWithTag("settings-im-dev-user").performTextReplacement("leo-vance")
        composeRule.waitUntil(5_000) {
            container.preferencesStore.currentImHttpBaseUrl == "https://forward.example.com/" &&
                container.preferencesStore.currentImWebSocketUrl == "wss://forward.example.com/ws" &&
                container.preferencesStore.currentImDevUserExternalId == "leo-vance"
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("已准备好进行 IM 验证").fetchSemanticsNode()

        composeRule.onNodeWithTag("settings-im-dev-user").performTextClearance()
        composeRule.waitUntil(5_000) { container.preferencesStore.currentImDevUserExternalId.isEmpty() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("IM validation config is incomplete or invalid.").fetchSemanticsNode()
    }

    private fun setApp(
        container: UiTestAppContainer,
        mediaPickerControllerFactory: MediaPickerControllerFactory? = null,
    ) {
        composeRule.setContent {
            GkimRootApp(
                container = container,
                mediaPickerControllerFactory = mediaPickerControllerFactory,
            )
        }
    }

    private fun fakeMediaPickerControllerFactory(): MediaPickerControllerFactory = { onMediaSelected ->
        MediaPickerController(
            pickImage = { onMediaSelected(MediaInput(AttachmentType.Image, Uri.parse("content://ui-test/fake-image"))) },
            pickVideo = { onMediaSelected(MediaInput(AttachmentType.Video, Uri.parse("content://ui-test/fake-video"))) },
        )
    }

    private fun nodeExists(tag: String): Boolean = runCatching {
        composeRule.onNodeWithTag(tag).fetchSemanticsNode()
        true
    }.getOrDefault(false)

    private fun textNodeMissing(text: String): Boolean = runCatching {
        composeRule.onNodeWithText(text).fetchSemanticsNode()
        false
    }.getOrDefault(true)

    private fun incomingAvatarAndSenderStayAheadOfBubble(
        avatarRight: Float,
        senderBottom: Float,
        bubbleLeft: Float,
        bubbleTop: Float,
    ): Boolean = avatarRight <= bubbleLeft && senderBottom <= bubbleTop
}

private fun adaptiveWidthConversations(): List<com.gkim.im.android.core.model.Conversation> =
    seedConversations.map { conversation ->
        if (conversation.id != "room-leo") {
            conversation
        } else {
            conversation.copy(
                lastMessage = "OK",
                lastTimestamp = "2026-04-06T13:47:00Z",
                messages = listOf(
                    com.gkim.im.android.core.model.ChatMessage(
                        id = "m-1",
                        direction = com.gkim.im.android.core.model.MessageDirection.Incoming,
                        kind = com.gkim.im.android.core.model.MessageKind.Text,
                        body = "The orbital thread is stable. Ready for review.",
                        createdAt = "2026-04-06T13:42:00Z",
                    ),
                    com.gkim.im.android.core.model.ChatMessage(
                        id = "m-short",
                        direction = com.gkim.im.android.core.model.MessageDirection.Outgoing,
                        kind = com.gkim.im.android.core.model.MessageKind.Text,
                        body = "OK",
                        createdAt = "2026-04-06T13:45:00Z",
                    ),
                    com.gkim.im.android.core.model.ChatMessage(
                        id = "m-long",
                        direction = com.gkim.im.android.core.model.MessageDirection.Outgoing,
                        kind = com.gkim.im.android.core.model.MessageKind.Text,
                        body = "Push the AIGC moodboard into the workshop, sync the feed, and leave the notes inline so the next review pass has full context.",
                        createdAt = "2026-04-06T13:46:00Z",
                    ),
                    com.gkim.im.android.core.model.ChatMessage(
                        id = "m-attachment",
                        direction = com.gkim.im.android.core.model.MessageDirection.Outgoing,
                        kind = com.gkim.im.android.core.model.MessageKind.Aigc,
                        body = "Here is the render reference.",
                        createdAt = "2026-04-06T13:47:00Z",
                        attachment = com.gkim.im.android.core.model.MessageAttachment(
                            type = com.gkim.im.android.core.model.AttachmentType.Image,
                            preview = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80",
                        ),
                    ),
                ),
            )
        }
    }

private class UiTestAppContainer(
    conversations: List<com.gkim.im.android.core.model.Conversation> = seedConversations,
    override val messagingRepository: MessagingRepository = InMemoryMessagingRepository(conversations),
) : AppContainer {
    override val preferencesStore = UiTestPreferencesStore()
    private val secureStore = UiInMemorySecureStore()

    override val contactsRepository: ContactsRepository = DefaultContactsRepository(seedContacts, preferencesStore, Dispatchers.Main)
    override val feedRepository: FeedRepository = DefaultFeedRepository(seedPosts, seedPrompts, Dispatchers.Main)
    override val aigcRepository: AigcRepository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, Dispatchers.Main)
    override val markdownParser: MarkdownDocumentParser = MarkdownDocumentParser()
    override val realtimeChatClient: RealtimeChatClient = RealtimeChatClient(OkHttpClient.Builder().build(), "wss://example.com/realtime")
}

private class RecordingMessagingRepository(
    private val delegate: MessagingRepository = InMemoryMessagingRepository(seedConversations),
) : MessagingRepository by delegate {
    private val integrationStateValue = MutableStateFlow(
        MessagingIntegrationState(phase = MessagingIntegrationPhase.Ready)
    )

    val loadedConversationIds = mutableListOf<String>()

    override val integrationState: StateFlow<MessagingIntegrationState> = integrationStateValue

    override fun loadConversationHistory(conversationId: String) {
        loadedConversationIds += conversationId
        delegate.loadConversationHistory(conversationId)
    }
}
