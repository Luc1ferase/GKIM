package com.gkim.im.android.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LlmCompanionChatTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun companionGreetingPickerRendersFirstMesAndAlternatesOnEmptyPath() {
        val card = demoCard(
            firstMes = "Hello traveller.",
            alternates = listOf("Welcome back.", "A new day begins."),
        )
        val options = resolveCompanionGreetings(card, AppLanguage.English)

        var picked: CompanionGreetingOption? = null
        composeRule.setContent {
            CompanionGreetingPicker(
                options = options,
                onSelect = { picked = it },
            )
        }

        composeRule.onNodeWithTag("chat-companion-greeting-picker").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-greeting-option-0").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-greeting-option-1").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-companion-greeting-option-2").assertIsDisplayed()

        composeRule.onNodeWithTag("chat-companion-greeting-option-1").performClick()
        assertEquals(1, picked?.index)
        assertEquals("Welcome back.", picked?.body)
    }

    @Test
    fun companionGreetingPickerDoesNotRenderWhenPathIsPopulated() {
        val card = demoCard(firstMes = "Hi.", alternates = emptyList())
        val options = resolveCompanionGreetings(card, AppLanguage.English)
        val shouldShow = shouldShowGreetingPicker(
            companionPathIsEmpty = false,
            options = options,
        )
        assertEquals(false, shouldShow)
    }

    @Test
    fun variantNavigationStateDrivesIndicatorAndBoundaryFlags() {
        val single = variantNavigationState(variantGroupSiblingCount = 1, activeIndex = 0)
        val middle = variantNavigationState(variantGroupSiblingCount = 3, activeIndex = 1)!!
        val terminal = variantNavigationState(variantGroupSiblingCount = 3, activeIndex = 2)!!
        assertEquals(null, single)
        assertEquals("2/3", middle.indicator)
        assertEquals(true, middle.hasPrevious)
        assertEquals(true, middle.hasNext)
        assertEquals("3/3", terminal.indicator)
        assertEquals(false, terminal.hasNext)
    }

    @Test
    fun outgoingSubmissionFailureLineDrivesUserBubbleCopy() {
        val failed = com.gkim.im.android.core.model.ChatMessage(
            id = "user-1",
            direction = com.gkim.im.android.core.model.MessageDirection.Outgoing,
            kind = com.gkim.im.android.core.model.MessageKind.Text,
            body = "Hi",
            createdAt = "2026-04-21T08:00:00Z",
            status = com.gkim.im.android.core.model.MessageStatus.Failed,
        )
        assertEquals("Failed to send", outgoingSubmissionFailureLine(failed))

        val timeout = failed.copy(status = com.gkim.im.android.core.model.MessageStatus.Timeout)
        assertEquals("Timed out — tap retry", outgoingSubmissionFailureLine(timeout))

        val completed = failed.copy(status = com.gkim.im.android.core.model.MessageStatus.Completed)
        assertEquals(null, outgoingSubmissionFailureLine(completed))
    }

    private fun demoCard(
        firstMes: String,
        alternates: List<String>,
    ): CompanionCharacterCard = CompanionCharacterCard(
        id = "card-1",
        displayName = LocalizedText("Aria", "Aria"),
        roleLabel = LocalizedText("Guide", "向导"),
        summary = LocalizedText("Guide", "向导"),
        firstMes = LocalizedText(firstMes, firstMes),
        alternateGreetings = alternates.map { LocalizedText(it, it) },
        avatarText = "AR",
        accent = AccentTone.Primary,
        source = CompanionCharacterSource.Preset,
    )
}
