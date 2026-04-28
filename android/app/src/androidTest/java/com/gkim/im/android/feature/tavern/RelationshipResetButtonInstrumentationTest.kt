package com.gkim.im.android.feature.tavern

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.AigcTask
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.MessageAttachment
import com.gkim.im.android.data.repository.MessagingIntegrationState
import com.gkim.im.android.data.repository.MessagingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §5.1 verification — drives the production `RelationshipResetButton` composable through the
 * three canonical paths (cancel-from-Armed / confirm-success / fail-then-retry) against a
 * fake `MessagingRepository` and pins the testTag matrix per §3.1 / §6.1 spec.
 */
@RunWith(AndroidJUnit4::class)
class RelationshipResetButtonInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val characterId = "daylight-listener"

    @Test
    fun cancelFromArmedReturnsToIdleAndDoesNotCallBackend() {
        val repository = RecordingMessagingRepository()
        var completed = false

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                RelationshipResetButton(
                    characterId = characterId,
                    repository = repository,
                    onCompleted = { completed = true },
                )
            }
        }

        composeRule.onNodeWithTag("relationship-reset-trigger").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-trigger").performClick()
        composeRule.onNodeWithTag("relationship-reset-confirmation-banner").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-cancel").performClick()
        composeRule.waitUntil(2_000) {
            composeRule.onAllNodesWithTag("relationship-reset-trigger")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("relationship-reset-trigger").assertIsDisplayed()
        assertTrue("Cancel must not produce a backend call", repository.resetCalls.isEmpty())
        assertEquals("Cancel must not fire onCompleted", false, completed)
    }

    @Test
    fun confirmFromArmedDispatchesAndAutoDismissesOnSuccess() {
        val repository = RecordingMessagingRepository()
        var completed = false

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                RelationshipResetButton(
                    characterId = characterId,
                    repository = repository,
                    onCompleted = { completed = true },
                )
            }
        }

        composeRule.onNodeWithTag("relationship-reset-trigger").performClick()
        composeRule.onNodeWithTag("relationship-reset-confirmation-banner").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-confirm").performClick()
        composeRule.waitUntil(5_000) { completed }
        assertTrue(completed)
        assertEquals(listOf(characterId), repository.resetCalls)
        // Completed phase auto-renders the Idle trigger again (per the when-branch fallthrough).
        composeRule.onNodeWithTag("relationship-reset-trigger").assertIsDisplayed()
    }

    @Test
    fun retryFromFailedAdvancesToSubmittingDirectlyWithoutReArming() {
        val repository = RecordingMessagingRepository(
            resetResults = ArrayDeque(
                listOf(
                    Result.failure(RuntimeException("network_failure")),
                    Result.success(Unit),
                ),
            ),
        )
        var completed = false

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                RelationshipResetButton(
                    characterId = characterId,
                    repository = repository,
                    onCompleted = { completed = true },
                )
            }
        }

        // First confirm flow ends in Failed — the inline error + retry are visible, the
        // confirmation banner is gone (we are not back in Armed).
        composeRule.onNodeWithTag("relationship-reset-trigger").performClick()
        composeRule.onNodeWithTag("relationship-reset-confirm").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("relationship-reset-error")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("relationship-reset-error").assertIsDisplayed()
        composeRule.onNodeWithTag("relationship-reset-retry").assertIsDisplayed()
        composeRule.onAllNodesWithTag("relationship-reset-confirmation-banner")
            .assertCountEquals(0)

        // Retry triggers a SECOND backend call directly without re-arming.
        composeRule.onNodeWithTag("relationship-reset-retry").performClick()
        composeRule.waitUntil(5_000) { completed }
        assertEquals(2, repository.resetCalls.size)
        assertEquals(characterId, repository.resetCalls[0])
        assertEquals(characterId, repository.resetCalls[1])
        assertTrue(completed)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals(
        expected: Int,
    ) {
        assertEquals(expected, fetchSemanticsNodes().size)
    }

    private class RecordingMessagingRepository(
        private val resetResults: ArrayDeque<Result<Unit>> = ArrayDeque(listOf(Result.success(Unit))),
    ) : MessagingRepository {
        val resetCalls = mutableListOf<String>()
        override val contacts: StateFlow<List<Contact>> = MutableStateFlow(emptyList())
        override val conversations: StateFlow<List<Conversation>> = MutableStateFlow(emptyList())
        override val integrationState: StateFlow<MessagingIntegrationState> =
            MutableStateFlow(MessagingIntegrationState())
        override fun conversation(conversationId: String): Flow<Conversation?> =
            conversations.map { _ -> null }
        override fun ensureConversation(contact: Contact, companionCardId: String?): Conversation =
            error("not used")
        override fun ensureStudioRoom(): Conversation = error("not used")
        override fun sendMessage(conversationId: String, body: String, attachment: MessageAttachment?) = Unit
        override fun appendAigcResult(conversationId: String, task: AigcTask) = Unit
        override fun loadConversationHistory(conversationId: String) = Unit
        override fun refreshBootstrap() = Unit
        override suspend fun resetRelationship(characterId: String): Result<Unit> {
            resetCalls += characterId
            return resetResults.removeFirstOrNull() ?: Result.success(Unit)
        }
    }
}
