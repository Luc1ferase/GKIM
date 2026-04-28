package com.gkim.im.android.feature.chat

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.data.remote.im.CompanionTurnRecordDto
import com.gkim.im.android.data.repository.DefaultCompanionTurnRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §3.4 / §6.1 verification — exercises the §3.1 chevron rendering + §2.2 selectVariantByGroup
 * mutation against the production `DefaultCompanionTurnRepository`'s variantGroups state
 * (rather than the §3.4 close-out's self-contained `BranchTreeHost`). The test renders
 * `ChatMessageRow` from the repository's `activePathByConversation` flow — the same flow
 * `ChatViewModel.uiState.companionMessages` consumes in production — and proves chevron
 * taps mutate the active variant index at every layer of the tree.
 *
 * Topology under test (companion-axis only — user-axis variant tracking is the
 * `tavern-experience-polish` §3.x slice's "honest spec gap" carried forward into this slice):
 * ```
 *   user-msg-1 → vg-c1 (2 companion siblings: c1-original, c1-regenerated)
 *   user-msg-2 → vg-c2 (2 companion siblings: c2-original, c2-regenerated)
 * ```
 * Two independent companion-axis variantGroups at different timeline depths compose into
 * 2 × 2 = 4 reachable end-to-end paths through the tree. Each chevron tap calls the
 * repository's `selectVariantByGroup` which mutates the variantGroup's `activeIndex`; the
 * §2.1 projection re-runs and emits a new active path; the recomposition shows the new
 * variant. The test exercises every chevron at every layer + the round-trip back to the
 * initial state to prove independence.
 */
@RunWith(AndroidJUnit4::class)
class ChatBranchNavigationInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chevronTapsMutateActivePathAtEveryLayerOfTheTree() {
        val repository = DefaultCompanionTurnRepository()
        seedTwoLayerTree(repository)

        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ProductionChatTimelineHost(repository = repository)
            }
        }
        composeRule.waitForIdle()

        // -- Branch 1: c1-original + c2-original (initial state on both axes) --
        composeRule.onNodeWithTag("chat-message-body-c1-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-variant-indicator-c1-original")
            .assertTextEquals("1/2")
        composeRule.onNodeWithTag("chat-companion-variant-indicator-c2-original")
            .assertTextEquals("1/2")

        // -- Branch 2: c1-original + c2-regenerated (advance second layer) --
        composeRule.onNodeWithTag("chat-companion-variant-next-c2-original").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat-message-body-c1-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-regenerated").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-variant-indicator-c2-regenerated")
            .assertTextEquals("2/2")

        // -- Branch 3: c1-regenerated + c2-original (rollback c2, advance c1) --
        composeRule.onNodeWithTag("chat-companion-variant-prev-c2-regenerated").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat-message-body-c2-original").assertIsDisplayed()

        composeRule.onNodeWithTag("chat-companion-variant-next-c1-original").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat-message-body-c1-regenerated").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-variant-indicator-c1-regenerated")
            .assertTextEquals("2/2")

        // -- Branch 4: c1-regenerated + c2-regenerated (advance c2 from current state) --
        composeRule.onNodeWithTag("chat-companion-variant-next-c2-original").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat-message-body-c1-regenerated").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-regenerated").assertIsDisplayed()

        // Round-trip back to Branch 1 — proves each branch is independently reachable both
        // forward and back, not only along a single traversal.
        composeRule.onNodeWithTag("chat-companion-variant-prev-c2-regenerated").performClick()
        composeRule.onNodeWithTag("chat-companion-variant-prev-c1-regenerated").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chat-message-body-c1-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-original").assertIsDisplayed()
    }

    private fun seedTwoLayerTree(repository: DefaultCompanionTurnRepository) {
        // First companion-turn variantGroup (vg-c1) under user-msg-1 with 2 siblings.
        repository.applyRecord(
            companionRecord(
                turnId = "c1-original",
                variantGroupId = "vg-c1",
                variantIndex = 0,
                parentMessageId = "user-msg-1",
                body = "first companion reply, original",
            ),
        )
        repository.applyRecord(
            companionRecord(
                turnId = "c1-regenerated",
                variantGroupId = "vg-c1",
                variantIndex = 1,
                parentMessageId = "user-msg-1",
                body = "first companion reply, regenerated",
            ),
        )
        // Roll back to original on vg-c1 so the test starts from a known initial state
        // (the repo's applyRecord defaults to "newly-applied sibling becomes active").
        repository.selectVariantByGroup(
            conversationId = CONVERSATION_ID,
            variantGroupId = "vg-c1",
            newIndex = 0,
        )
        // Second companion-turn variantGroup (vg-c2) under user-msg-2 with 2 siblings.
        repository.applyRecord(
            companionRecord(
                turnId = "c2-original",
                variantGroupId = "vg-c2",
                variantIndex = 0,
                parentMessageId = "user-msg-2",
                body = "second companion reply, original",
            ),
        )
        repository.applyRecord(
            companionRecord(
                turnId = "c2-regenerated",
                variantGroupId = "vg-c2",
                variantIndex = 1,
                parentMessageId = "user-msg-2",
                body = "second companion reply, regenerated",
            ),
        )
        // Same rollback for the second variantGroup so c2-original is the initial active.
        repository.selectVariantByGroup(
            conversationId = CONVERSATION_ID,
            variantGroupId = "vg-c2",
            newIndex = 0,
        )
    }

    private fun companionRecord(
        turnId: String,
        variantGroupId: String,
        variantIndex: Int,
        parentMessageId: String,
        body: String,
    ): CompanionTurnRecordDto = CompanionTurnRecordDto(
        turnId = turnId,
        conversationId = CONVERSATION_ID,
        messageId = turnId,
        variantGroupId = variantGroupId,
        variantIndex = variantIndex,
        parentMessageId = parentMessageId,
        status = "completed",
        accumulatedBody = body,
        lastDeltaSeq = 0,
        startedAt = "2026-04-26T12:00:00Z",
        completedAt = "2026-04-26T12:00:01Z",
    )

    private companion object {
        const val CONVERSATION_ID = "conversation-branch-nav-smoke"
    }
}

/**
 * Production-shaped Compose host: subscribes to the repository's `activePathByConversation`
 * flow (the same flow `ChatViewModel.uiState.companionMessages` consumes) and renders each
 * message through the production `ChatMessageRow`. Chevron taps route through
 * `repository.selectVariantByGroup` — the same mutation `ChatViewModel.selectVariantAt`
 * delegates to.
 */
@Composable
private fun ProductionChatTimelineHost(
    repository: DefaultCompanionTurnRepository,
) {
    val activePathByConversation by repository.activePathByConversation.collectAsStateWithLifecycle()
    val messages = activePathByConversation["conversation-branch-nav-smoke"].orEmpty()
    LazyColumn {
        items(messages, key = { it.id }) { message ->
            ChatMessageRow(
                conversation = null,
                message = message,
                isMostRecentCompanionVariant = true,
                onSelectVariantAt = { variantGroupId, newIndex ->
                    repository.selectVariantByGroup(
                        conversationId = "conversation-branch-nav-smoke",
                        variantGroupId = variantGroupId,
                        newIndex = newIndex,
                    )
                },
            )
        }
    }
}
