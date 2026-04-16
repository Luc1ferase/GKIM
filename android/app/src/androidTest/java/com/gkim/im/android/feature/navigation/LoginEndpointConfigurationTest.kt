package com.gkim.im.android.feature.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.media.GeneratedImageSaveResult
import com.gkim.im.android.core.media.GeneratedImageSaver
import com.gkim.im.android.core.rendering.MarkdownDocumentParser
import com.gkim.im.android.data.local.SessionStore
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BackendUserDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
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
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginEndpointConfigurationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loginRouteShowsEndpointConfigurationErrorWhenDeveloperOverrideIsInvalid() {
        val backendClient = LoginEndpointRecordingBackendClient()
        val container = LoginEndpointTestAppContainer(imBackendClient = backendClient)
        runBlocking {
            container.preferencesStore.setImBackendOrigin("not-a-valid-origin")
            container.preferencesStore.setImDevUserExternalId("nox-dev")
        }

        composeRule.setContent {
            GkimRootApp(
                container = container,
                initialAuthStart = RootAuthStart.Unauthenticated,
            )
        }

        composeRule.waitUntil(5_000) { nodeExists("welcome-screen") }

        composeRule.onNodeWithTag("welcome-login-button").performClick()
        composeRule.onNodeWithTag("login-screen").fetchSemanticsNode()
        composeRule.onNodeWithTag("login-username").performTextReplacement("nova_user")
        composeRule.onNodeWithTag("login-password").performTextReplacement("passw0rd!")
        composeRule.onNodeWithTag("login-submit").performClick()

        composeRule.waitUntil(5_000) { nodeExists("login-error") }

        composeRule.onNodeWithTag("login-error").fetchSemanticsNode()
        assertTrue(backendClient.loginCalls == 0)
        assertTrue(container.sessionStore.token.isNullOrBlank())
    }

    private fun nodeExists(tag: String): Boolean = runCatching {
        composeRule.onNodeWithTag(tag).fetchSemanticsNode()
        true
    }.getOrDefault(false)
}

private class LoginEndpointTestAppContainer(
    override val imBackendClient: ImBackendClient,
) : AppContainer {
    private val okHttpClient = OkHttpClient.Builder().build()
    override val preferencesStore = UiTestPreferencesStore()
    override val sessionStore: SessionStore = SessionStore(ApplicationProvider.getApplicationContext())
    private val secureStore = UiInMemorySecureStore()

    init {
        sessionStore.clear()
        secureStore.putString("preset_provider_hunyuan_api_key", "ui-test-hunyuan-key")
        secureStore.putString("preset_provider_tongyi_api_key", "ui-test-tongyi-key")
    }

    override val messagingRepository: MessagingRepository = InMemoryMessagingRepository(seedConversations)
    override val contactsRepository: ContactsRepository = DefaultContactsRepository(seedContacts, preferencesStore, Dispatchers.Main)
    override val feedRepository: FeedRepository = DefaultFeedRepository(seedPosts, seedPrompts, Dispatchers.Main)
    override val aigcRepository: AigcRepository = DefaultAigcRepository(
        presets = presetProviders,
        preferencesStore = preferencesStore,
        secureStore = secureStore,
        dispatcher = Dispatchers.Main,
        providerClients = mapOf(
            "hunyuan" to UiTestRemoteAigcProviderClient("hunyuan"),
            "tongyi" to UiTestRemoteAigcProviderClient("tongyi"),
        ),
        mediaInputEncoder = UiTestMediaInputEncoder(),
    )
    override val markdownParser: MarkdownDocumentParser = MarkdownDocumentParser()
    override val realtimeChatClient: RealtimeChatClient =
        RealtimeChatClient(okHttpClient, "wss://example.com/realtime")
    override val generatedImageSaver: GeneratedImageSaver = object : GeneratedImageSaver {
        override suspend fun saveImage(imageUrl: String, prompt: String): GeneratedImageSaveResult {
            return GeneratedImageSaveResult.Failure("unused in login endpoint configuration test")
        }
    }
}

private class LoginEndpointRecordingBackendClient : ImBackendClient {
    var loginCalls: Int = 0

    override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto {
        error("issueDevSession should not be called in login endpoint configuration test")
    }

    override suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto {
        error("register should not be called in login endpoint configuration test")
    }

    override suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto {
        loginCalls += 1
        error("login should not be called when IM validation target is missing")
    }

    override suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto> = emptyList()

    override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto {
        error("sendFriendRequest should not be called in login endpoint configuration test")
    }

    override suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto> = emptyList()

    override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto {
        error("acceptFriendRequest should not be called in login endpoint configuration test")
    }

    override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto {
        error("rejectFriendRequest should not be called in login endpoint configuration test")
    }

    override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto {
        error("loadBootstrap should not be called in login endpoint configuration test")
    }

    override suspend fun sendDirectImageMessage(
        baseUrl: String,
        token: String,
        request: com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto,
    ): com.gkim.im.android.data.remote.im.SendDirectMessageResultDto {
        error("sendDirectImageMessage should not be called in login endpoint configuration test")
    }

    override suspend fun loadHistory(
        baseUrl: String,
        token: String,
        conversationId: String,
        limit: Int,
        before: String?,
    ): MessageHistoryPageDto {
        error("loadHistory should not be called in login endpoint configuration test")
    }
}
