package com.gkim.im.android.feature.navigation

import androidx.test.platform.app.InstrumentationRegistry
import com.gkim.im.android.data.remote.im.DEFAULT_IM_DEVELOPER_BACKEND_ORIGIN
import com.gkim.im.android.data.remote.im.deriveWebSocketUrl
import com.gkim.im.android.data.remote.im.normalizeBackendOrigin

private const val LIVE_BACKEND_ORIGIN_ARG = "liveImBackendOrigin"
private const val LEGACY_LIVE_HTTP_ARG = "liveImHttpBaseUrl"
private const val LEGACY_LIVE_WS_ARG = "liveImWebSocketUrl"

internal object LiveEndpointOverrides {
    fun backendOrigin(): String {
        val explicitBackendOrigin = argumentOrBlank(LIVE_BACKEND_ORIGIN_ARG)
        if (explicitBackendOrigin.isNotBlank()) {
            return normalizeBackendOrigin(explicitBackendOrigin) ?: DEFAULT_IM_DEVELOPER_BACKEND_ORIGIN
        }

        val legacyHttpBaseUrl = argumentOrBlank(LEGACY_LIVE_HTTP_ARG)
        if (legacyHttpBaseUrl.isNotBlank()) {
            return normalizeBackendOrigin(legacyHttpBaseUrl) ?: DEFAULT_IM_DEVELOPER_BACKEND_ORIGIN
        }

        val legacyWebSocketUrl = argumentOrBlank(LEGACY_LIVE_WS_ARG)
        if (legacyWebSocketUrl.isNotBlank()) {
            return normalizeBackendOrigin(legacyWebSocketUrl.replaceFirst("wss://", "https://").replaceFirst("ws://", "http://").removeSuffix("/ws"))
                ?: DEFAULT_IM_DEVELOPER_BACKEND_ORIGIN
        }

        return DEFAULT_IM_DEVELOPER_BACKEND_ORIGIN
    }

    fun httpBaseUrl(): String = backendOrigin()

    fun webSocketUrl(): String = deriveWebSocketUrl(backendOrigin())

    private fun argumentOrBlank(key: String): String =
        InstrumentationRegistry.getArguments().getString(key)?.trim().orEmpty()
}
