package com.gkim.im.android.feature.tavern

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.model.AppLanguage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §6.2 verification — seeds a companion with two conversations + two memory pins, drives
 * the §6.1 affordance through the two-step gate (arm + confirm), and asserts the tavern
 * surface shows zero conversations and an empty memory record after the reset's
 * `applyResetEffect` runs. Also verifies the cancel-from-Armed and retry-from-Failed
 * branches per §6.1 + §9.2 spec contracts.
 *
 * Self-contained: no live backend. The [ResetAffordanceHost] composable owns its own
 * conversation + memory caches as `mutableStateOf`; the §6.1 affordance state's transitions
 * drive the cache mutations exactly the same way the next-layer wire-up will (a
 * LiveCompanionTurnRepository will project repository-state changes; here a Compose
 * `LaunchedEffect` keyed on the affordance phase synchronously simulates the network
 * round-trip + cache application).
 */
@RunWith(AndroidJUnit4::class)
class RelationshipResetInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun armingThenConfirmingClearsPairStateWhileCancelKeepsItIntact() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ResetAffordanceHost(
                    characterId = "daylight-listener",
                    initialConversations = listOf("conv-A", "conv-B"),
                    initialMemoryPins = listOf("memory-A", "memory-B"),
                )
            }
        }

        // Initial state — both conversations + both pins visible.
        composeRule.onNodeWithTag("relationship-reset-conv-conv-A").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-conv-conv-B").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-memory-pin-memory-A").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-memory-pin-memory-B").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-confirmation-banner").assertDoesNotExist()

        // First tap arms — confirmation banner appears, conversations still visible.
        composeRule.onNodeWithTag("relationship-reset-trigger").performClick()
        composeRule.onNodeWithTag("relationship-reset-confirmation-banner").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-conv-conv-A").assertIsDisplayed()

        // Cancel returns to Idle — banner disappears, conversations + pins intact.
        composeRule.onNodeWithTag("relationship-reset-cancel").performClick()
        composeRule.onNodeWithTag("relationship-reset-confirmation-banner").assertDoesNotExist()
        composeRule.onNodeWithTag("relationship-reset-conv-conv-A").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-conv-conv-B").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-memory-pin-memory-A").assertIsDisplayed()

        // Re-arm and confirm — phase advances Armed → Submitting → Completed (the host's
        // synthetic completion fires synchronously) → applyResetEffect clears the caches.
        composeRule.onNodeWithTag("relationship-reset-trigger").performClick()
        composeRule.onNodeWithTag("relationship-reset-confirm").performClick()
        composeRule.waitForIdle()

        // Final state — zero conversations + zero memory pins for this companion.
        composeRule.onNodeWithTag("relationship-reset-conv-conv-A").assertDoesNotExist()
        composeRule.onNodeWithTag("relationship-reset-conv-conv-B").assertDoesNotExist()
        composeRule.onNodeWithTag("relationship-reset-memory-pin-memory-A").assertDoesNotExist()
        composeRule.onNodeWithTag("relationship-reset-memory-pin-memory-B").assertDoesNotExist()
        composeRule.onNodeWithTag("relationship-reset-empty-state").assertIsDisplayed()
    }

    @Test
    fun retryFromFailedAdvancesWithoutReArmingTheTwoStepGate() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ResetAffordanceHost(
                    characterId = "architect-oracle",
                    initialConversations = listOf("conv-only-one"),
                    initialMemoryPins = emptyList(),
                    forceFirstSubmitToFail = true,
                )
            }
        }

        // Arm + confirm — first attempt fails → inline error appears; conversation persists.
        composeRule.onNodeWithTag("relationship-reset-trigger").performClick()
        composeRule.onNodeWithTag("relationship-reset-confirm").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("relationship-reset-error-banner").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-conv-conv-only-one").assertIsDisplayed()

        // Retry — does NOT pass through the two-step gate again; succeeds → cache clears.
        composeRule.onNodeWithTag("relationship-reset-retry").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("relationship-reset-conv-conv-only-one").assertDoesNotExist()
        composeRule.onNodeWithTag("relationship-reset-empty-state").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-error-banner").assertDoesNotExist()
    }
}

/**
 * Minimal Compose surface that mirrors how the next-layer wire-up will project the
 * §6.1 affordance state: a trigger button, a two-step confirmation banner, an inline
 * error + retry on failure, and a list of conversations + memory pins that the
 * affordance's [applyResetEffect] mutates on successful reset.
 */
@Composable
private fun ResetAffordanceHost(
    characterId: String,
    initialConversations: List<String>,
    initialMemoryPins: List<String>,
    forceFirstSubmitToFail: Boolean = false,
) {
    var affordance by remember {
        mutableStateOf(RelationshipResetAffordanceState(characterId = characterId))
    }
    var conversations by remember { mutableStateOf(initialConversations) }
    var memoryPins by remember { mutableStateOf(initialMemoryPins) }
    var firstSubmitConsumed by remember { mutableStateOf(false) }

    // Synthetic network round-trip: when phase transitions to Submitting, advance to
    // Completed (or Failed on the first attempt if forceFirstSubmitToFail). On Completed,
    // apply the effect to clear pair-scoped caches.
    LaunchedEffect(affordance.phase) {
        if (affordance.phase == RelationshipResetPhase.Submitting) {
            if (forceFirstSubmitToFail && !firstSubmitConsumed) {
                firstSubmitConsumed = true
                affordance = affordance.markFailed("network_error")
            } else {
                affordance = affordance.markCompleted()
                affordance.applyResetEffect()?.let { effect ->
                    if (effect.clearConversationsForCharacter == characterId) {
                        conversations = emptyList()
                    }
                    if (effect.clearMemoryForCharacter == characterId) {
                        memoryPins = emptyList()
                    }
                }
            }
        }
    }

    Column {
        // Conversations region.
        if (conversations.isEmpty() && memoryPins.isEmpty()) {
            Text(
                text = "No conversations or memory for this companion.",
                modifier = Modifier.testTag("relationship-reset-empty-state"),
            )
        }
        conversations.forEach { conv ->
            Text(
                text = conv,
                modifier = Modifier.testTag("relationship-reset-conv-$conv"),
            )
        }
        memoryPins.forEach { pin ->
            Text(
                text = pin,
                modifier = Modifier.testTag("relationship-reset-memory-pin-$pin"),
            )
        }

        // Trigger button — arms the affordance.
        Text(
            text = "Reset relationship",
            modifier = Modifier
                .testTag("relationship-reset-trigger")
                .clickable { affordance = affordance.arm() },
        )

        // Confirmation banner — visible only when Armed.
        if (affordance.phase == RelationshipResetPhase.Armed) {
            Row(modifier = Modifier.testTag("relationship-reset-confirmation-banner")) {
                Text(
                    text = "Confirm reset",
                    modifier = Modifier
                        .testTag("relationship-reset-confirm")
                        .clickable { affordance = affordance.confirm() },
                )
                Text(
                    text = "Cancel",
                    modifier = Modifier
                        .testTag("relationship-reset-cancel")
                        .clickable { affordance = affordance.cancel() },
                )
            }
        }

        // Inline error + retry — visible only when Failed.
        if (affordance.phase == RelationshipResetPhase.Failed) {
            Row(modifier = Modifier.testTag("relationship-reset-error-banner")) {
                Text(text = "Reset failed: ${affordance.errorCode}")
                Text(
                    text = "Retry",
                    modifier = Modifier
                        .testTag("relationship-reset-retry")
                        .clickable { affordance = affordance.retry() },
                )
            }
        }
    }
}
