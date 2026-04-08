package com.gkim.im.android.feature.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.data.remote.im.ImBackendHttpClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.DefaultAppContainer
import com.gkim.im.android.data.repository.MessagingIntegrationPhase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveImBackendValidationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val httpBaseUrl = "http://127.0.0.1:18080/"
    private val webSocketUrl = "ws://127.0.0.1:18080/ws"

    private lateinit var backendClient: ImBackendHttpClient
    private lateinit var counterpartGateway: com.gkim.im.android.data.remote.realtime.RealtimeChatClient
    private lateinit var appContainer: DefaultAppContainer
    private lateinit var swapContainer: (DefaultAppContainer) -> Unit
    private lateinit var noxToken: String
    private lateinit var conversationId: String
    private lateinit var latestHistoryBody: String
    private var contentInitialized = false

    @Before
    fun setUp() = runBlocking {
        backendClient = ImBackendHttpClient(OkHttpClient.Builder().build())

        val noxSession = backendClient.issueDevSession(httpBaseUrl, "nox-dev")
        noxToken = noxSession.token
        val bootstrap = backendClient.loadBootstrap(httpBaseUrl, noxToken)
        val conversation = bootstrap.conversations.first { it.contact.externalId == "leo-vance" }
        conversationId = conversation.conversationId
        latestHistoryBody = backendClient
            .loadHistory(httpBaseUrl, noxToken, conversationId)
            .messages
            .last()
            .body

        val counterpartSession = backendClient.issueDevSession(httpBaseUrl, "leo-vance")
        counterpartGateway = com.gkim.im.android.data.remote.realtime.RealtimeChatClient(
            OkHttpClient.Builder().build(),
            webSocketUrl,
        )
        counterpartGateway.connect(
            token = counterpartSession.token,
            endpointOverride = webSocketUrl,
        )
        waitFor { counterpartGateway.isConnected.value }

        setLiveApp()
        waitFor(timeoutMs = 15_000) {
            appContainer.messagingRepository.integrationState.value.phase == MessagingIntegrationPhase.Ready
        }
    }

    @After
    fun tearDown() {
        if (::counterpartGateway.isInitialized) {
            counterpartGateway.disconnect()
        }
    }

    @Test
    fun emulatorValidationCoversLiveRoundTripAndReloadRecovery() = runBlocking {
        val outboundBody = "Emulator outbound ${System.currentTimeMillis()}"
        val inboundBody = "Emulator reply ${System.currentTimeMillis()}"

        composeRule.waitUntil(15_000) { textExists("Leo Vance") }
        composeRule.onNodeWithText("Leo Vance").performClick()

        composeRule.waitUntil(15_000) { textExists(latestHistoryBody) }
        composeRule.onNodeWithTag("chat-composer-input").performTextReplacement(outboundBody)
        composeRule.onNodeWithTag("chat-send-button").performClick()

        val received = waitForEvent {
            it is ImGatewayEvent.MessageReceived && it.message.body == outboundBody
        } as ImGatewayEvent.MessageReceived

        counterpartGateway.markRead(conversationId, received.message.id)
        waitForMessageReceipt(outboundBody)

        counterpartGateway.sendMessage(
            recipientExternalId = "nox-dev",
            clientMessageId = "emu-reply-${System.currentTimeMillis()}",
            body = inboundBody,
        )

        composeRule.waitUntil(15_000) { textExists(inboundBody) }

        setLiveApp()
        waitFor(timeoutMs = 15_000) {
            appContainer.messagingRepository.integrationState.value.phase == MessagingIntegrationPhase.Ready
        }
        composeRule.waitUntil(15_000) { textExists("Leo Vance") }
        composeRule.onNodeWithText("Leo Vance").performClick()
        composeRule.waitUntil(15_000) { textExists(outboundBody) && textExists(inboundBody) }
    }

    private fun setLiveApp() {
        appContainer = DefaultAppContainer(composeRule.activity)
        runBlocking {
            appContainer.preferencesStore.setImHttpBaseUrl(httpBaseUrl)
            appContainer.preferencesStore.setImWebSocketUrl(webSocketUrl)
            appContainer.preferencesStore.setImDevUserExternalId("nox-dev")
        }

        if (!contentInitialized) {
            composeRule.setContent {
                var container by remember { mutableStateOf<AppContainer>(appContainer) }
                swapContainer = { replacement ->
                    container = replacement
                }

                key(container) {
                    GkimRootApp(container = container)
                }
            }
            composeRule.waitForIdle()
            contentInitialized = true
        } else {
            composeRule.runOnIdle {
                swapContainer(appContainer)
            }
        }
    }

    private suspend fun waitForEvent(predicate: (ImGatewayEvent) -> Boolean): ImGatewayEvent =
        withTimeout(15_000) {
            counterpartGateway.events.first(predicate)
        }

    private suspend fun waitForMessageReceipt(outboundBody: String) {
        withTimeout(15_000) {
            while (true) {
                val history = backendClient.loadHistory(
                    baseUrl = httpBaseUrl,
                    token = noxToken,
                    conversationId = conversationId,
                )
                val matched = history.messages.firstOrNull { it.body == outboundBody }
                if (matched != null && matched.deliveredAt != null && matched.readAt != null) {
                    return@withTimeout
                }
            }
        }
    }

    private suspend fun waitFor(timeoutMs: Long = 10_000, predicate: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!predicate()) {
                kotlinx.coroutines.delay(100)
            }
        }
    }

    private fun textExists(text: String): Boolean = runCatching {
        composeRule.onNodeWithText(text, substring = true).fetchSemanticsNode()
        true
    }.getOrDefault(false)
}
