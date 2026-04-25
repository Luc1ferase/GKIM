package com.gkim.im.android.data.remote.im

import com.gkim.im.android.BuildConfig
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.data.local.PreferencesStore
import com.gkim.im.android.data.local.SessionStore
import com.gkim.im.android.data.repository.AppContainer
import kotlinx.coroutines.flow.first
import java.net.URI

const val DEFAULT_IM_DEVELOPER_BACKEND_ORIGIN = "https://chat.lastxuans.sbs/"
const val DEFAULT_IM_HTTP_BASE_URL = DEFAULT_IM_DEVELOPER_BACKEND_ORIGIN
const val DEFAULT_IM_WEBSOCKET_URL = "wss://chat.lastxuans.sbs/ws"

private const val IM_REALTIME_PATH_SEGMENT = "ws"

enum class ImEndpointSource {
    Session,
    DeveloperOverride,
    BundledDefault,
    MissingConfiguration,
}

typealias ImHttpEndpointSource = ImEndpointSource

data class ResolvedImHttpEndpoint(
    val baseUrl: String,
    val source: ImEndpointSource,
    val isLoopbackHost: Boolean,
    val webSocketUrl: String = deriveWebSocketUrl(baseUrl),
    val backendOrigin: String = baseUrl,
    val httpBaseUrl: String = baseUrl,
    val isCleartext: Boolean = isCleartextOrigin(baseUrl),
)

object ImHttpEndpointResolver {
    fun resolve(
        sessionBaseUrl: String?,
        developerOverrideOrigin: String?,
        shippedBackendOrigin: String,
        allowDeveloperOverrides: Boolean,
    ): ResolvedImHttpEndpoint {
        normalizeBackendOrigin(sessionBaseUrl)?.let { normalized ->
            return resolvedEndpoint(normalized, ImEndpointSource.Session)
        }

        val developerCandidate = developerOverrideOrigin?.trim().orEmpty()
        if (developerCandidate.isNotBlank()) {
            if (!allowDeveloperOverrides) {
                return bundledOrMissing(shippedBackendOrigin)
            }

            normalizeBackendOrigin(developerCandidate)?.let { normalized ->
                return resolvedEndpoint(normalized, ImEndpointSource.DeveloperOverride)
            }

            return missingConfiguration()
        }

        return bundledOrMissing(shippedBackendOrigin)
    }

    suspend fun resolve(
        sessionStore: SessionStore,
        preferencesStore: PreferencesStore,
        shippedBackendOrigin: String = BuildConfig.IM_BACKEND_ORIGIN,
        allowDeveloperOverrides: Boolean = BuildConfig.DEBUG,
    ): ResolvedImHttpEndpoint = resolve(
        sessionBaseUrl = sessionStore.baseUrl,
        developerOverrideOrigin = preferencesStore.imBackendOrigin.first(),
        shippedBackendOrigin = shippedBackendOrigin,
        allowDeveloperOverrides = allowDeveloperOverrides,
    )

    private fun bundledOrMissing(shippedBackendOrigin: String): ResolvedImHttpEndpoint {
        normalizeBackendOrigin(shippedBackendOrigin)?.let { normalized ->
            return resolvedEndpoint(normalized, ImEndpointSource.BundledDefault)
        }
        return missingConfiguration()
    }

    private fun resolvedEndpoint(
        backendOrigin: String,
        source: ImEndpointSource,
    ): ResolvedImHttpEndpoint {
        val normalized = normalizeBackendOrigin(backendOrigin).orEmpty()
        return ResolvedImHttpEndpoint(
            baseUrl = normalized,
            source = source,
            isLoopbackHost = isLoopbackHost(normalized),
            webSocketUrl = deriveWebSocketUrl(normalized),
            backendOrigin = normalized,
            httpBaseUrl = normalized,
            isCleartext = isCleartextOrigin(normalized),
        )
    }

    private fun missingConfiguration(): ResolvedImHttpEndpoint =
        ResolvedImHttpEndpoint(
            baseUrl = "",
            source = ImEndpointSource.MissingConfiguration,
            isLoopbackHost = false,
            webSocketUrl = "",
            backendOrigin = "",
            httpBaseUrl = "",
            isCleartext = false,
        )
}

suspend fun AppContainer.resolveImHttpEndpoint(): ResolvedImHttpEndpoint =
    ImHttpEndpointResolver.resolve(sessionStore = sessionStore, preferencesStore = preferencesStore)

fun resolveStoredImBackendOrigin(
    storedBackendOrigin: String,
    legacyHttpBaseUrl: String,
    legacyWebSocketUrl: String,
    shippedBackendOrigin: String,
): String {
    normalizeBackendOrigin(storedBackendOrigin)?.let { return it }
    normalizeBackendOrigin(legacyHttpBaseUrl)?.let { return it }
    deriveBackendOriginFromWebSocketUrl(legacyWebSocketUrl)?.let { return it }
    return normalizeBackendOrigin(shippedBackendOrigin).orEmpty()
}

fun normalizeImBackendOriginForStorage(value: String): String {
    val candidate = value.trim()
    if (candidate.isBlank()) return ""
    return normalizeBackendOrigin(candidate) ?: candidate
}

fun normalizeBackendOrigin(value: String?): String? {
    val candidate = value?.trim().orEmpty()
    if (candidate.isBlank()) return null
    return runCatching {
        val uri = URI(candidate)
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        if (scheme !in setOf("http", "https") || host.isNullOrBlank()) {
            null
        } else {
            URI(
                scheme,
                uri.userInfo,
                host,
                uri.port,
                normalizePath(uri.path),
                null,
                null,
            ).toString()
        }
    }.getOrNull()
}

fun deriveWebSocketUrl(backendOrigin: String): String {
    val normalized = normalizeBackendOrigin(backendOrigin).orEmpty()
    if (normalized.isBlank()) return ""
    val uri = URI(normalized)
    val webSocketScheme = if (uri.scheme.equals("https", ignoreCase = true)) "wss" else "ws"
    return URI(
        webSocketScheme,
        uri.userInfo,
        uri.host,
        uri.port,
        normalizeRealtimePath(uri.path),
        null,
        null,
    ).toString()
}

fun deriveBackendOriginFromWebSocketUrl(value: String?): String? {
    val candidate = value?.trim().orEmpty()
    if (candidate.isBlank()) return null
    return runCatching {
        val uri = URI(candidate)
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        if (scheme !in setOf("ws", "wss") || host.isNullOrBlank()) {
            null
        } else {
            val httpScheme = if (scheme == "wss") "https" else "http"
            val path = normalizePath(removeRealtimeSuffix(uri.path))
            URI(
                httpScheme,
                uri.userInfo,
                host,
                uri.port,
                path,
                null,
                null,
            ).toString()
        }
    }.getOrNull()
}

private fun normalizePath(path: String?): String {
    val candidate = path?.takeIf { it.isNotBlank() } ?: "/"
    return if (candidate.endsWith("/")) candidate else "$candidate/"
}

private fun normalizeRealtimePath(path: String?): String {
    val basePath = normalizePath(path).removeSuffix("/")
    return if (basePath == "") "/$IM_REALTIME_PATH_SEGMENT" else "$basePath/$IM_REALTIME_PATH_SEGMENT"
}

private fun removeRealtimeSuffix(path: String?): String {
    val candidate = path?.trim().orEmpty()
    if (candidate.isBlank()) return "/"
    val suffix = "/$IM_REALTIME_PATH_SEGMENT"
    return if (candidate.endsWith(suffix)) {
        candidate.removeSuffix(suffix).ifBlank { "/" }
    } else {
        candidate
    }
}

private fun isLoopbackHost(baseUrl: String): Boolean {
    val host = runCatching { URI(baseUrl).host?.lowercase() }.getOrNull()
    return host == "127.0.0.1" || host == "localhost" || host == "0.0.0.0" || host == "10.0.2.2"
}

private fun isCleartextOrigin(baseUrl: String): Boolean =
    runCatching { URI(baseUrl).scheme.equals("http", ignoreCase = true) }.getOrDefault(false)

fun authFailureMessage(
    appLanguage: AppLanguage,
    endpoint: ResolvedImHttpEndpoint,
    error: Throwable,
): String {
    val message = error.message.orEmpty()
    if (endpoint.httpBaseUrl.isBlank()) {
        return appLanguage.pick(
            "Connection details are unavailable right now. Please try again later.",
            "当前连接信息暂不可用，请稍后再试。",
        )
    }

    if (
        message.contains("401") ||
        message.contains("unauthorized", ignoreCase = true) ||
        message.contains("invalid credential", ignoreCase = true)
    ) {
        return appLanguage.pick("Invalid username or password", "用户名或密码错误")
    }

    val selectedBaseUrl = endpoint.httpBaseUrl.removeSuffix("/")
    return if (endpoint.isLoopbackHost) {
        appLanguage.pick(
            "Cannot reach the backend at $selectedBaseUrl. The app is still using a local address; update the connection address and try again.",
            "无法连接到 $selectedBaseUrl。当前应用仍在使用本地地址，请更新连接地址后重试。",
        )
    } else {
        appLanguage.pick(
            "Cannot reach the backend at $selectedBaseUrl. Check that the server is running and reachable from the client.",
            "无法连接到 $selectedBaseUrl。请检查服务端是否已启动，并确认当前客户端可以访问该地址。",
        )
    }
}
