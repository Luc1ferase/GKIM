package com.gkim.im.android.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GradientPrimaryButton
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.remote.im.authFailureMessage
import com.gkim.im.android.data.remote.im.resolveImHttpEndpoint
import kotlinx.coroutines.launch

@Composable
fun RegisterRoute(
    container: AppContainer,
    onRegistered: () -> Unit,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .padding(horizontal = 28.dp, vertical = 40.dp)
            .testTag("register-screen"),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = appLanguage.pick("Create Account", "创建账号"),
            style = MaterialTheme.typography.headlineLarge,
            color = AetherColors.OnSurface,
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; errorMessage = null },
            label = { Text(appLanguage.pick("Username", "用户名")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("register-username"),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it; errorMessage = null },
            label = { Text(appLanguage.pick("Display Name", "显示名称")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("register-display-name"),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text(appLanguage.pick("Password", "密码")) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().testTag("register-password"),
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("register-error"),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        GradientPrimaryButton(
            label = if (isLoading) appLanguage.pick("Registering...", "注册中...") else appLanguage.pick("Register", "注册"),
            onClick = {
                if (isLoading) return@GradientPrimaryButton
                isLoading = true
                errorMessage = null
                scope.launch {
                    val endpoint = container.resolveImHttpEndpoint()
                    try {
                        val response = container.imBackendClient.register(endpoint.baseUrl, username.trim(), password, displayName.trim())
                        container.sessionStore.token = response.token
                        container.sessionStore.username = response.user.externalId
                        container.sessionStore.baseUrl = endpoint.baseUrl
                        onRegistered()
                    } catch (e: Exception) {
                        val msg = e.message ?: "Registration failed"
                        errorMessage = if (msg.contains("409") || msg.contains("username_taken", ignoreCase = true)) {
                            appLanguage.pick("Username is already taken", "用户名已被使用")
                        } else if (msg.contains("400")) {
                            appLanguage.pick("Invalid input — check username (3-20 chars), password (8+ chars), and display name", "输入无效 — 请检查用户名(3-20字符)、密码(8+字符)和显示名称")
                        } else {
                            authFailureMessage(appLanguage, endpoint, e)
                        }
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.testTag("register-submit"),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = appLanguage.pick("← Back", "← 返回"),
            style = MaterialTheme.typography.bodyLarge,
            color = AetherColors.Primary,
            modifier = Modifier
                .testTag("register-back")
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp)
                .let { mod ->
                    mod
                },
        )
    }
}
