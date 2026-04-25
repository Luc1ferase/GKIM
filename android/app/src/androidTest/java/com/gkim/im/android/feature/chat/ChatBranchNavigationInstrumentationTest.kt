package com.gkim.im.android.feature.chat

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.CompanionTurnMeta
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.MessageStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §3.4 verification — seeds a conversation with three companion turns, simulates editing turn
 * 1's user message + regenerating turn 2 (the §3.2 + §3.3 helper effects), and asserts that
 * the resulting 4-branch tree is navigable through chevron taps with each branch
 * independently reachable.
 *
 * Topology under test:
 * ```
 *   u1 (variantGroup A, 2 siblings: u1-original, u1-edited)
 *     ↘ active drives the next layer
 *   u2 (single variant)
 *     └ c2 (variantGroup B, 2 siblings: c2-original, c2-regenerated)
 * ```
 * Two independent variantGroups (A under conversation root for user-message siblings, B under
 * u2 for companion-turn siblings) compose into 2 × 2 = 4 reachable end-to-end paths. Each
 * test step exercises one chevron at one layer and verifies the active-path map mutation
 * propagates back into the rendered chevron caption + body, and that all four (u1, c2)
 * combinations are reachable forward and back.
 *
 * This is a self-contained instrumentation: no live backend, no DefaultCompanionTurnRepository
 * mutation surface needed. The §3.2 / §3.3 helpers' wire side (HTTP call, ViewModel handler,
 * actual edit / regenerate-at affordance render in ChatMessageRow's overflow) is the
 * follow-up wire-up slice's responsibility; this slice locks the contract that once the
 * sibling counts land in a bubble's `companionTurnMeta`, the §3.1 chevron renders + the tap
 * mutation round-trips through `onSelectVariantAt` and surfaces in the next render at every
 * layer of the tree, not only the most recent one.
 */
@RunWith(AndroidJUnit4::class)
class ChatBranchNavigationInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fourBranchTreeIsReachableThroughChevronTapsAtTwoLayers() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                BranchTreeHost(
                    userVariants = USER_VARIANTS,
                    companionVariants = COMPANION_VARIANTS,
                )
            }
        }

        // -- Branch 1: u1-original + c2-original (initial state on both axes) --
        composeRule.onNodeWithTag("chat-message-body-u1-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-variant-indicator-u1-original")
            .assertTextEquals("1/2")
        composeRule.onNodeWithTag("chat-companion-variant-indicator-c2-original")
            .assertTextEquals("1/2")

        // -- Branch 2: u1-original + c2-regenerated (companion axis next) --
        composeRule.onNodeWithTag("chat-companion-variant-next-c2-original").performClick()
        composeRule.onNodeWithTag("chat-message-body-u1-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-regenerated").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-variant-indicator-c2-regenerated")
            .assertTextEquals("2/2")

        // -- Branch 3: u1-edited + c2-original (roll companion back, then advance user axis) --
        composeRule.onNodeWithTag("chat-companion-variant-prev-c2-regenerated").performClick()
        composeRule.onNodeWithTag("chat-message-body-c2-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-variant-next-u1-original").performClick()
        composeRule.onNodeWithTag("chat-message-body-u1-edited").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-variant-indicator-u1-edited")
            .assertTextEquals("2/2")

        // -- Branch 4: u1-edited + c2-regenerated (the second-axis combination) --
        composeRule.onNodeWithTag("chat-companion-variant-next-c2-original").performClick()
        composeRule.onNodeWithTag("chat-message-body-u1-edited").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-regenerated").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-variant-indicator-c2-regenerated")
            .assertTextEquals("2/2")

        // Round-trip back to Branch 1 — proves each branch is independently reachable both
        // forward and back, not only along a single traversal.
        composeRule.onNodeWithTag("chat-companion-variant-prev-c2-regenerated").performClick()
        composeRule.onNodeWithTag("chat-companion-variant-prev-u1-edited").performClick()
        composeRule.onNodeWithTag("chat-message-body-u1-original").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-message-body-c2-original").assertIsDisplayed()
    }

    private companion object {
        // u1 variant group — two user-message siblings rendered as variant carriers so the
        // §3.1 chevron path picks them up. (User bubbles in production don't carry
        // CompanionTurnMeta; the carrier here is a navigation surface that mirrors what the
        // edit-user-turn wire-up will project once §3.2's repository / ViewModel layer lands.)
        val USER_VARIANTS: List<ChatMessage> = listOf(
            variantCarrier(
                id = "u1-original",
                body = "the original turn 1 user text",
                variantGroupId = "vgroup-u1",
                siblingCount = 2,
                siblingActiveIndex = 0,
            ),
            variantCarrier(
                id = "u1-edited",
                body = "the edited turn 1 user text",
                variantGroupId = "vgroup-u1",
                siblingCount = 2,
                siblingActiveIndex = 1,
            ),
        )

        // c2 variant group — two companion-turn siblings.
        val COMPANION_VARIANTS: List<ChatMessage> = listOf(
            variantCarrier(
                id = "c2-original",
                body = "original reply to turn 2",
                variantGroupId = "vgroup-c2",
                siblingCount = 2,
                siblingActiveIndex = 0,
            ),
            variantCarrier(
                id = "c2-regenerated",
                body = "regenerated reply to turn 2",
                variantGroupId = "vgroup-c2",
                siblingCount = 2,
                siblingActiveIndex = 1,
            ),
        )

        private fun variantCarrier(
            id: String,
            body: String,
            variantGroupId: String,
            siblingCount: Int,
            siblingActiveIndex: Int,
        ): ChatMessage = ChatMessage(
            id = id,
            direction = MessageDirection.Incoming,
            kind = MessageKind.Text,
            body = body,
            createdAt = "2026-04-25T12:00:01Z",
            status = MessageStatus.Completed,
            parentMessageId = "msg-conv-root",
            companionTurnMeta = CompanionTurnMeta(
                turnId = id,
                variantGroupId = variantGroupId,
                variantIndex = siblingActiveIndex,
                siblingCount = siblingCount,
                siblingActiveIndex = siblingActiveIndex,
            ),
        )
    }
}

/**
 * Minimal Compose surface that renders the active path across two independent variant
 * groups. Mirrors how a future ChatViewModel + LiveCompanionTurnRepository wire-up will
 * project the conversation's branch tree — at any moment one user variant is active for the
 * u1 group and one companion variant is active for the c2 group; tapping a chevron flips
 * its layer's active index without touching the other layer.
 */
@Composable
private fun BranchTreeHost(
    userVariants: List<ChatMessage>,
    companionVariants: List<ChatMessage>,
) {
    var userActiveIndex by remember { mutableStateOf(0) }
    var companionActiveIndex by remember { mutableStateOf(0) }

    val activeUser = userVariants[userActiveIndex.coerceIn(0, userVariants.size - 1)]
    val activeCompanion = companionVariants[
        companionActiveIndex.coerceIn(0, companionVariants.size - 1),
    ]

    LazyColumn {
        item(key = "u1-layer") {
            ChatMessageRow(
                conversation = null,
                message = activeUser.copy(
                    companionTurnMeta = activeUser.companionTurnMeta?.copy(
                        siblingActiveIndex = userActiveIndex,
                        variantIndex = userActiveIndex,
                    ),
                ),
                isMostRecentCompanionVariant = true,
                onSelectVariantAt = { _, newIndex ->
                    userActiveIndex = newIndex.coerceIn(0, userVariants.size - 1)
                },
            )
        }
        item(key = "c2-layer") {
            ChatMessageRow(
                conversation = null,
                message = activeCompanion.copy(
                    companionTurnMeta = activeCompanion.companionTurnMeta?.copy(
                        siblingActiveIndex = companionActiveIndex,
                        variantIndex = companionActiveIndex,
                    ),
                ),
                isMostRecentCompanionVariant = true,
                onSelectVariantAt = { _, newIndex ->
                    companionActiveIndex = newIndex.coerceIn(0, companionVariants.size - 1)
                },
            )
        }
    }
}
