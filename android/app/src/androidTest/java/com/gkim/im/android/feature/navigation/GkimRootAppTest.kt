package com.gkim.im.android.feature.navigation

import androidx.activity.ComponentActivity
import android.net.Uri
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.media.MediaPickerController
import com.gkim.im.android.core.media.MediaPickerControllerFactory
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.rendering.MarkdownDocumentParser
import com.gkim.im.android.data.remote.realtime.RealtimeChatClient
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.ContactsRepository
import com.gkim.im.android.data.repository.DefaultAigcRepository
import com.gkim.im.android.data.repository.DefaultContactsRepository
import com.gkim.im.android.data.repository.DefaultFeedRepository
import com.gkim.im.android.data.repository.FeedRepository
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.data.repository.presetProviders
import com.gkim.im.android.data.repository.seedContacts
import com.gkim.im.android.data.repository.seedConversations
import com.gkim.im.android.data.repository.seedPosts
import com.gkim.im.android.data.repository.seedPrompts
import kotlinx.coroutines.Dispatchers
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
    fun bottomNavigationSwitchesAcrossPrimaryTabs() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("bottom-nav").fetchSemanticsNode()
        composeRule.onNodeWithText("Contacts").performClick()
        composeRule.onNodeWithTag("contacts-screen").fetchSemanticsNode()
        composeRule.onNodeWithText("Space").performClick()
        composeRule.onNodeWithTag("space-screen").fetchSemanticsNode()
    }

    @Test
    fun messagesScreenShowsEmptyStateWhenNoConversationsExist() {
        setApp(UiTestAppContainer(conversations = emptyList()))

        composeRule.onNodeWithTag("messages-empty").fetchSemanticsNode()
    }

    @Test
    fun messagesScreenUsesCompactUnreadSummaryAboveConversationList() {
        setApp(UiTestAppContainer())

        val summaryBounds = composeRule.onNodeWithTag("messages-unread-summary").fetchSemanticsNode().boundsInRoot
        val firstRowBounds = composeRule.onNodeWithTag("conversation-row-room-leo").fetchSemanticsNode().boundsInRoot

        assertTrue(summaryBounds.height < firstRowBounds.height)
        assertTrue(textNodeMissing("UNREAD PULSE"))
        composeRule.onNodeWithTag("messages-list").fetchSemanticsNode()
    }

    @Test
    fun messagesConversationRowsStillExposeUnreadMetadata() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-name-room-leo", useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag("conversation-preview-room-leo", useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag("conversation-time-room-leo", useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithTag("conversation-unread-room-leo", useUnmergedTree = true).fetchSemanticsNode()
    }

    @Test
    fun chatScreenUsesCompactHeaderAndBackNavigation() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        composeRule.onNodeWithTag("chat-top-bar").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-back-button").fetchSemanticsNode()
        composeRule.onNodeWithText("Leo Vance").fetchSemanticsNode()
        assertTrue(textNodeMissing("Active Room"))
        assertTrue(textNodeMissing("Back"))

        composeRule.onNodeWithTag("chat-back-button").performClick()
        composeRule.onNodeWithTag("messages-screen").fetchSemanticsNode()
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
    fun chatTimelineUsesAvatarLedRowsForIncomingAndOutgoingMessages() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        composeRule.onNodeWithTag("chat-message-avatar-m-1").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-sender-m-1").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-bubble-m-1").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-avatar-m-2").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-sender-m-2").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-message-bubble-m-2").fetchSemanticsNode()

        composeRule.onNodeWithTag("chat-message-sender-m-1").assertTextContains("Leo Vance")
        composeRule.onNodeWithTag("chat-message-sender-m-2").assertTextContains("You")

        val incomingAvatar = composeRule.onNodeWithTag("chat-message-avatar-m-1").fetchSemanticsNode().boundsInRoot
        val incomingSender = composeRule.onNodeWithTag("chat-message-sender-m-1").fetchSemanticsNode().boundsInRoot
        val incomingBubble = composeRule.onNodeWithTag("chat-message-bubble-m-1").fetchSemanticsNode().boundsInRoot
        val outgoingAvatar = composeRule.onNodeWithTag("chat-message-avatar-m-2").fetchSemanticsNode().boundsInRoot
        val outgoingSender = composeRule.onNodeWithTag("chat-message-sender-m-2").fetchSemanticsNode().boundsInRoot
        val outgoingBubble = composeRule.onNodeWithTag("chat-message-bubble-m-2").fetchSemanticsNode().boundsInRoot

        assertTrue(incomingAvatar.right <= incomingBubble.left)
        assertTrue(incomingSender.bottom <= incomingBubble.top)
        assertTrue(outgoingAvatar.right <= outgoingBubble.left)
        assertTrue(outgoingSender.bottom <= outgoingBubble.top)
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
    fun contactSortingChangesRenderedRowOrder() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("Contacts").performClick()
        composeRule.onNodeWithTag("contact-sort-AddedDescending").performClick()
        composeRule.waitUntil(5_000) { nodeExists("contact-row-clara-wu") && nodeExists("contact-row-aria-thorne") }

        val claraTop = composeRule.onNodeWithTag("contact-row-clara-wu").fetchSemanticsNode().boundsInRoot.top
        val ariaTop = composeRule.onNodeWithTag("contact-row-aria-thorne").fetchSemanticsNode().boundsInRoot.top

        assertTrue(claraTop < ariaTop)
    }

    @Test
    fun settingsInteractionsUpdateProviderConfiguration() {
        val container = UiTestAppContainer()
        setApp(container)

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithTag("settings-screen").fetchSemanticsNode()
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
}

private class UiTestAppContainer(
    conversations: List<com.gkim.im.android.core.model.Conversation> = seedConversations,
) : AppContainer {
    private val preferencesStore = UiTestPreferencesStore()
    private val secureStore = UiInMemorySecureStore()

    override val messagingRepository: MessagingRepository = InMemoryMessagingRepository(conversations)
    override val contactsRepository: ContactsRepository = DefaultContactsRepository(seedContacts, preferencesStore, Dispatchers.Main)
    override val feedRepository: FeedRepository = DefaultFeedRepository(seedPosts, seedPrompts, Dispatchers.Main)
    override val aigcRepository: AigcRepository = DefaultAigcRepository(presetProviders, preferencesStore, secureStore, Dispatchers.Main)
    override val markdownParser: MarkdownDocumentParser = MarkdownDocumentParser()
    override val realtimeChatClient: RealtimeChatClient = RealtimeChatClient(OkHttpClient.Builder().build(), "wss://example.com/realtime")
}
