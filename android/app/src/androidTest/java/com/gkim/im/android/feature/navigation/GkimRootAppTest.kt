package com.gkim.im.android.feature.navigation

import androidx.activity.ComponentActivity
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import com.gkim.im.android.core.media.MediaPickerController
import com.gkim.im.android.core.media.MediaPickerControllerFactory
import com.gkim.im.android.core.media.GeneratedImageSaveResult
import com.gkim.im.android.core.media.GeneratedImageSaver
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.MediaInput
import com.gkim.im.android.core.model.TaskStatus
import com.gkim.im.android.core.rendering.MarkdownDocumentParser
import com.gkim.im.android.core.util.formatChatTimestamp
import com.gkim.im.android.data.local.SessionStore
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BackendUserDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.ContactProfileDto
import com.gkim.im.android.data.remote.im.ConversationSummaryDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImBackendHttpClient
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.MessageRecordDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import com.gkim.im.android.data.remote.aigc.RemoteAigcProviderClient
import com.gkim.im.android.data.remote.realtime.RealtimeChatClient
import com.gkim.im.android.data.repository.AigcRepository
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.CompanionMemoryRepository
import com.gkim.im.android.data.repository.CompanionPresetRepository
import com.gkim.im.android.data.repository.CompanionRosterRepository
import com.gkim.im.android.data.repository.CompanionSkinRepository
import com.gkim.im.android.data.repository.CompanionTurnRepository
import com.gkim.im.android.data.repository.InMemoryCompanionSkinRepository
import com.gkim.im.android.data.repository.DefaultCompanionMemoryRepository
import com.gkim.im.android.data.repository.DefaultCompanionPresetRepository
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.data.repository.DefaultCardInteropRepository
import com.gkim.im.android.data.repository.LiveCardInteropRepository
import com.gkim.im.android.data.repository.ContactsRepository
import com.gkim.im.android.data.repository.DefaultAigcRepository
import com.gkim.im.android.data.repository.DefaultCompanionRosterRepository
import com.gkim.im.android.data.repository.DefaultCompanionTurnRepository
import com.gkim.im.android.data.repository.DefaultContactsRepository
import com.gkim.im.android.data.repository.DefaultFeedRepository
import com.gkim.im.android.data.repository.DefaultUserPersonaRepository
import com.gkim.im.android.data.repository.FeedRepository
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import com.gkim.im.android.data.repository.MessagingIntegrationPhase
import com.gkim.im.android.data.repository.MessagingIntegrationState
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.data.repository.UserPersonaRepository
import com.gkim.im.android.data.repository.DefaultWorldInfoRepository
import com.gkim.im.android.data.repository.WorldInfoRepository
import com.gkim.im.android.data.repository.presetProviders
import com.gkim.im.android.data.repository.seedBuiltInPersonas
import com.gkim.im.android.data.repository.seedDrawPoolCharacters
import com.gkim.im.android.data.repository.seedContacts
import com.gkim.im.android.data.repository.seedConversations
import com.gkim.im.android.data.repository.seedPresetCharacters
import com.gkim.im.android.data.repository.seedPosts
import com.gkim.im.android.data.repository.seedPrompts
import com.gkim.im.android.feature.qr.QrScannerController
import com.gkim.im.android.feature.qr.QrScannerControllerFactory
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
    fun launchShowsWelcomeOnboardingBeforeMainShell() {
        setApp(UiTestAppContainer(), initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.onNodeWithTag("welcome-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("welcome-video").fetchSemanticsNode()
        composeRule.onNodeWithTag("welcome-native-atmosphere").fetchSemanticsNode()
        composeRule.onNodeWithTag("welcome-hero-content").fetchSemanticsNode()
        composeRule.onNodeWithTag("welcome-login-button").fetchSemanticsNode()
        composeRule.onNodeWithTag("welcome-register-button").fetchSemanticsNode()
        assertTrue(!nodeExists("bottom-nav"))
    }

    @Test
    fun welcomeScreenUsesSimplifiedProductCopyWithoutTechnicalHelperText() {
        setApp(UiTestAppContainer(), initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.onNodeWithText("和朋友聊天、分享灵感，把常用的沟通都放在一个地方。").fetchSemanticsNode()
        assertTrue(textNodeMissing("一个以建筑感为先的社交壳层，把实时消息、提示工程与工程文化合成在同一个入口。"))
        assertTrue(textNodeMissing("使用账号凭据进入实时 IM 壳层，并让开屏动效持续在原生界面后方保持可见。"))
        assertTrue(textNodeMissing("加密连接"))
    }

    @Test
    fun welcomeVideoReportsActivePlaybackStateOnLaunch() {
        setApp(UiTestAppContainer(), initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.waitUntil(10_000) { nodeExists("welcome-video-state-playing") }

        composeRule.onNodeWithTag("welcome-video-state-playing").fetchSemanticsNode()
        assertTrue(!nodeExists("welcome-video-state-fallback"))
    }

    @Test
    fun welcomeVideoRecoversPlayingStateAfterActivityResume() {
        setApp(UiTestAppContainer(), initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.waitUntil(10_000) { nodeExists("welcome-video-state-playing") }

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.waitUntil(10_000) { nodeExists("welcome-video-state-playing") }
        assertTrue(!nodeExists("welcome-video-state-fallback"))
    }

    @Test
    fun welcomeLoginActionOpensLoginRouteInsteadOfPreviewShell() {
        setApp(UiTestAppContainer(), initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.onNodeWithTag("welcome-login-button").performClick()

        composeRule.onNodeWithTag("login-screen").fetchSemanticsNode()
        assertTrue(!nodeExists("bottom-nav"))
    }

    @Test
    fun registerRouteSubmitsCredentialsPersistsSessionAndEntersShell() {
        val backendClient = RecordingImBackendClient()
        val container = UiTestAppContainer(imBackendClient = backendClient)
        setApp(container, initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.onNodeWithTag("welcome-register-button").performClick()
        composeRule.onNodeWithTag("register-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("register-username").performTextReplacement("nova_user")
        composeRule.onNodeWithTag("register-display-name").performTextReplacement("Nova User")
        composeRule.onNodeWithTag("register-password").performTextReplacement("passw0rd!")
        composeRule.onNodeWithTag("register-submit").performClick()

        composeRule.waitUntil(5_000) {
            backendClient.registerCalls.size == 1 && nodeExists("bottom-nav")
        }

        val registerCall = backendClient.registerCalls.single()
        assertEquals("nova_user", registerCall.username)
        assertEquals("Nova User", registerCall.displayName)
        assertEquals("auth-token-register", container.sessionStore.token)
        assertEquals("nova_user", container.sessionStore.username)
    }

    @Test
    fun loginRouteShowsInlineErrorWhenBackendRejectsCredentials() {
        val backendClient = RecordingImBackendClient(
            loginFailure = IllegalStateException("401 unauthorized"),
        )
        val container = UiTestAppContainer(imBackendClient = backendClient)
        setApp(container, initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.onNodeWithTag("welcome-login-button").performClick()
        composeRule.onNodeWithTag("login-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("login-username").performTextReplacement("nova_user")
        composeRule.onNodeWithTag("login-password").performTextReplacement("wrong-pass")
        composeRule.onNodeWithTag("login-submit").performClick()

        composeRule.waitUntil(5_000) {
            backendClient.loginCalls.size == 1 && nodeExists("login-error")
        }

        composeRule.onNodeWithTag("login-error").assertTextContains("用户名或密码错误")
        assertTrue(!nodeExists("bottom-nav"))
        assertTrue(container.sessionStore.token.isNullOrBlank())
    }

    @Test
    fun loginBackAffordanceReturnsToWelcomeSurface() {
        setApp(UiTestAppContainer(), initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.onNodeWithTag("welcome-login-button").performClick()
        composeRule.onNodeWithTag("login-screen").fetchSemanticsNode()

        composeRule.onNodeWithTag("login-back").performClick()

        composeRule.waitUntil(5_000) { nodeExists("welcome-screen") }
        assertTrue(!nodeExists("login-screen"))
    }

    @Test
    fun registerBackAffordanceReturnsToWelcomeSurface() {
        setApp(UiTestAppContainer(), initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.onNodeWithTag("welcome-register-button").performClick()
        composeRule.onNodeWithTag("register-screen").fetchSemanticsNode()

        composeRule.onNodeWithTag("register-back").performClick()

        composeRule.waitUntil(5_000) { nodeExists("welcome-screen") }
        assertTrue(!nodeExists("register-screen"))
    }

    @Test
    fun storedSessionRestoresAuthenticatedShellAfterBootstrapValidation() {
        val backendClient = RecordingImBackendClient()
        val container = UiTestAppContainer(imBackendClient = backendClient)
        container.sessionStore.baseUrl = "http://127.0.0.1:18080/"
        container.sessionStore.token = "persisted-token"
        container.sessionStore.username = "nox-dev"

        setApp(container, initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.waitUntil(5_000) {
            backendClient.bootstrapTokens.contains("persisted-token") && nodeExists("bottom-nav")
        }

        assertEquals(listOf("persisted-token"), backendClient.bootstrapTokens)
    }

    @Test
    fun invalidStoredSessionFallsBackToWelcomeAndClearsSession() {
        val backendClient = RecordingImBackendClient(
            bootstrapFailure = IllegalStateException("session expired"),
        )
        val container = UiTestAppContainer(imBackendClient = backendClient)
        container.sessionStore.baseUrl = "http://127.0.0.1:18080/"
        container.sessionStore.token = "stale-token"
        container.sessionStore.username = "nox-dev"

        setApp(container, initialAuthStart = RootAuthStart.Unauthenticated)

        composeRule.waitUntil(5_000) {
            backendClient.bootstrapTokens.contains("stale-token") && nodeExists("welcome-screen")
        }

        assertTrue(container.sessionStore.token.isNullOrBlank())
        assertTrue(container.sessionStore.username.isNullOrBlank())
        assertTrue(!nodeExists("bottom-nav"))
    }

    @Test
    fun welcomeReferenceMockupAssetIsNotPackagedInRuntimeResources() {
        val packagedDrawable = composeRule.activity.resources.getIdentifier(
            "welcome_screen",
            "drawable",
            composeRule.activity.packageName,
        )

        assertEquals(0, packagedDrawable)
    }

    @Test
    fun welcomeUsesApprovedReplacementVideoResource() {
        val packagedReplacementVideo = composeRule.activity.resources.getIdentifier(
            "welcome_intro_1",
            "raw",
            composeRule.activity.packageName,
        )
        val supersededVideo = composeRule.activity.resources.getIdentifier(
            "welcome_atrium",
            "raw",
            composeRule.activity.packageName,
        )

        assertTrue(packagedReplacementVideo != 0)
        assertEquals(0, supersededVideo)
    }

    @Test
    fun rootShellDefaultsToChineseAndLightThemeOnFirstRun() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("gkim-theme-Light").fetchSemanticsNode()
        composeRule.onNodeWithText("消息").fetchSemanticsNode()
        composeRule.onNodeWithText("联系人").fetchSemanticsNode()
        composeRule.onNodeWithText("酒馆").fetchSemanticsNode()
    }

    @Test
    fun bottomNavigationSwitchesAcrossPrimaryTabs() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("bottom-nav").fetchSemanticsNode()
        composeRule.onNodeWithText("联系人").performClick()
        composeRule.onNodeWithTag("contacts-screen").fetchSemanticsNode()
        composeRule.onNodeWithText("酒馆").performClick()
        composeRule.onNodeWithTag("tavern-screen").fetchSemanticsNode()
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
        assertTrue(!nodeExists("messages-integration-status"))
        assertTrue(textNodeMissing("Signal Lattice"))
        assertTrue(textNodeMissing("Recent conversations, unread momentum, and a direct path into AIGC-assisted chats."))
        assertTrue(textNodeMissing("实时 IM"))
        composeRule.onNodeWithTag("messages-list").fetchSemanticsNode()
    }

    @Test
    fun messagesHeaderUsesQuickActionsInsteadOfPassiveConversationCountCopy() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("messages-quick-actions-trigger").fetchSemanticsNode()
        assertTrue(textNodeMissing("3 个活跃会话"))
        assertTrue(textNodeMissing("3 active"))

        composeRule.onNodeWithTag("messages-quick-actions-trigger").performClick()

        composeRule.onNodeWithTag("messages-quick-actions-menu").fetchSemanticsNode()
        composeRule.onNodeWithTag("messages-action-add-friend").fetchSemanticsNode()
        composeRule.onNodeWithTag("messages-action-scan-qr").fetchSemanticsNode()
    }

    @Test
    fun messagesQuickActionAddFriendOpensUserSearchFlow() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("messages-quick-actions-trigger").performClick()
        composeRule.onNodeWithTag("messages-action-add-friend").performClick()

        composeRule.onNodeWithTag("user-search-screen").fetchSemanticsNode()
    }

    @Test
    fun messagesQuickActionAddFriendBackReturnsToMessages() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("messages-quick-actions-trigger").performClick()
        composeRule.onNodeWithTag("messages-action-add-friend").performClick()
        composeRule.onNodeWithTag("user-search-back").performClick()

        composeRule.waitUntil(5_000) { nodeExists("messages-screen") }
        assertTrue(!nodeExists("user-search-screen"))
    }

    @Test
    fun messagesQuickActionScanQrOpensQrScanFlow() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("messages-quick-actions-trigger").performClick()
        composeRule.onNodeWithTag("messages-action-scan-qr").performClick()

        composeRule.onNodeWithTag("qr-scan-screen").fetchSemanticsNode()
    }

    @Test
    fun messagesQuickActionScanQrShowsDecodedPayloadWithoutSideEffects() {
        val backendClient = RecordingImBackendClient()
        val container = UiTestAppContainer(imBackendClient = backendClient)

        setApp(
            container,
            qrScannerControllerFactory = fakeQrScannerControllerFactory("gkim://friend?user=nova"),
        )

        composeRule.onNodeWithTag("messages-quick-actions-trigger").performClick()
        composeRule.onNodeWithTag("messages-action-scan-qr").performClick()
        composeRule.onNodeWithTag("qr-scan-detect-test").performClick()

        composeRule.onNodeWithTag("qr-result-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("qr-result-payload").assertTextContains("gkim://friend?user=nova")
        assertTrue(backendClient.sentFriendRequestIds.isEmpty())
    }

    @Test
    fun qrQuickActionBackStackReturnsToMessagesAfterViewingResult() {
        setApp(
            UiTestAppContainer(),
            qrScannerControllerFactory = fakeQrScannerControllerFactory("gkim://friend?user=nova"),
        )

        composeRule.onNodeWithTag("messages-quick-actions-trigger").performClick()
        composeRule.onNodeWithTag("messages-action-scan-qr").performClick()
        composeRule.onNodeWithTag("qr-scan-detect-test").performClick()
        composeRule.onNodeWithText("返回").performClick()

        composeRule.waitUntil(5_000) { nodeExists("qr-scan-screen") }

        composeRule.onNodeWithText("返回").performClick()

        composeRule.waitUntil(5_000) { nodeExists("messages-screen") }
        assertTrue(!nodeExists("qr-result-screen"))
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
    fun tavernScreenShowsPresetRosterAndDrawEntry() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("酒馆").performClick()

        composeRule.onNodeWithTag("tavern-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("tavern-preset-section").fetchSemanticsNode()
        composeRule.onNodeWithTag("tavern-draw-trigger").fetchSemanticsNode()
        composeRule.onNodeWithTag("tavern-screen").performScrollToNode(hasTestTag("tavern-owned-section"))
        composeRule.onNodeWithTag("tavern-owned-section").fetchSemanticsNode()
        assertTrue(textNodeMissing("为你推荐"))
        assertTrue(textNodeMissing("提示工程"))
    }

    @Test
    fun tavernScreenUsesChineseCompanionCopyByDefault() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("酒馆").performClick()

        composeRule.onNodeWithText("筑谕师").fetchSemanticsNode()
        composeRule.onNodeWithText("冷静策士").fetchSemanticsNode()
        composeRule.onNodeWithText(
            "把纷乱感受整理成清晰计划，并陪你迈出下一步的精确同伴。",
            substring = true,
        ).fetchSemanticsNode()
        composeRule.onNodeWithText(
            "我一直在酒馆等你。今晚是什么样的夜色，说给我听。",
            substring = true,
        ).fetchSemanticsNode()
    }

    @Test
    fun switchingToEnglishRefreshesTavernAndCompanionChatCopy() {
        val container = UiTestAppContainer()
        setApp(container)

        composeRule.onNodeWithText("酒馆").performClick()
        composeRule.onNodeWithText("筑谕师").fetchSemanticsNode()

        openSettingsFromSpace()
        composeRule.onNodeWithTag("settings-menu-appearance").performClick()
        composeRule.onNodeWithTag("settings-detail-appearance").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-language-english").performClick()
        composeRule.waitUntil(5_000) {
            container.preferencesStore.currentLanguage == com.gkim.im.android.core.model.AppLanguage.English
        }

        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(5_000) { nodeExists("settings-menu-screen") }
        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(5_000) { nodeExists("tavern-screen") }

        composeRule.onNodeWithText("Architect Oracle").fetchSemanticsNode()
        composeRule.onNodeWithText("Calm Strategist").fetchSemanticsNode()
        composeRule.onNodeWithTag("tavern-preset-card-architect-oracle").performClick()

        composeRule.waitUntil(5_000) { nodeExists("character-detail-screen") }
        composeRule.onNodeWithTag("character-detail-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("character-detail-activate").performClick()

        composeRule.waitUntil(5_000) { nodeExists("chat-screen") }
        composeRule.onNodeWithTag("chat-contact-name").assertTextContains("Architect Oracle")
        composeRule.onNodeWithTag("chat-contact-title").assertTextContains("Calm Strategist")
    }

    @Test
    fun tavernCreateCharacterOpensEditorAndSavesCustomCard() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("酒馆").performClick()
        composeRule.onNodeWithText("新建").performClick()

        composeRule.waitUntil(5_000) { nodeExists("character-editor-screen") }
        composeRule.onAllNodesWithText("Display name")[0].performTextReplacement("Custom Oracle")
        composeRule.onAllNodesWithText("角色名")[0].performTextReplacement("自建谕师")
        composeRule.onAllNodesWithText("Role label")[0].performTextReplacement("Custom Role")
        composeRule.onAllNodesWithText("角色标签")[0].performTextReplacement("自建角色")
        composeRule.onAllNodesWithText("Summary")[0].performTextReplacement("A custom tavern card")
        composeRule.onAllNodesWithText("摘要")[0].performTextReplacement("一个自建酒馆角色")
        composeRule.onAllNodesWithText("First message")[0].performTextReplacement("Hello there")
        composeRule.onAllNodesWithText("开场白")[0].performTextReplacement("你好呀")
        composeRule.onNodeWithText("保存").performClick()

        composeRule.waitUntil(5_000) { nodeExists("tavern-screen") }
        composeRule.onNodeWithTag("tavern-screen").performScrollToNode(hasTestTag("tavern-user-section"))
        composeRule.onNodeWithTag("tavern-user-section").fetchSemanticsNode()
        composeRule.onNodeWithText("自建谕师").fetchSemanticsNode()
    }

    @Test
    fun tavernScreenHeaderShowsSettingsEntryPoint() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("酒馆").performClick()

        composeRule.onNodeWithText("设置").fetchSemanticsNode()
    }

    @Test
    fun tavernCharacterActivationOpensCompanionConversation() {
        val container = UiTestAppContainer()
        setApp(container)

        composeRule.onNodeWithText("酒馆").performClick()
        composeRule.onNodeWithTag("tavern-preset-card-architect-oracle").performClick()

        composeRule.waitUntil(5_000) { nodeExists("character-detail-screen") }
        composeRule.onNodeWithTag("character-detail-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("character-detail-activate").performClick()

        composeRule.waitUntil(5_000) {
            container.companionRosterRepository.activeCharacterId.value == "architect-oracle" &&
                nodeExists("chat-screen")
        }

        composeRule.onNodeWithTag("chat-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-contact-name").assertTextContains("筑谕师")
        composeRule.onNodeWithTag("chat-contact-title").assertTextContains("冷静策士")
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
    fun chatSecondaryMenuFiltersAigcActionsToTheActiveProviderCapabilities() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()
        composeRule.onNodeWithTag("chat-plus-button").performClick()

        composeRule.onNodeWithTag("chat-secondary-menu").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-action-send-image-message").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-action-text-to-image").fetchSemanticsNode()
        assertTrue(!nodeExists("chat-action-choose-image-source"))
        assertTrue(!nodeExists("chat-action-image-to-image"))
        assertTrue(!nodeExists("chat-action-choose-video-source"))
        assertTrue(!nodeExists("chat-action-video-to-video"))
    }

    @Test
    fun chatSecondaryMenuSeparatesChatAttachmentAndGenerationSourceFlows() {
        val container = UiTestAppContainer()
        setApp(container, fakeMediaPickerControllerFactory())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()
        composeRule.onNodeWithTag("chat-composer-input").performTextReplacement("Turn this orbit frame into a cinematic cut.")

        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.onNodeWithTag("chat-action-send-image-message").performClick()
        composeRule.onNodeWithTag("chat-chat-attachment-ready").fetchSemanticsNode()
        assertTrue(!nodeExists("chat-generation-source-ready"))
        assertTrue(!nodeExists("chat-action-image-to-image"))

        composeRule.onNodeWithTag("chat-action-text-to-image").performClick()
        composeRule.waitUntil(5_000) {
            val latestTask = container.aigcRepository.history.value.firstOrNull()
            latestTask?.mode == AigcMode.TextToImage && latestTask.input == null
        }
        composeRule.onNodeWithText("TextToImage · Turn this orbit frame into a cinematic cut.").fetchSemanticsNode()
    }

    @Test
    fun chatSendButtonSendsStagedImageAttachmentAsNormalOutgoingMessage() {
        val container = UiTestAppContainer()
        setApp(container, fakeMediaPickerControllerFactory())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()

        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.onNodeWithTag("chat-action-send-image-message").performClick()
        composeRule.onNodeWithTag("chat-chat-attachment-ready").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-send-button").performClick()

        composeRule.waitUntil(5_000) {
            container.messagingRepository.conversations.value
                .first { it.id == "room-leo" }
                .messages
                .lastOrNull()
                ?.attachment
                ?.preview == "content://ui-test/fake-image"
        }
    }

    @Test
    fun latestGenerationCardOffersSaveAndSendActionsForSuccessfulImages() {
        val generatedImageSaver = RecordingGeneratedImageSaver()
        val container = UiTestAppContainer(generatedImageSaver = generatedImageSaver)
        setApp(container, fakeMediaPickerControllerFactory())

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()
        composeRule.onNodeWithTag("chat-composer-input").performTextReplacement("Render a launch-ready concept frame.")

        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.onNodeWithTag("chat-action-text-to-image").performClick()
        composeRule.waitUntil(5_000) {
            val latestTask = container.aigcRepository.history.value.firstOrNull()
            latestTask?.status == TaskStatus.Succeeded && !latestTask.outputPreview.isNullOrBlank()
        }

        composeRule.onNodeWithTag("chat-generation-save").fetchSemanticsNode()
        composeRule.onNodeWithTag("chat-generation-send").fetchSemanticsNode()

        composeRule.onNodeWithTag("chat-generation-save").performClick()
        composeRule.waitUntil(5_000) { generatedImageSaver.savedUrls.isNotEmpty() }
        composeRule.waitUntil(5_000) { textExists("Saved generated image locally.") }
        composeRule.onNodeWithText("Saved generated image locally.").fetchSemanticsNode()

        composeRule.onNodeWithTag("chat-generation-send").performClick()
        val generatedPreview = container.aigcRepository.history.value.first().outputPreview
        composeRule.waitUntil(5_000) {
            container.messagingRepository.conversations.value
                .first { it.id == "room-leo" }
                .messages
                .lastOrNull()
                ?.attachment
                ?.preview == generatedPreview
        }
    }

    @Test
    fun chatGenerationFailureShowsMissingPresetKeyFeedback() {
        val container = UiTestAppContainer(presetApiKeys = emptyMap())
        setApp(container)

        composeRule.onNodeWithTag("conversation-row-room-leo").performClick()
        composeRule.onNodeWithTag("chat-composer-input").performTextReplacement("Render a blocked frame.")
        composeRule.onNodeWithTag("chat-plus-button").performClick()
        composeRule.onNodeWithTag("chat-action-text-to-image").performClick()

        composeRule.waitUntil(5_000) {
            container.aigcRepository.history.value.firstOrNull()?.status == TaskStatus.Failed
        }

        composeRule.onNodeWithTag("chat-generation-error")
            .assertTextContains("Tencent Hunyuan API key is required before generation can start.")
        assertTrue(!nodeExists("chat-action-video-to-video"))
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
    fun primaryTabsUseConsistentTopLevelHeadingScale() {
        setApp(UiTestAppContainer())

        val messagesBounds = topmostTextBounds("最近对话")

        composeRule.onNodeWithText("联系人").performClick()
        val contactsBounds = topmostTextBounds("联系人")

        composeRule.onNodeWithText("空间").performClick()
        val spaceBounds = topmostTextBounds("空间")

        val messagesHeight = messagesBounds.bottom - messagesBounds.top
        val contactsHeight = contactsBounds.bottom - contactsBounds.top
        val spaceHeight = spaceBounds.bottom - spaceBounds.top

        assertTrue("messagesHeight=$messagesHeight contactsHeight=$contactsHeight", kotlin.math.abs(messagesHeight - contactsHeight) <= 8f)
        assertTrue("messagesHeight=$messagesHeight spaceHeight=$spaceHeight", kotlin.math.abs(messagesHeight - spaceHeight) <= 8f)
        assertTrue("messagesTop=${messagesBounds.top} contactsTop=${contactsBounds.top}", kotlin.math.abs(messagesBounds.top - contactsBounds.top) <= 12f)
        assertTrue("messagesTop=${messagesBounds.top} spaceTop=${spaceBounds.top}", kotlin.math.abs(messagesBounds.top - spaceBounds.top) <= 12f)
    }

    @Test
    fun contactsScreenPlacesSortDropdownInsideTitleBand() {
        setApp(UiTestAppContainer())

        composeRule.onNodeWithText("联系人").performClick()

        val titleBounds = topmostTextBounds("联系人")
        val sortBounds = composeRule.onNodeWithTag("contact-sort-dropdown").fetchSemanticsNode().boundsInRoot
        val firstRowBounds = composeRule.onNodeWithTag("contact-row-aria-thorne").fetchSemanticsNode().boundsInRoot
        val topBandBottom = maxOf(titleBounds.bottom, sortBounds.bottom)
        val listGap = firstRowBounds.top - topBandBottom

        assertTrue("titleBottom=${titleBounds.bottom} sortTop=${sortBounds.top}", sortBounds.top < titleBounds.bottom)
        assertTrue("listGap=$listGap", listGap <= 48f)
    }

    @Test
    fun settingsMenuPresentsFocusedEntriesAndAccountActionsSurface() {
        setApp(UiTestAppContainer())

        openSettingsFromSpace()

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

        openSettingsFromSpace()
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
    fun settingsInteractionsUpdatePresetProviderConfiguration() {
        val container = UiTestAppContainer()
        setApp(container)

        openSettingsFromSpace()
        composeRule.onNodeWithTag("settings-menu-ai-provider").performClick()
        composeRule.onNodeWithTag("settings-detail-ai-provider").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-provider-hunyuan").performClick()
        composeRule.waitUntil(5_000) { container.aigcRepository.activeProviderId.value == "hunyuan" }

        assertTrue(!nodeExists("settings-base-url"))
        composeRule.onNodeWithTag("settings-model").performTextReplacement("hy-image-v3.6")
        composeRule.onNodeWithTag("settings-api-key").performTextReplacement("updated-preset-secret")

        composeRule.waitUntil(5_000) {
            container.aigcRepository.presetProviderConfigs.value["hunyuan"]?.model == "hy-image-v3.6"
        }

        assertEquals("hy-image-v3.6", container.aigcRepository.presetProviderConfigs.value["hunyuan"]?.model)
        assertEquals("updated-preset-secret", container.aigcRepository.presetProviderConfigs.value["hunyuan"]?.apiKey)
    }

    @Test
    fun settingsScreenAppliesLanguageAndThemePreferencesAcrossShell() {
        val container = UiTestAppContainer()
        setApp(container)

        openSettingsFromSpace()
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
        composeRule.waitUntil(5_000) { nodeExists("tavern-screen") }

        composeRule.onNodeWithTag("gkim-theme-Light").fetchSemanticsNode()
        composeRule.onNodeWithTag("tavern-screen").fetchSemanticsNode()
    }

    @Test
    fun settingsScreenExposesImBackendValidationControlsAndStatus() {
        val container = UiTestAppContainer()
        setApp(container)

        openSettingsFromSpace()
        composeRule.onNodeWithTag("settings-menu-im-validation").performClick()
        composeRule.onNodeWithTag("settings-detail-im-validation").fetchSemanticsNode()
        assertTrue(!nodeExists("settings-im-http-base-url"))
        assertTrue(!nodeExists("settings-im-websocket-url"))
        composeRule.onNodeWithTag("settings-im-resolved-backend-origin").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-im-resolved-websocket-url").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-im-developer-toggle").performClick()
        composeRule.onNodeWithTag("settings-im-backend-origin").performTextReplacement("https://forward.example.com/")
        composeRule.onNodeWithTag("settings-im-dev-user").performTextReplacement("leo-vance")
        composeRule.waitUntil(5_000) {
            container.preferencesStore.currentImBackendOrigin == "https://forward.example.com/" &&
                container.preferencesStore.currentImDevUserExternalId == "leo-vance"
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings-im-resolved-backend-origin").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-im-resolved-websocket-url").fetchSemanticsNode()
        composeRule.onNodeWithTag("settings-im-validation-status").assertTextContains("已连接")

        composeRule.onNodeWithTag("settings-im-dev-user").performTextClearance()
        composeRule.waitUntil(5_000) { container.preferencesStore.currentImDevUserExternalId.isEmpty() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("连接设置不完整或格式无效。").fetchSemanticsNode()
    }

    @Test
    fun contactsScreenLoadsPendingRequestsAndAcceptsThem() {
        val backendClient = RecordingImBackendClient(
            pendingRequests = mutableListOf(
                FriendRequestViewDto(
                    id = "request-1",
                    fromUser = BackendUserDto(
                        id = "user-nova",
                        externalId = "nova_user",
                        displayName = "Nova User",
                        title = "",
                        avatarText = "NU",
                    ),
                    toUserId = "user-nox",
                    toUserExternalId = "nox-dev",
                    status = "pending",
                    createdAt = "2026-04-10T09:00:00Z",
                )
            ),
        )
        val container = UiTestAppContainer(imBackendClient = backendClient)
        container.sessionStore.baseUrl = "http://127.0.0.1:18080/"
        container.sessionStore.token = "session-token"
        container.sessionStore.username = "nox-dev"

        setApp(container)

        composeRule.onNodeWithText("联系人").performClick()
        composeRule.onNodeWithTag("contacts-friend-requests-header").fetchSemanticsNode()
        composeRule.onNodeWithTag("friend-request-request-1").fetchSemanticsNode()
        composeRule.onNodeWithText("接受").performClick()

        composeRule.waitUntil(5_000) {
            backendClient.acceptedRequestIds.contains("request-1") &&
                !nodeExists("friend-request-request-1")
        }
    }

    @Test
    fun userSearchFlowShowsResultsAndMarksPendingAfterAdd() {
        val backendClient = RecordingImBackendClient(
            searchResults = mutableListOf(
                UserSearchResultDto(
                    id = "user-leo",
                    username = "leo-vance",
                    displayName = "Leo Vance",
                    avatarText = "LV",
                    contactStatus = "none",
                )
            ),
        )
        val container = UiTestAppContainer(imBackendClient = backendClient)
        container.sessionStore.baseUrl = "http://127.0.0.1:18080/"
        container.sessionStore.token = "session-token"
        container.sessionStore.username = "nox-dev"

        setApp(container)

        composeRule.onNodeWithText("联系人").performClick()
        composeRule.onNodeWithText("搜索用户").performClick()
        composeRule.onNodeWithTag("user-search-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("user-search-input").performTextReplacement("leo")

        composeRule.waitUntil(5_000) {
            backendClient.searchQueries.contains("leo") && nodeExists("search-result-user-leo")
        }

        composeRule.onNodeWithText("添加").performClick()

        composeRule.waitUntil(5_000) {
            backendClient.sentFriendRequestIds.contains("user-leo")
        }

        composeRule.onNodeWithText("已发送").fetchSemanticsNode()
    }

    @Test
    fun messagesLaunchedAddFriendFlowShowsErrorWhenRequestFails() {
        val backendClient = RecordingImBackendClient(
            sendFriendRequestFailure = IllegalStateException("backend unavailable"),
            searchResults = mutableListOf(
                UserSearchResultDto(
                    id = "user-leo",
                    username = "leo-vance",
                    displayName = "Leo Vance",
                    avatarText = "LV",
                    contactStatus = "none",
                )
            ),
        )
        val container = UiTestAppContainer(imBackendClient = backendClient)
        container.sessionStore.baseUrl = "http://127.0.0.1:18080/"
        container.sessionStore.token = "session-token"
        container.sessionStore.username = "nox-dev"

        setApp(container)

        composeRule.onNodeWithTag("messages-quick-actions-trigger").performClick()
        composeRule.onNodeWithTag("messages-action-add-friend").performClick()
        composeRule.onNodeWithTag("user-search-input").performTextReplacement("leo")

        composeRule.waitUntil(5_000) {
            backendClient.searchQueries.contains("leo") && nodeExists("search-result-user-leo")
        }

        composeRule.onNodeWithText("添加").performClick()

        composeRule.waitUntil(5_000) {
            backendClient.sentFriendRequestIds.contains("user-leo") && textExists("发送失败")
        }

        assertTrue(textExists("发送失败"))
        assertTrue(textNodeMissing("已发送"))
    }

    private fun setApp(
        container: UiTestAppContainer,
        mediaPickerControllerFactory: MediaPickerControllerFactory? = null,
        qrScannerControllerFactory: QrScannerControllerFactory? = null,
        initialAuthStart: RootAuthStart = RootAuthStart.Authenticated,
    ) {
        composeRule.setContent {
            GkimRootApp(
                container = container,
                mediaPickerControllerFactory = mediaPickerControllerFactory,
                qrScannerControllerFactory = qrScannerControllerFactory,
                initialAuthStart = initialAuthStart,
            )
        }
        composeRule.waitUntil(5_000) {
            when (initialAuthStart) {
                RootAuthStart.Authenticated -> nodeExists("bottom-nav")
                RootAuthStart.Unauthenticated -> nodeExists("welcome-screen") || nodeExists("bottom-nav")
            }
        }
    }

    private fun openSettingsFromSpace() {
        composeRule.onNodeWithText("酒馆").performClick()
        composeRule.onNodeWithText("设置").performClick()
    }

    private fun fakeMediaPickerControllerFactory(): MediaPickerControllerFactory = { onMediaSelected ->
        MediaPickerController(
            pickImage = { onMediaSelected(MediaInput(AttachmentType.Image, Uri.parse("content://ui-test/fake-image"))) },
            pickVideo = { onMediaSelected(MediaInput(AttachmentType.Video, Uri.parse("content://ui-test/fake-video"))) },
        )
    }

    private fun fakeQrScannerControllerFactory(payload: String): QrScannerControllerFactory = { onPayloadScanned ->
        QrScannerController(
            hasCameraPermission = true,
            requestPermission = {},
            previewContent = { modifier ->
                Text(
                    text = "Trigger QR detection",
                    modifier = modifier
                        .testTag("qr-scan-detect-test")
                        .clickable { onPayloadScanned(payload) },
                )
            },
        )
    }

    private fun nodeExists(tag: String): Boolean = runCatching {
        composeRule.onNodeWithTag(tag).fetchSemanticsNode()
        true
    }.getOrDefault(false)

    private fun textExists(text: String): Boolean = runCatching {
        composeRule.onNodeWithText(text, substring = true).fetchSemanticsNode()
        true
    }.getOrDefault(false)

    private fun textNodeMissing(text: String): Boolean = runCatching {
        composeRule.onNodeWithText(text).fetchSemanticsNode()
        false
    }.getOrDefault(true)

    private fun topmostTextBounds(text: String): androidx.compose.ui.geometry.Rect =
        composeRule
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .minBy { it.boundsInRoot.top }
            .boundsInRoot

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
    initialActiveProviderId: String = "hunyuan",
    override val messagingRepository: MessagingRepository = InMemoryMessagingRepository(conversations),
    override val imBackendClient: ImBackendClient = RecordingImBackendClient(),
    override val generatedImageSaver: GeneratedImageSaver = RecordingGeneratedImageSaver(),
    private val presetApiKeys: Map<String, String> = mapOf(
        "hunyuan" to "ui-test-hunyuan-key",
        "tongyi" to "ui-test-tongyi-key",
    ),
    private val providerClients: Map<String, RemoteAigcProviderClient> = mapOf(
        "hunyuan" to UiTestRemoteAigcProviderClient("hunyuan"),
        "tongyi" to UiTestRemoteAigcProviderClient("tongyi"),
    ),
) : AppContainer {
    private val okHttpClient = OkHttpClient.Builder().build()
    override val preferencesStore = UiTestPreferencesStore(initialActiveProviderId = initialActiveProviderId)
    override val sessionStore: SessionStore =
        SessionStore(ApplicationProvider.getApplicationContext())
    private val secureStore = UiInMemorySecureStore()

    init {
        sessionStore.clear()
        sessionStore.baseUrl = null
        presetApiKeys.forEach { (providerId, apiKey) ->
            secureStore.putString("preset_provider_${providerId}_api_key", apiKey)
        }
    }

    override val contactsRepository: ContactsRepository = DefaultContactsRepository(seedContacts, preferencesStore, Dispatchers.Main)
    override val feedRepository: FeedRepository = DefaultFeedRepository(seedPosts, seedPrompts, Dispatchers.Main)
    override val companionRosterRepository: CompanionRosterRepository = DefaultCompanionRosterRepository(
        presetCharacters = seedPresetCharacters,
        drawPool = seedDrawPoolCharacters,
    )
    override val companionSkinRepository: CompanionSkinRepository = InMemoryCompanionSkinRepository()
    override val companionTurnRepository: CompanionTurnRepository = DefaultCompanionTurnRepository()
    override val cardInteropRepository: CardInteropRepository = DefaultCardInteropRepository(
        delegate = LiveCardInteropRepository(
            backendClient = imBackendClient,
            baseUrlProvider = { sessionStore.baseUrl },
            tokenProvider = { sessionStore.token },
        ),
    )
    override val userPersonaRepository: UserPersonaRepository = DefaultUserPersonaRepository(
        initialPersonas = seedBuiltInPersonas,
    )
    override val worldInfoRepository: WorldInfoRepository = DefaultWorldInfoRepository()
    override val companionMemoryRepository: CompanionMemoryRepository = DefaultCompanionMemoryRepository()
    override val companionPresetRepository: CompanionPresetRepository = DefaultCompanionPresetRepository()
    override val aigcRepository: AigcRepository = DefaultAigcRepository(
        presets = presetProviders,
        preferencesStore = preferencesStore,
        secureStore = secureStore,
        dispatcher = Dispatchers.Main,
        providerClients = providerClients,
        mediaInputEncoder = UiTestMediaInputEncoder(),
    )
    override val markdownParser: MarkdownDocumentParser = MarkdownDocumentParser()
    override val realtimeChatClient: RealtimeChatClient =
        RealtimeChatClient(okHttpClient, "wss://example.com/realtime")
}

private class RecordingGeneratedImageSaver : GeneratedImageSaver {
    val savedUrls = mutableListOf<String>()

    override suspend fun saveImage(imageUrl: String, prompt: String): GeneratedImageSaveResult {
        savedUrls += imageUrl
        return GeneratedImageSaveResult.Success(Uri.parse("content://ui-test/saved-generated-image"))
    }
}

private data class RegisterCall(
    val baseUrl: String,
    val username: String,
    val password: String,
    val displayName: String,
)

private data class LoginCall(
    val baseUrl: String,
    val username: String,
    val password: String,
)

private class RecordingImBackendClient(
    private val registerFailure: Throwable? = null,
    private val loginFailure: Throwable? = null,
    private val bootstrapFailure: Throwable? = null,
    private val sendFriendRequestFailure: Throwable? = null,
    val searchResults: MutableList<UserSearchResultDto> = mutableListOf(),
    val pendingRequests: MutableList<FriendRequestViewDto> = mutableListOf(),
) : ImBackendClient {
    val registerCalls = mutableListOf<RegisterCall>()
    val loginCalls = mutableListOf<LoginCall>()
    val bootstrapTokens = mutableListOf<String>()
    val searchQueries = mutableListOf<String>()
    val sentFriendRequestIds = mutableListOf<String>()
    val acceptedRequestIds = mutableListOf<String>()
    val rejectedRequestIds = mutableListOf<String>()

    override suspend fun issueDevSession(baseUrl: String, externalId: String): com.gkim.im.android.data.remote.im.DevSessionResponseDto {
        return com.gkim.im.android.data.remote.im.DevSessionResponseDto(
            token = "dev-session-token",
            expiresAt = "2026-04-15T10:00:00Z",
            user = backendUser(id = "user-$externalId", externalId = externalId, displayName = externalId),
        )
    }

    override suspend fun register(
        baseUrl: String,
        username: String,
        password: String,
        displayName: String,
    ): AuthResponseDto {
        registerCalls += RegisterCall(baseUrl, username, password, displayName)
        registerFailure?.let { throw it }
        return AuthResponseDto(
            token = "auth-token-register",
            expiresAt = "2026-04-15T10:00:00Z",
            user = backendUser(id = "user-$username", externalId = username, displayName = displayName),
        )
    }

    override suspend fun login(
        baseUrl: String,
        username: String,
        password: String,
    ): AuthResponseDto {
        loginCalls += LoginCall(baseUrl, username, password)
        loginFailure?.let { throw it }
        return AuthResponseDto(
            token = "auth-token-login",
            expiresAt = "2026-04-15T10:00:00Z",
            user = backendUser(id = "user-$username", externalId = username, displayName = username),
        )
    }

    override suspend fun searchUsers(
        baseUrl: String,
        token: String,
        query: String,
    ): List<UserSearchResultDto> {
        searchQueries += query
        return searchResults.toList()
    }

    override suspend fun sendFriendRequest(
        baseUrl: String,
        token: String,
        toUserId: String,
    ): FriendRequestViewDto {
        sentFriendRequestIds += toUserId
        sendFriendRequestFailure?.let { throw it }
        return FriendRequestViewDto(
            id = "request-${sentFriendRequestIds.size}",
            fromUser = backendUser(id = "user-nox", externalId = "nox-dev", displayName = "Nox Dev"),
            toUserId = toUserId,
            toUserExternalId = "target-user",
            status = "pending",
            createdAt = "2026-04-10T09:00:00Z",
        )
    }

    override suspend fun listFriendRequests(
        baseUrl: String,
        token: String,
    ): List<FriendRequestViewDto> = pendingRequests.toList()

    override suspend fun acceptFriendRequest(
        baseUrl: String,
        token: String,
        requestId: String,
    ): FriendRequestViewDto {
        acceptedRequestIds += requestId
        val request = pendingRequests.first { it.id == requestId }
        pendingRequests.removeAll { it.id == requestId }
        return request.copy(status = "accepted")
    }

    override suspend fun rejectFriendRequest(
        baseUrl: String,
        token: String,
        requestId: String,
    ): FriendRequestViewDto {
        rejectedRequestIds += requestId
        val request = pendingRequests.first { it.id == requestId }
        pendingRequests.removeAll { it.id == requestId }
        return request.copy(status = "rejected")
    }

    override suspend fun loadBootstrap(
        baseUrl: String,
        token: String,
    ): BootstrapBundleDto {
        bootstrapTokens += token
        bootstrapFailure?.let { throw it }
        return BootstrapBundleDto(
            user = backendUser(id = "user-nox", externalId = "nox-dev", displayName = "Nox Dev"),
            contacts = listOf(
                ContactProfileDto(
                    userId = "user-leo",
                    externalId = "leo-vance",
                    displayName = "Leo Vance",
                    title = "Realtime Systems",
                    avatarText = "LV",
                    addedAt = "2026-04-08T08:58:00Z",
                )
            ),
            conversations = listOf(
                ConversationSummaryDto(
                    conversationId = "conversation-1",
                    contact = ContactProfileDto(
                        userId = "user-leo",
                        externalId = "leo-vance",
                        displayName = "Leo Vance",
                        title = "Realtime Systems",
                        avatarText = "LV",
                        addedAt = "2026-04-08T08:58:00Z",
                    ),
                    unreadCount = 1,
                    lastMessage = MessageRecordDto(
                        id = "message-1",
                        conversationId = "conversation-1",
                        senderUserId = "user-leo",
                        senderExternalId = "leo-vance",
                        kind = "text",
                        body = "Hello Nox",
                        createdAt = "2026-04-08T09:00:00Z",
                        deliveredAt = "2026-04-08T09:00:01Z",
                        readAt = null,
                    ),
                )
            ),
        )
    }

    override suspend fun sendDirectImageMessage(
        baseUrl: String,
        token: String,
        request: com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto,
    ): com.gkim.im.android.data.remote.im.SendDirectMessageResultDto {
        error("sendDirectImageMessage is not configured for GkimRootAppTest")
    }

    override suspend fun loadHistory(
        baseUrl: String,
        token: String,
        conversationId: String,
        limit: Int,
        before: String?,
    ): MessageHistoryPageDto {
        return MessageHistoryPageDto(
            conversationId = conversationId,
            hasMore = false,
            messages = emptyList(),
        )
    }

    private fun backendUser(
        id: String,
        externalId: String,
        displayName: String,
    ): BackendUserDto {
        return BackendUserDto(
            id = id,
            externalId = externalId,
            displayName = displayName,
            title = "",
            avatarText = externalId.take(2).uppercase(),
        )
    }
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
