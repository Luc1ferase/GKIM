package com.gkim.im.android.data.remote.aigc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AigcProviderClientsTest {
    @Test
    fun `hunyuan client submits polls and returns the generated image url`() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"created":1776067137,"request_id":"req-1","data":[{"url":"https://cdn.example.com/hunyuan.png"}]}"""
            )
        )
        server.start()

        val client = HunyuanImageProviderClient(
            baseUrl = server.url("/").toString(),
            httpClient = OkHttpClient(),
        )

        val result = client.generate(
            RemoteAigcGenerateRequest(
                model = "hy-image-v3.0",
                prompt = "Render a moonlit cyberpunk skyline",
                apiKey = "sk-hunyuan",
            )
        )

        val submitRequest = server.takeRequest()
        val submitBody = submitRequest.body.readUtf8()

        assertEquals("/v1/api/image/lite", submitRequest.path)
        assertEquals("Bearer sk-hunyuan", submitRequest.getHeader("Authorization"))
        assertTrue(submitBody.contains("\"model\":\"hy-image-v3.0\""))
        assertTrue(submitBody.contains("\"prompt\":\"Render a moonlit cyberpunk skyline\""))
        assertTrue(submitBody.contains("\"rsp_img_type\":\"url\""))
        assertEquals("https://cdn.example.com/hunyuan.png", result.outputUrl)
        assertEquals("req-1", result.remoteId)

        server.shutdown()
    }

    @Test
    fun `hunyuan client surfaces provider-side generation failures`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"quota exceeded","code":"invalid_api_key"}}"""))
        server.start()

        val client = HunyuanImageProviderClient(
            baseUrl = server.url("/").toString(),
            httpClient = OkHttpClient(),
        )

        val failure = runCatching {
            client.generate(
                RemoteAigcGenerateRequest(
                    model = "hy-image-v3.0",
                    prompt = "Render a failure case",
                    apiKey = "sk-hunyuan",
                )
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(failure?.message.orEmpty().contains("quota exceeded"))

        server.shutdown()
    }

    @Test
    fun `tongyi client posts a bearer-auth multimodal request and returns the first image url`() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "request_id":"req-1",
                  "output":{
                    "choices":[
                      {
                        "message":{
                          "content":[
                            {"image":"https://cdn.example.com/tongyi.png","type":"image"}
                          ]
                        }
                      }
                    ]
                  }
                }
                """.trimIndent()
            )
        )
        server.start()

        val client = TongyiImageProviderClient(
            baseUrl = server.url("/").toString(),
            httpClient = OkHttpClient(),
        )

        val result = client.generate(
            RemoteAigcGenerateRequest(
                model = "wan2.7-image",
                prompt = "Render a clean product poster",
                apiKey = "sk-tongyi",
                imageBase64 = "BASE64_IMAGE",
                imageMimeType = "image/png",
            )
        )

        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertEquals("/api/v1/services/aigc/multimodal-generation/generation", request.path)
        assertEquals("Bearer sk-tongyi", request.getHeader("Authorization"))
        assertTrue(body.contains("\"model\":\"wan2.7-image\""))
        assertTrue(body.contains("\"image\":\"data:image/png;base64,BASE64_IMAGE\""))
        assertTrue(body.contains("\"text\":\"Render a clean product poster\""))
        assertEquals("https://cdn.example.com/tongyi.png", result.outputUrl)
        assertEquals("req-1", result.remoteId)

        server.shutdown()
    }

    @Test
    fun `tongyi client maps non-success responses into provider errors`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"code":"InvalidApiKey","message":"No API key"}"""))
        server.start()

        val client = TongyiImageProviderClient(
            baseUrl = server.url("/").toString(),
            httpClient = OkHttpClient(),
        )

        val failure = runCatching {
            client.generate(
                RemoteAigcGenerateRequest(
                    model = "wan2.7-image",
                    prompt = "Render a failure case",
                    apiKey = "sk-tongyi",
                )
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(failure?.message.orEmpty().contains("No API key"))

        server.shutdown()
    }

    @Test
    fun `tongyi client defaults to the beijing dashscope endpoint`() {
        val client = TongyiImageProviderClient(
            httpClient = OkHttpClient(),
        )

        val baseUrlField = TongyiImageProviderClient::class.java.getDeclaredField("baseUrl")
        baseUrlField.isAccessible = true
        val baseUrl = baseUrlField.get(client) as String

        assertEquals("https://dashscope.aliyuncs.com/", baseUrl)
    }
}
