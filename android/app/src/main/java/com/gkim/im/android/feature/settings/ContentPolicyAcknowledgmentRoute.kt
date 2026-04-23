package com.gkim.im.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.ContentPolicyCopy
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.shared.simpleViewModelFactory

@Composable
fun ContentPolicyAcknowledgmentRoute(
    container: AppContainer,
    onAccepted: () -> Unit,
    onBack: () -> Unit,
) {
    val sessionStore = container.sessionStore
    val backendClient = container.imBackendClient
    val vm: ContentPolicyAcknowledgmentViewModel = viewModel(
        factory = simpleViewModelFactory {
            ContentPolicyAcknowledgmentViewModel(
                submitter = { version ->
                    val baseUrl = sessionStore.baseUrl.orEmpty()
                    val token = sessionStore.token.orEmpty()
                    val response = backendClient.postContentPolicyAcknowledgment(
                        baseUrl = baseUrl,
                        token = token,
                        version = version,
                    )
                    response.acceptedAtMillis ?: System.currentTimeMillis()
                },
                preferencesStore = container.preferencesStore,
            )
        },
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    ContentPolicyAcknowledgmentScreen(
        uiState = uiState,
        onAccept = {
            vm.accept()
        },
        onAccepted = onAccepted,
        onBack = onBack,
    )
}

@Composable
internal fun ContentPolicyAcknowledgmentScreen(
    uiState: ContentPolicyAcknowledgmentUiState,
    onAccept: () -> Unit,
    onAccepted: () -> Unit,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    PageHeader(
        eyebrow = appLanguage.pick("Settings", "设置"),
        title = appLanguage.pick(
            ContentPolicyCopy.title.english,
            ContentPolicyCopy.title.chinese,
        ),
        description = appLanguage.pick(
            "Please read the content policy and confirm your acknowledgment to continue.",
            "请阅读内容政策并确认后继续。",
        ),
        leadingLabel = appLanguage.pick("Back", "返回"),
        onLeading = onBack,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-content-policy-ack-route"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-content-policy-ack-body"),
        ) {
            Text(
                text = appLanguage.pick(
                    ContentPolicyCopy.body.english,
                    ContentPolicyCopy.body.chinese,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurface,
                modifier = Modifier.testTag("settings-content-policy-ack-body-text"),
            )
        }

        val statusText = when {
            uiState.isAcknowledged -> appLanguage.pick(
                ContentPolicyCopy.acceptedCopy.english,
                ContentPolicyCopy.acceptedCopy.chinese,
            )
            uiState.isSubmitting -> appLanguage.pick(
                ContentPolicyCopy.accepting.english,
                ContentPolicyCopy.accepting.chinese,
            )
            uiState.errorMessage != null -> appLanguage.pick(
                ContentPolicyCopy.errorFallback.english,
                ContentPolicyCopy.errorFallback.chinese,
            )
            else -> null
        }
        if (statusText != null) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.errorMessage != null) AetherColors.Danger else AetherColors.OnSurfaceVariant,
                modifier = Modifier.testTag("settings-content-policy-ack-status"),
            )
        }

        Button(
            onClick = {
                if (uiState.isAcknowledged) {
                    onAccepted()
                } else {
                    onAccept()
                }
            },
            enabled = !uiState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-content-policy-ack-accept"),
        ) {
            Text(
                text = if (uiState.isAcknowledged) {
                    appLanguage.pick("Continue", "继续")
                } else {
                    appLanguage.pick(
                        ContentPolicyCopy.acceptCta.english,
                        ContentPolicyCopy.acceptCta.chinese,
                    )
                },
            )
        }
    }
}
