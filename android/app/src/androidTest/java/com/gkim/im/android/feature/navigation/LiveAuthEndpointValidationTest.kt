package com.gkim.im.android.feature.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
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
        val httpBaseUrl = LiveEndpointOverrides.httpBaseUrl()
        val username = "codex_${System.currentTimeMillis()}"
        val password = "Passw0rd!"
        val displayName = "Codex Validation"

        val registerResponse = client.register(
            baseUrl = httpBaseUrl,
            username = username,
            password = password,
            displayName = displayName,
        )
        val loginResponse = client.login(
            baseUrl = httpBaseUrl,
            username = username,
            password = password,
        )
        val bootstrap = client.loadBootstrap(
            baseUrl = httpBaseUrl,
            token = loginResponse.token,
        )

        assertEquals(username, registerResponse.user.externalId)
        assertEquals(username, loginResponse.user.externalId)
        assertEquals(username, bootstrap.user.externalId)
        assertTrue(registerResponse.token.isNotBlank())
        assertTrue(loginResponse.token.isNotBlank())
    }
}
