package com.gkim.im.android.data.repository

import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.CompanionTurnPendingListDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImGatewayEvent
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.SendDirectMessageResultDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import com.gkim.im.android.data.remote.realtime.RealtimeGateway
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * §4.1 verification — `LiveCompanionTurnRepository.exportConversation(...)` calls
 * `GET /api/conversations/:conversationId/export?format=...&pathOnly=...`, wraps the JSONL
 * response body + a deterministic filename + `application/x-ndjson` content-type into
 * `ExportedChatPayload`, and remaps wire failures to stable error codes
 * (`404_unknown_conversation`, `unsupported_format`, `network_failure`).
 */
class LiveCompanionTurnRepositoryExportConversationTest {

    private val conversationId = "conversation-export-smoke-1234567890"

    private fun buildRepo(
        backend: FakeBackendClient,
        scope: kotlinx.coroutines.CoroutineScope,
        baseUrl: String? = "http://fake",
        token: String? = "token",
    ): LiveCompanionTurnRepository = LiveCompanionTurnRepository(
        default = DefaultCompanionTurnRepository(),
        backendClient = backend,
        gateway = FakeGateway(),
        scope = scope,
        baseUrlProvider = { baseUrl },
        tokenProvider = { token },
    )

    @Test
    fun `exportConversation forwards format and pathOnly to the wire call`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(exportResponse = { _, _ -> "{}\n" })
            val repo = buildRepo(backend, backgroundScope)
            repo.exportConversation(conversationId, format = "jsonl", pathOnly = true)
            val call = backend.exportCalls.single()
            assertEquals(conversationId, call.conversationId)
            assertEquals("jsonl", call.format)
            assertEquals(true, call.pathOnly)
        }

    @Test
    fun `exportConversation pathOnly true emits active-path filename`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(exportResponse = { _, _ -> "{}\n" })
            val repo = buildRepo(backend, backgroundScope)
            val result = repo.exportConversation(conversationId, format = "jsonl", pathOnly = true)
            val payload = result.getOrThrow()
            assertEquals("chat-export-active-path_conversa.jsonl", payload.filename)
            assertEquals("application/x-ndjson", payload.contentType)
            assertEquals("{}\n", payload.bytes.toString(Charsets.UTF_8))
        }

    @Test
    fun `exportConversation pathOnly false emits full-tree filename`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(exportResponse = { _, _ -> "{\"role\":\"u\"}\n" })
            val repo = buildRepo(backend, backgroundScope)
            val result = repo.exportConversation(conversationId, format = "jsonl", pathOnly = false)
            val payload = result.getOrThrow()
            assertEquals("chat-export-full-tree_conversa.jsonl", payload.filename)
            val call = backend.exportCalls.single()
            assertEquals(false, call.pathOnly)
        }

    @Test
    fun `exportConversation truncates short conversation id without crashing`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(exportResponse = { _, _ -> "" })
            val repo = buildRepo(backend, backgroundScope)
            val result = repo.exportConversation(
                conversationId = "abc",
                format = "jsonl",
                pathOnly = true,
            )
            assertEquals("chat-export-active-path_abc.jsonl", result.getOrThrow().filename)
        }

    @Test
    fun `exportConversation 404 maps to 404_unknown_conversation`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(
                exportResponse = { _, _ -> throw httpException(404, "{\"error\":\"not_found\"}") },
            )
            val repo = buildRepo(backend, backgroundScope)
            val result = repo.exportConversation(conversationId, format = "jsonl", pathOnly = true)
            assertTrue(result.isFailure)
            assertEquals("404_unknown_conversation", result.exceptionOrNull()?.message)
        }

    @Test
    fun `exportConversation 400 unsupported_format body maps to unsupported_format`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(
                exportResponse = { _, _ ->
                    throw httpException(400, "{\"error\":\"unsupported_format\",\"message\":\"only jsonl\"}")
                },
            )
            val repo = buildRepo(backend, backgroundScope)
            val result = repo.exportConversation(conversationId, format = "csv", pathOnly = true)
            assertTrue(result.isFailure)
            assertEquals("unsupported_format", result.exceptionOrNull()?.message)
        }

    @Test
    fun `exportConversation IO failure maps to network_failure`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(exportResponse = { _, _ -> throw IOException("boom") })
            val repo = buildRepo(backend, backgroundScope)
            val result = repo.exportConversation(conversationId, format = "jsonl", pathOnly = true)
            assertTrue(result.isFailure)
            assertEquals("network_failure", result.exceptionOrNull()?.message)
        }

    @Test
    fun `exportConversation fails fast with no base url and never calls backend`() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = FakeBackendClient(exportResponse = { _, _ -> "" })
            val repo = buildRepo(backend, backgroundScope, baseUrl = null)
            val result = repo.exportConversation(conversationId, format = "jsonl", pathOnly = true)
            assertTrue(result.isFailure)
            assertTrue(backend.exportCalls.isEmpty())
        }

    private fun httpException(code: Int, body: String): HttpException {
        val errBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Unit>(code, errBody))
    }

    private data class CapturedExport(
        val conversationId: String,
        val format: String,
        val pathOnly: Boolean?,
    )

    private class FakeGateway : RealtimeGateway {
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(false)
        override val lastFailure: StateFlow<String?> = MutableStateFlow(null)
        override val events: SharedFlow<ImGatewayEvent> = MutableSharedFlow(extraBufferCapacity = 1)
        override fun connect(token: String?, endpointOverride: String?) = Unit
        override fun send(payload: String): Boolean = true
        override fun sendMessage(recipientExternalId: String, clientMessageId: String?, body: String): Boolean = true
        override fun markRead(conversationId: String, messageId: String): Boolean = true
        override fun disconnect() = Unit
    }

    private class FakeBackendClient(
        private val exportResponse: ((conversationId: String, pathOnly: Boolean?) -> String)? = null,
    ) : ImBackendClient {
        val exportCalls = mutableListOf<CapturedExport>()

        override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto = error("n/a")
        override suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto = error("n/a")
        override suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto = error("n/a")
        override suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto> = error("n/a")
        override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto = error("n/a")
        override suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto> = error("n/a")
        override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto = error("n/a")
        override suspend fun sendDirectImageMessage(baseUrl: String, token: String, request: SendDirectImageMessageRequestDto): SendDirectMessageResultDto = error("n/a")
        override suspend fun loadHistory(baseUrl: String, token: String, conversationId: String, limit: Int, before: String?): MessageHistoryPageDto = error("n/a")

        override suspend fun listPendingCompanionTurns(
            baseUrl: String,
            token: String,
        ): CompanionTurnPendingListDto = CompanionTurnPendingListDto(emptyList())

        override suspend fun exportConversation(
            baseUrl: String,
            token: String,
            conversationId: String,
            format: String,
            pathOnly: Boolean?,
        ): String {
            exportCalls += CapturedExport(conversationId, format, pathOnly)
            return exportResponse?.invoke(conversationId, pathOnly) ?: error("export response not configured")
        }
    }
}
