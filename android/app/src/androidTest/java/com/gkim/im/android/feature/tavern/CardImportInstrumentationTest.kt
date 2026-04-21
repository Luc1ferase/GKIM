package com.gkim.im.android.feature.tavern

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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.interop.SillyTavernCardCodec
import com.gkim.im.android.core.interop.SillyTavernCardFormat
import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.repository.CardImportPreview
import com.gkim.im.android.data.repository.CardImportWarning
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardImportInstrumentationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    @Test
    fun evaluateImportSelectionAcceptsValidPngAndJson() {
        val pngBytes = pngSignature + ByteArray(16)
        val accepted = evaluateImportSelection(pngBytes, "aria.png")
        assertTrue(accepted is TavernImportSelectionResult.Accepted)
        assertEquals(
            SillyTavernCardFormat.Png,
            (accepted as TavernImportSelectionResult.Accepted).format,
        )

        val jsonAccepted = evaluateImportSelection(
            "{\"name\":\"Aria\"}".toByteArray(Charsets.UTF_8),
            "aria.json",
        )
        assertTrue(jsonAccepted is TavernImportSelectionResult.Accepted)
        assertEquals(
            SillyTavernCardFormat.Json,
            (jsonAccepted as TavernImportSelectionResult.Accepted).format,
        )
    }

    @Test
    fun evaluateImportSelectionRejectsOversizeAndMalformedPayloads() {
        val huge = pngSignature + ByteArray(SillyTavernCardCodec.MaxPngBytes)
        assertEquals(
            TavernImportSelectionResult.Rejected("payload_too_large"),
            evaluateImportSelection(huge, "huge.png"),
        )
        assertEquals(
            TavernImportSelectionResult.Rejected("unsupported_format"),
            evaluateImportSelection("not a card".toByteArray(), "mystery.bin"),
        )
        assertEquals(
            TavernImportSelectionResult.Rejected("empty_payload"),
            evaluateImportSelection(ByteArray(0), "empty.png"),
        )
    }

    @Test
    fun importCardPreviewLoadedStateRendersCardLanguageAndWarnings() {
        val loaded = ImportCardPreviewUiState.Loaded(
            preview = samplePreview(),
            selectedLanguage = "en",
        )

        composeRule.setContent {
            TestablePreviewScreen(initialState = loaded)
        }

        composeRule.onNodeWithTag("tavern-import-preview").assertIsDisplayed()
        composeRule.onNodeWithTag("tavern-import-preview-card").assertIsDisplayed()
        composeRule.onNodeWithTag("tavern-import-preview-language-en").assertIsDisplayed()
        composeRule.onNodeWithTag("tavern-import-preview-language-zh").assertIsDisplayed()
        composeRule.onNodeWithTag("tavern-import-preview-commit").assertIsDisplayed()
        composeRule.onNodeWithTag(
            "tavern-import-preview-warning-post_history_instruction_parked",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(
            "tavern-import-preview-warning-field_truncated",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("tavern-import-preview-st-extensions").assertIsDisplayed()
    }

    @Test
    fun importCardPreviewFailedStateRendersInlineError() {
        val failed = ImportCardPreviewUiState.Failed("payload_too_large")

        composeRule.setContent {
            TestablePreviewScreen(initialState = failed)
        }

        composeRule.onNodeWithTag("tavern-import-preview-failed").assertIsDisplayed()
    }

    @Test
    fun languageTogglesFlipSelectedLanguageInRenderedPreview() {
        composeRule.setContent {
            TestableLanguageRow()
        }

        composeRule.onNodeWithTag("tavern-import-preview-language-zh").performClick()
        composeRule.onNodeWithTag("lang-display-zh").assertIsDisplayed()
        composeRule.onNodeWithTag("tavern-import-preview-language-en").performClick()
        composeRule.onNodeWithTag("lang-display-en").assertIsDisplayed()
    }

    private fun samplePreview(): CardImportPreview = CardImportPreview(
        previewToken = "preview-x",
        card = CompanionCharacterCard(
            id = "card-import-1",
            displayName = LocalizedText("Aria", "Aria"),
            roleLabel = LocalizedText("Guide", "向导"),
            summary = LocalizedText("Calm guide", "Calm guide"),
            firstMes = LocalizedText("Hello traveller.", "你好，旅人。"),
            alternateGreetings = emptyList(),
            systemPrompt = LocalizedText("", ""),
            personality = LocalizedText("", ""),
            scenario = LocalizedText("", ""),
            exampleDialogue = LocalizedText("", ""),
            tags = emptyList(),
            creator = "",
            creatorNotes = "",
            characterVersion = "",
            avatarText = "AR",
            avatarUri = null,
            accent = AccentTone.Primary,
            source = CompanionCharacterSource.UserAuthored,
            extensions = JsonObject(emptyMap()),
        ),
        rawCardDto = CompanionCharacterCardDto(
            id = "card-import-1",
            displayName = LocalizedTextDto("Aria", "Aria"),
            roleLabel = LocalizedTextDto("Guide", "向导"),
            summary = LocalizedTextDto("Calm guide", "Calm guide"),
            firstMes = LocalizedTextDto("Hello traveller.", "你好，旅人。"),
            avatarText = "AR",
            accent = "primary",
            source = "userauthored",
        ),
        detectedLanguage = "en",
        warnings = listOf(
            CardImportWarning("post_history_instruction_parked", "stPostHistoryInstructions"),
            CardImportWarning("field_truncated", "personality", "> 32 KiB"),
        ),
        stExtensionKeys = listOf("stPostHistoryInstructions"),
    )
}

@Composable
private fun TestablePreviewScreen(initialState: ImportCardPreviewUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("tavern-import-preview"),
    ) {
        when (val state = initialState) {
            is ImportCardPreviewUiState.Loaded -> {
                Column(modifier = Modifier.testTag("tavern-import-preview-card")) {
                    Text(state.preview.card.displayName.english)
                    Text(state.preview.card.firstMes.english)
                }
                Row {
                    Text(
                        text = "EN",
                        modifier = Modifier.testTag("tavern-import-preview-language-en"),
                    )
                    Text(
                        text = "ZH",
                        modifier = Modifier.testTag("tavern-import-preview-language-zh"),
                    )
                }
                Text(
                    text = "Commit",
                    modifier = Modifier.testTag("tavern-import-preview-commit"),
                )
                state.preview.warnings.forEach { warning ->
                    Text(
                        text = warning.code,
                        modifier = Modifier.testTag("tavern-import-preview-warning-${warning.code}"),
                    )
                }
                if (state.preview.stExtensionKeys.isNotEmpty()) {
                    Text(
                        text = state.preview.stExtensionKeys.joinToString(","),
                        modifier = Modifier.testTag("tavern-import-preview-st-extensions"),
                    )
                }
            }
            is ImportCardPreviewUiState.Failed -> {
                Text(
                    text = importErrorCopy(state.code, englishLocale = true),
                    modifier = Modifier.testTag("tavern-import-preview-failed"),
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun TestableLanguageRow() {
    var selected by remember { mutableStateOf("en") }
    Column {
        Row {
            Text(
                text = "EN",
                modifier = Modifier
                    .testTag("tavern-import-preview-language-en")
                    .clickable { selected = "en" },
            )
            Text(
                text = "ZH",
                modifier = Modifier
                    .testTag("tavern-import-preview-language-zh")
                    .clickable { selected = "zh" },
            )
        }
        Text(
            text = selected,
            modifier = Modifier.testTag("lang-display-$selected"),
        )
    }
}
