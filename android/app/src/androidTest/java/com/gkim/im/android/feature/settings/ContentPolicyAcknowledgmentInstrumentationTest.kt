package com.gkim.im.android.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.ContentPolicyCopy
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.feature.bootstrap.BootstrapAcknowledgmentDecision
import com.gkim.im.android.feature.bootstrap.BootstrapAcknowledgmentGate
import com.gkim.im.android.feature.bootstrap.BootstrapAcknowledgmentSnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentPolicyAcknowledgmentInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun freshInstallShowsAcknowledgmentThenTapAcceptEntersTavern() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                BootstrapAcknowledgmentTestHost(
                    isDebugBuild = false,
                    backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                        accepted = false,
                        version = "",
                    ),
                    localAcceptedMillis = null,
                    localAcceptedVersion = "",
                )
            }
        }

        composeRule.onNodeWithTag("settings-content-policy-ack-route").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-content-policy-ack-body-text").assertIsDisplayed()
        composeRule.onAllNodesWithTag("test-bootstrap-tavern-home").assertCountEquals(0)

        composeRule.onNodeWithTag("settings-content-policy-ack-accept").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings-content-policy-ack-accept").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("test-bootstrap-tavern-home").assertIsDisplayed()
        composeRule.onAllNodesWithTag("settings-content-policy-ack-route").assertCountEquals(0)
    }

    @Test
    fun subsequentLaunchSkipsAcknowledgmentWhenBackendAcceptsCurrentVersion() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                BootstrapAcknowledgmentTestHost(
                    isDebugBuild = false,
                    backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                        accepted = true,
                        version = ContentPolicyCopy.currentVersion,
                    ),
                    localAcceptedMillis = 1_700_000_000_000L,
                    localAcceptedVersion = ContentPolicyCopy.currentVersion,
                )
            }
        }

        composeRule.onNodeWithTag("test-bootstrap-tavern-home").assertIsDisplayed()
        composeRule.onAllNodesWithTag("settings-content-policy-ack-route").assertCountEquals(0)
    }

    @Test
    fun debugBuildSkipsAcknowledgmentEvenWhenBackendSaysUnaccepted() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                BootstrapAcknowledgmentTestHost(
                    isDebugBuild = true,
                    backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                        accepted = false,
                        version = "",
                    ),
                    localAcceptedMillis = null,
                    localAcceptedVersion = "",
                )
            }
        }

        composeRule.onNodeWithTag("test-bootstrap-tavern-home").assertIsDisplayed()
        composeRule.onAllNodesWithTag("settings-content-policy-ack-route").assertCountEquals(0)
    }

    @Test
    fun versionBumpForcesReacknowledgmentEvenWhenLocalHasStaleAcceptance() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                BootstrapAcknowledgmentTestHost(
                    isDebugBuild = false,
                    backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                        accepted = true,
                        version = "2025-01-01-v0",
                    ),
                    localAcceptedMillis = 1_600_000_000_000L,
                    localAcceptedVersion = "2025-01-01-v0",
                )
            }
        }

        composeRule.onNodeWithTag("settings-content-policy-ack-route").assertIsDisplayed()
        composeRule.onAllNodesWithTag("test-bootstrap-tavern-home").assertCountEquals(0)
    }

    @Test
    fun backendUnknownWithNoLocalAcceptanceRequiresAcknowledgment() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                BootstrapAcknowledgmentTestHost(
                    isDebugBuild = false,
                    backendSnapshot = BootstrapAcknowledgmentSnapshot.Unknown,
                    localAcceptedMillis = null,
                    localAcceptedVersion = "",
                )
            }
        }

        composeRule.onNodeWithTag("settings-content-policy-ack-route").assertIsDisplayed()
        composeRule.onAllNodesWithTag("test-bootstrap-tavern-home").assertCountEquals(0)
    }

    @Test
    fun backendUnknownWithLocalAcceptedAtCurrentVersionAllows() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                BootstrapAcknowledgmentTestHost(
                    isDebugBuild = false,
                    backendSnapshot = BootstrapAcknowledgmentSnapshot.Unknown,
                    localAcceptedMillis = 1_700_000_000_000L,
                    localAcceptedVersion = ContentPolicyCopy.currentVersion,
                )
            }
        }

        composeRule.onNodeWithTag("test-bootstrap-tavern-home").assertIsDisplayed()
        composeRule.onAllNodesWithTag("settings-content-policy-ack-route").assertCountEquals(0)
    }

    @Test
    fun acknowledgmentScreenRendersChineseCopyUnderChineseLocale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.Chinese) {
                BootstrapAcknowledgmentTestHost(
                    isDebugBuild = false,
                    backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                        accepted = false,
                        version = "",
                    ),
                    localAcceptedMillis = null,
                    localAcceptedVersion = "",
                )
            }
        }

        composeRule.onNodeWithTag("settings-content-policy-ack-route").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-content-policy-ack-body").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-content-policy-ack-accept").assertIsDisplayed()
    }

}

@Composable
private fun BootstrapAcknowledgmentTestHost(
    isDebugBuild: Boolean,
    backendSnapshot: BootstrapAcknowledgmentSnapshot,
    localAcceptedMillis: Long?,
    localAcceptedVersion: String,
) {
    val initialDecision = BootstrapAcknowledgmentGate.decide(
        isDebugBuild = isDebugBuild,
        backendSnapshot = backendSnapshot,
        localAcceptedAtMillis = localAcceptedMillis,
        localAcceptedVersion = localAcceptedVersion,
        currentVersion = ContentPolicyCopy.currentVersion,
    )
    var requiresAcknowledgment by remember {
        mutableStateOf(initialDecision == BootstrapAcknowledgmentDecision.RequireAcknowledgment)
    }
    var uiState by remember {
        mutableStateOf(
            ContentPolicyAcknowledgmentUiState(version = ContentPolicyCopy.currentVersion),
        )
    }

    if (requiresAcknowledgment) {
        ContentPolicyAcknowledgmentScreen(
            uiState = uiState,
            onAccept = {
                uiState = uiState.copy(
                    isAcknowledged = true,
                    isSubmitting = false,
                    errorMessage = null,
                )
            },
            onAccepted = { requiresAcknowledgment = false },
            onBack = { requiresAcknowledgment = false },
        )
    } else {
        TestTavernHome()
    }
}

@Composable
private fun TestTavernHome() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("test-bootstrap-tavern-home"),
    ) {
        Text(text = "Tavern home (test)")
    }
}
