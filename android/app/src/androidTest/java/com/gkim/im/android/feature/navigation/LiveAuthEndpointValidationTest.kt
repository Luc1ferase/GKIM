package com.gkim.im.android.feature.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.data.remote.im.DEFAULT_IM_HTTP_BASE_URL
import com.gkim.im.android.data.remote.im.ImBackendHttpClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveAuthEndpointValidationTest {
    @Test
    fun emulatorCanRegisterLoginAndBootstrapThroughPublishedHostBridgeEndpoint() = runBlocking {
        val client = ImBackendHttpClient(OkHttpClient.Builder().build())
        val username = "codex_${System.currentTimeMillis()}"
        val password = "Passw0rd!"
        val displayName = "Codex Validation"

        val registerResponse = client.register(
            baseUrl = DEFAULT_IM_HTTP_BASE_URL,
            username = username,
            password = password,
            displayName = displayName,
        )
        val loginResponse = client.login(
            baseUrl = DEFAULT_IM_HTTP_BASE_URL,
            username = username,
            password = password,
        )
        val bootstrap = client.loadBootstrap(
            baseUrl = DEFAULT_IM_HTTP_BASE_URL,
            token = loginResponse.token,
        )

        assertEquals(username, registerResponse.user.externalId)
        assertEquals(username, loginResponse.user.externalId)
        assertEquals(username, bootstrap.user.externalId)
        assertTrue(registerResponse.token.isNotBlank())
        assertTrue(loginResponse.token.isNotBlank())
    }
}
