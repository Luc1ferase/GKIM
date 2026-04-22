package com.gkim.im.android.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonaIntegrationChatTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pillAndFooterRenderActivePersonaDisplayNameOnEntry() {
        composeRule.setContent {
            TestableChatChrome(
                personas = samplePersonas(),
                initialActiveId = "persona-nova",
                language = AppLanguage.English,
            )
        }

        composeRule.onNodeWithTag("chat-persona-pill-label").assertTextEquals("Nova")
        composeRule.onNodeWithTag("chat-persona-footer-text").assertTextEquals("Talking as Nova")
    }

    @Test
    fun switchingActivePersonaMidSessionUpdatesPillAndFooter() {
        composeRule.setContent {
            TestableChatChrome(
                personas = samplePersonas(),
                initialActiveId = "persona-nova",
                language = AppLanguage.English,
            )
        }

        composeRule.onNodeWithTag("chat-persona-pill-label").assertTextEquals("Nova")
        composeRule.onNodeWithTag("chat-persona-footer-text").assertTextEquals("Talking as Nova")

        composeRule.onNodeWithTag("chat-switch-active-persona-auric").performClick()

        composeRule.onNodeWithTag("chat-persona-pill-label").assertTextEquals("Auric")
        composeRule.onNodeWithTag("chat-persona-footer-text").assertTextEquals("Talking as Auric")
    }

    @Test
    fun nextTurnGreetingPreviewReflectsNewPersonaDisplayName() {
        composeRule.setContent {
            TestableChatChrome(
                personas = samplePersonas(),
                initialActiveId = "persona-nova",
                language = AppLanguage.English,
                greetingTemplate = "Welcome {{user}}, {{char}} is listening.",
                companionName = "Eris",
            )
        }

        composeRule.onNodeWithTag("chat-greeting-preview")
            .assertTextEquals("Welcome Nova, Eris is listening.")

        composeRule.onNodeWithTag("chat-switch-active-persona-auric").performClick()

        composeRule.onNodeWithTag("chat-greeting-preview")
            .assertTextEquals("Welcome Auric, Eris is listening.")
    }

    @Test
    fun chinesePillAndFooterRenderChineseDisplayName() {
        composeRule.setContent {
            TestableChatChrome(
                personas = samplePersonas(),
                initialActiveId = "persona-nova",
                language = AppLanguage.Chinese,
            )
        }

        composeRule.onNodeWithTag("chat-persona-pill-label").assertTextEquals("新星")
        composeRule.onNodeWithTag("chat-persona-footer-text").assertTextEquals("以 新星 的身份对话")
    }

    @Test
    fun switchingLanguageKeepsSamePersonaActiveButSwitchesDisplay() {
        composeRule.setContent {
            TestableChatChrome(
                personas = samplePersonas(),
                initialActiveId = "persona-auric",
                language = AppLanguage.English,
            )
        }

        composeRule.onNodeWithTag("chat-persona-pill-label").assertTextEquals("Auric")
        composeRule.onNodeWithTag("chat-toggle-language-zh").performClick()
        composeRule.onNodeWithTag("chat-persona-pill-label").assertTextEquals("金辉")
        composeRule.onNodeWithTag("chat-persona-footer-text").assertTextEquals("以 金辉 的身份对话")
    }

    private fun samplePersonas(): List<UserPersona> = listOf(
        UserPersona(
            id = "persona-nova",
            displayName = LocalizedText("Nova", "新星"),
            description = LocalizedText("Thoughtful traveller.", "深思熟虑的旅人。"),
            isBuiltIn = true,
            isActive = true,
        ),
        UserPersona(
            id = "persona-auric",
            displayName = LocalizedText("Auric", "金辉"),
            description = LocalizedText("Archivist.", "档案员。"),
            isBuiltIn = false,
            isActive = false,
        ),
    )
}

@Composable
private fun TestableChatChrome(
    personas: List<UserPersona>,
    initialActiveId: String,
    language: AppLanguage,
    greetingTemplate: String = "",
    companionName: String = "",
) {
    var activeId by remember { mutableStateOf(initialActiveId) }
    var currentLanguage by remember { mutableStateOf(language) }

    val activePersona = personas.first { it.id == activeId }
    val pill = chatChromePersonaPill(activePersona = activePersona, language = currentLanguage)
    val footer = chatChromePersonaFooter(activePersona = activePersona, language = currentLanguage)
    val resolvedGreeting = if (greetingTemplate.isNotBlank()) {
        com.gkim.im.android.core.model.MacroSubstitution.substituteMacros(
            template = greetingTemplate,
            userDisplayName = activePersona.displayName.resolve(currentLanguage),
            charDisplayName = companionName,
        )
    } else {
        ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("chat-chrome-root"),
    ) {
        Text(
            text = pill.label,
            modifier = Modifier.testTag("chat-persona-pill-label"),
        )
        footer?.let { line ->
            Text(
                text = line.text,
                modifier = Modifier.testTag("chat-persona-footer-text"),
            )
        }
        if (greetingTemplate.isNotBlank()) {
            Text(
                text = resolvedGreeting,
                modifier = Modifier.testTag("chat-greeting-preview"),
            )
        }
        Row {
            personas.forEach { persona ->
                Text(
                    text = "Switch ${persona.id}",
                    modifier = Modifier
                        .testTag("chat-switch-active-${persona.id}")
                        .clickable { activeId = persona.id },
                )
            }
            Text(
                text = "EN",
                modifier = Modifier
                    .testTag("chat-toggle-language-en")
                    .clickable { currentLanguage = AppLanguage.English },
            )
            Text(
                text = "ZH",
                modifier = Modifier
                    .testTag("chat-toggle-language-zh")
                    .clickable { currentLanguage = AppLanguage.Chinese },
            )
        }
    }
}
