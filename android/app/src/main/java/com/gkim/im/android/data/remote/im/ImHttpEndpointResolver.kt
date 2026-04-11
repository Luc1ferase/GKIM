package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.local.SessionStore
import com.gkim.im.android.data.repository.AppContainer
import kotlinx.coroutines.flow.first
import java.net.URI

const val DEFAULT_IM_HTTP_BASE_URL = "http://10.0.2.2:18080/"
const val DEFAULT_IM_WEBSOCKET_URL = "ws://10.0.2.2:18080/ws"

enum class ImHttpEndpointSource {
    Session,
    ImValidation,
    DefaultPublishedTarget,
}

data class ResolvedImHttpEndpoint(
    val baseUrl: String,
    val source: ImHttpEndpointSource,
    val isLoopbackHost: Boolean,
)

object ImHttpEndpointResolver {
    fun resolve(
        sessionBaseUrl: String?,
        validationBaseUrl: String?,
    ): ResolvedImHttpEndpoint {
        val sessionCandidate = sessionBaseUrl?.trim().orEmpty()
        if (sessionCandidate.isNotBlank()) {
            val normalized = normalize(sessionCandidate)
            return ResolvedImHttpEndpoint(
                baseUrl = normalized,
                source = ImHttpEndpointSource.Session,
                isLoopbackHost = isLoopbackHost(normalized),
            )
        }

        val validationCandidate = validationBaseUrl?.trim().orEmpty()
        if (validationCandidate.isNotBlank()) {
            val normalized = normalize(validationCandidate)
            return ResolvedImHttpEndpoint(
                baseUrl = normalized,
                source = ImHttpEndpointSource.ImValidation,
                isLoopbackHost = isLoopbackHost(normalized),
            )
        }

        return ResolvedImHttpEndpoint(
            baseUrl = DEFAULT_IM_HTTP_BASE_URL,
            source = ImHttpEndpointSource.DefaultPublishedTarget,
            isLoopbackHost = false,
        )
    }

    suspend fun resolve(
        sessionStore: SessionStore,
        preferencesStore: PreferencesStore,
    ): ResolvedImHttpEndpoint = resolve(
        sessionBaseUrl = sessionStore.baseUrl,
        validationBaseUrl = preferencesStore.imHttpBaseUrl.first(),
    )

    private fun normalize(baseUrl: String): String =
        if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    private fun isLoopbackHost(baseUrl: String): Boolean {
        val host = runCatching { URI(normalize(baseUrl)).host?.lowercase() }.getOrNull()
        return host == "127.0.0.1" || host == "localhost" || host == "0.0.0.0"
    }
}

suspend fun AppContainer.resolveImHttpEndpoint(): ResolvedImHttpEndpoint =
    ImHttpEndpointResolver.resolve(sessionStore = sessionStore, preferencesStore = preferencesStore)

fun authFailureMessage(
    appLanguage: AppLanguage,
    endpoint: ResolvedImHttpEndpoint,
    error: Throwable,
): String {
    val message = error.message.orEmpty()
    if (
        message.contains("401") ||
        message.contains("unauthorized", ignoreCase = true) ||
        message.contains("invalid credential", ignoreCase = true)
    ) {
        return appLanguage.pick("Invalid username or password", "用户名或密码错误")
    }

    val selectedBaseUrl = endpoint.baseUrl.removeSuffix("/")
    return if (endpoint.isLoopbackHost) {
        appLanguage.pick(
            "Cannot reach the backend at $selectedBaseUrl. The client is still targeting a loopback address; update Settings > IM Validation HTTP Base URL.",
            "无法连接到 $selectedBaseUrl。当前客户端仍在使用回环地址，请先到 设置 > IM验证 更新 HTTP 基础 URL。",
        )
    } else {
        appLanguage.pick(
            "Cannot reach the backend at $selectedBaseUrl. Check that the server is running and reachable from the emulator.",
            "无法连接到 $selectedBaseUrl。请检查服务端是否已启动，并确认该地址可被模拟器访问。",
        )
    }
}
