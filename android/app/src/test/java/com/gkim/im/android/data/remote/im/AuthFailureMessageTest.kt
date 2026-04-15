package com.gkim.im.android.data.remote.im

import com.gkim.im.android.core.model.AppLanguage
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFailureMessageTest {
    @Test
    fun `unauthorized responses stay mapped to invalid credential copy`() {
        val message = authFailureMessage(
            appLanguage = AppLanguage.Chinese,
            endpoint = ResolvedImHttpEndpoint(
                baseUrl = "https://forward.example.com/",
                source = ImEndpointSource.DeveloperOverride,
                isLoopbackHost = false,
            ),
            error = IllegalStateException("401 unauthorized"),
        )

        assertTrue(message.contains("用户名或密码错误"))
    }

    @Test
    fun `loopback targets explain that the client url is still local`() {
        val message = authFailureMessage(
            appLanguage = AppLanguage.Chinese,
            endpoint = ResolvedImHttpEndpoint(
                baseUrl = "http://127.0.0.1:18080/",
                source = ImEndpointSource.DeveloperOverride,
                isLoopbackHost = true,
            ),
            error = IllegalStateException("failed to connect"),
        )

        assertTrue(message.contains("127.0.0.1:18080"))
        assertTrue(message.contains("本地地址"))
        assertTrue(message.contains("连接地址"))
    }

    @Test
    fun `reachable non loopback targets explain server reachability instead of credential failure`() {
        val message = authFailureMessage(
            appLanguage = AppLanguage.Chinese,
            endpoint = ResolvedImHttpEndpoint(
                baseUrl = "http://10.0.2.2:18080/",
                source = ImEndpointSource.BundledDefault,
                isLoopbackHost = false,
            ),
            error = IllegalStateException("timeout"),
        )

        assertTrue(message.contains("10.0.2.2:18080"))
        assertTrue(message.contains("客户端"))
        assertTrue(!message.contains("用户名或密码错误"))
    }

    @Test
    fun `missing validation target tells the operator to configure settings before retrying`() {
        val message = authFailureMessage(
            appLanguage = AppLanguage.Chinese,
            endpoint = ResolvedImHttpEndpoint(
                baseUrl = "",
                source = ImEndpointSource.MissingConfiguration,
                isLoopbackHost = false,
            ),
            error = IllegalStateException("missing endpoint"),
        )

        assertTrue(message.contains("连接信息"))
        assertTrue(message.contains("稍后再试"))
    }
}
