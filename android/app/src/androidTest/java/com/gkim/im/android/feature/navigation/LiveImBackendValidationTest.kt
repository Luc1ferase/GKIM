package com.gkim.im.android.feature.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import android.content.Context
import com.gkim.im.android.data.local.AppPreferencesStore
import com.gkim.im.android.data.local.SessionStore
import com.gkim.im.android.data.remote.im.ImBackendHttpClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
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
    companion object {
        private const val TAG = "LiveImBackendValidation"
        private const val VALIDATION_TIMEOUT_MS = 45_000L
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val httpBaseUrl = LiveEndpointOverrides.httpBaseUrl()
    private val webSocketUrl = LiveEndpointOverrides.webSocketUrl()

    private lateinit var backendClient: ImBackendHttpClient
    private lateinit var counterpartGateway: com.gkim.im.android.data.remote.realtime.RealtimeChatClient
    private lateinit var appContainer: DefaultAppContainer
    private lateinit var swapContainer: (DefaultAppContainer) -> Unit
    private lateinit var noxToken: String
    private lateinit var counterpartToken: String
    private lateinit var conversationId: String
    private lateinit var latestHistoryBody: String
    private var contentInitialized = false

    @Before
    fun setUp() {
        runBlocking {
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
        counterpartToken = counterpartSession.token
        counterpartGateway = com.gkim.im.android.data.remote.realtime.RealtimeChatClient(
            OkHttpClient.Builder().build(),
            webSocketUrl,
        )
        counterpartGateway.connect(
            token = counterpartSession.token,
            endpointOverride = webSocketUrl,
        )
        waitFor(timeoutMs = VALIDATION_TIMEOUT_MS) { counterpartGateway.isConnected.value }

        setLiveApp()
        waitFor(timeoutMs = VALIDATION_TIMEOUT_MS) {
            appContainer.messagingRepository.integrationState.value.phase == MessagingIntegrationPhase.Ready
        }
        Log.d(
            TAG,
            "setUp ready with conversations=${appContainer.messagingRepository.conversations.value.map { it.id to it.contactName }}",
        )
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
        val missedBody = "Missed while reconnecting ${System.currentTimeMillis()}"

        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { nodeExists("conversation-row-$conversationId") }
        composeRule.onNodeWithTag("conversation-row-$conversationId").performClick()
        composeRule.onNodeWithTag("chat-screen").fetchSemanticsNode()
        Log.d(TAG, "chat screen opened for $conversationId")

        waitForConversationMessage(latestHistoryBody)
        Log.d(TAG, "history hydrated with latest body: $latestHistoryBody")
        composeRule.onNodeWithTag("chat-composer-input").performTextReplacement(outboundBody)
        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { composerText() == outboundBody }
        Log.d(TAG, "composer populated with outbound body: ${composerText()}")
        logRealtimeState("before-send-click")
        composeRule.onNodeWithTag("chat-send-button").performClick()
        Log.d(TAG, "outbound message submitted: $outboundBody")
        logRealtimeState("after-send-click")

        val received = try {
            waitForEvent {
                it is ImGatewayEvent.MessageReceived && it.message.body == outboundBody
            } as ImGatewayEvent.MessageReceived
        } catch (error: Throwable) {
            logSendFailureDiagnostics(outboundBody, error)
            throw error
        }
        Log.d(TAG, "counterpart received outbound message id=${received.message.id}")

        counterpartGateway.markRead(conversationId, received.message.id)
        waitForMessageReceipt(outboundBody)
        Log.d(TAG, "outbound message reached delivered+read state")

        counterpartGateway.sendMessage(
            recipientExternalId = "nox-dev",
            clientMessageId = "emu-reply-${System.currentTimeMillis()}",
            body = inboundBody,
        )
        Log.d(TAG, "counterpart sent inbound reply: $inboundBody")

        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { textExists(inboundBody) }
        Log.d(TAG, "inbound reply became visible in chat UI")

        val uploadedImage = backendClient.sendDirectImageMessage(
            baseUrl = httpBaseUrl,
            token = counterpartToken,
            request = SendDirectImageMessageRequestDto(
                recipientExternalId = "nox-dev",
                clientMessageId = "emu-image-${System.currentTimeMillis()}",
                body = "",
                contentType = "image/png",
                imageBase64 = "aGVsbG8taW1hZ2U=",
            ),
        )
        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) {
            nodeExists("chat-message-attachment-${uploadedImage.message.id}")
        }
        Log.d(TAG, "image message became visible in chat UI id=${uploadedImage.message.id}")

        appContainer.realtimeChatClient.disconnect()
        waitFor(timeoutMs = VALIDATION_TIMEOUT_MS) { !appContainer.realtimeChatClient.isConnected.value }
        Log.d(TAG, "app realtime connection forced closed")

        counterpartGateway.sendMessage(
            recipientExternalId = "nox-dev",
            clientMessageId = "emu-missed-${System.currentTimeMillis()}",
            body = missedBody,
        )
        Log.d(TAG, "counterpart sent missed message during disconnect: $missedBody")

        appContainer.realtimeChatClient.connect(
            token = noxToken,
            endpointOverride = webSocketUrl,
        )
        waitForConversationMessage(missedBody)
        Log.d(TAG, "missed message recovered after forced reconnect")

        setLiveApp()
        waitFor(timeoutMs = VALIDATION_TIMEOUT_MS) {
            appContainer.messagingRepository.integrationState.value.phase == MessagingIntegrationPhase.Ready
        }
        Log.d(TAG, "app container restarted and integration returned to Ready")
        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { nodeExists("conversation-row-$conversationId") }
        composeRule.onNodeWithTag("conversation-row-$conversationId").performClick()
        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { nodeExists("chat-screen") }
        composeRule.onNodeWithTag("chat-screen").fetchSemanticsNode()
        Log.d(TAG, "chat reopened after relaunch")
        val relaunchedHistory = backendClient.loadHistory(
            baseUrl = httpBaseUrl,
            token = noxToken,
            conversationId = conversationId,
        )
        Log.d(
            TAG,
            "backend history after relaunch count=${relaunchedHistory.messages.size} bodies=${relaunchedHistory.messages.takeLast(5).joinToString { it.body }}",
        )
        val relaunchedConversation = appContainer.messagingRepository.conversation(conversationId).first()
        Log.d(
            TAG,
            "repository conversation before relaunch wait count=${relaunchedConversation?.messages?.size ?: 0} bodies=${relaunchedConversation?.messages?.takeLast(5)?.joinToString { it.body }.orEmpty()}",
        )
        waitForConversationMessage(outboundBody)
        waitForConversationMessage(inboundBody)
        waitForConversationMessage(missedBody)
        composeRule.onNodeWithTag("chat-timeline").performScrollToNode(hasText(inboundBody, substring = true))
        composeRule.waitUntil(VALIDATION_TIMEOUT_MS) { textExists(inboundBody) }
        Log.d(TAG, "relaunch recovery confirmed with inbound body visible")
        Unit
    }

    private fun setLiveApp() {
        preparePersistedLiveState()
        appContainer = DefaultAppContainer(composeRule.activity)

        if (!contentInitialized) {
            composeRule.setContent {
                var container by remember { mutableStateOf<AppContainer>(appContainer) }
                swapContainer = { replacement ->
                    container = replacement
                }

                key(container) {
                    GkimRootApp(
                        container = container,
                        initialAuthStart = RootAuthStart.Authenticated,
                    )
                }
            }
            composeRule.waitForIdle()
            contentInitialized = true
        } else {
            composeRule.runOnIdle {
                swapContainer(appContainer)
            }
            composeRule.waitForIdle()
        }
    }

    private fun preparePersistedLiveState() = runBlocking {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        SessionStore(appContext).apply {
            clear()
            baseUrl = null
        }
        AppPreferencesStore(appContext).apply {
            setImBackendOrigin(httpBaseUrl)
            setImDevUserExternalId("nox-dev")
        }
    }

    private suspend fun waitForEvent(predicate: (ImGatewayEvent) -> Boolean): ImGatewayEvent =
        withTimeout(VALIDATION_TIMEOUT_MS) {
            counterpartGateway.events.first(predicate)
        }

    private suspend fun waitForMessageReceipt(outboundBody: String) {
        withTimeout(VALIDATION_TIMEOUT_MS) {
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

    private suspend fun waitForConversationMessage(body: String) {
        withTimeout(VALIDATION_TIMEOUT_MS) {
            appContainer.messagingRepository.conversation(conversationId).first { conversation ->
                conversation?.messages?.any { it.body == body } == true
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

    private fun composerText(): String {
        val semanticsNode = composeRule.onNodeWithTag("chat-composer-input").fetchSemanticsNode()
        val editableText = runCatching { semanticsNode.config[SemanticsProperties.EditableText].text }.getOrNull()
        val text = runCatching {
            semanticsNode.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        }.getOrNull()
        return buildString {
            editableText?.let(::append)
            text?.let(::append)
        }
    }

    private fun logRealtimeState(label: String) {
        Log.d(
            TAG,
            "$label appConnected=${appContainer.realtimeChatClient.isConnected.value} " +
                "counterpartConnected=${counterpartGateway.isConnected.value} " +
                "integration=${appContainer.messagingRepository.integrationState.value}",
        )
    }

    private fun logSendFailureDiagnostics(outboundBody: String, error: Throwable) = runBlocking {
        logRealtimeState("send-failure")
        Log.d(TAG, "send-failure composerText=${composerText()}")
        val history = backendClient.loadHistory(
            baseUrl = httpBaseUrl,
            token = noxToken,
            conversationId = conversationId,
        )
        Log.d(
            TAG,
            "send-failure backendHistory bodies=${history.messages.takeLast(5).joinToString { it.body }} matched=${history.messages.any { it.body == outboundBody }} error=${error.message}",
        )
    }

    private fun textExists(text: String): Boolean = runCatching {
        composeRule.onNodeWithText(text, substring = true).fetchSemanticsNode()
        true
    }.getOrDefault(false)

    private fun nodeExists(tag: String): Boolean = runCatching {
        composeRule.onNodeWithTag(tag).fetchSemanticsNode()
        true
    }.getOrDefault(false)
}
