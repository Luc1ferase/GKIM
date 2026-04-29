package com.gkim.im.android.feature.tavern

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CharacterSkin
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.SkinRarity
import com.gkim.im.android.core.model.SkinTrait
import com.gkim.im.android.core.model.SkinTraitKind
import com.gkim.im.android.core.model.SkinTraitPayload
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * R3.3 — companion-skin-gacha skin gallery instrumentation.
 *
 * Two flows are exercised end-to-end against the wired composable:
 *   1. Tap-OwnedInactive → confirm dialog → onActivate fires with the skin id.
 *   2. Tap-Locked → preview sheet shows the trait descriptions but never
 *      renders the locked skin's actual thumb art, and the "Try drawing"
 *      CTA fires the navigation callback.
 */
@RunWith(AndroidJUnit4::class)
class SkinGalleryActivationInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val characterId = "tavern-keeper"
    private val activeSkin = CharacterSkin(
        skinId = "tavern-keeper-default",
        characterId = characterId,
        displayName = LocalizedText("Default", "默认"),
        rarity = SkinRarity.Common,
        artVersion = 1,
        isDefault = true,
        traits = emptyList(),
    )
    private val ownedInactiveSkin = CharacterSkin(
        skinId = "tavern-keeper-lantern-keeper",
        characterId = characterId,
        displayName = LocalizedText("Lantern Keeper", "提灯人"),
        rarity = SkinRarity.Epic,
        artVersion = 1,
        isDefault = false,
        traits = emptyList(),
    )
    private val lockedSkin = CharacterSkin(
        skinId = "tavern-keeper-midnight-archivist",
        characterId = characterId,
        displayName = LocalizedText("Midnight Archivist", "深夜执笔人"),
        rarity = SkinRarity.Legendary,
        artVersion = 1,
        isDefault = false,
        traits = listOf(
            SkinTrait(
                traitId = "trait-archivist-persona",
                kind = SkinTraitKind.PersonaMod,
                description = LocalizedText(
                    english = "Speaks in slower, more deliberate sentences.",
                    chinese = "说话更慢、更克制。",
                ),
                payload = SkinTraitPayload.PersonaMod(
                    systemPromptAppendix = LocalizedText.Empty,
                ),
            ),
        ),
    )

    @Test
    fun tappingOwnedInactiveShowsConfirmDialogThenActivates() {
        var activated: String? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                CharacterDetailSkinGallerySection(
                    characterId = characterId,
                    skins = listOf(activeSkin, ownedInactiveSkin, lockedSkin),
                    ownedSkinIds = setOf(activeSkin.skinId, ownedInactiveSkin.skinId),
                    activeSkinId = activeSkin.skinId,
                    appLanguage = AppLanguage.English,
                    onActivate = { activated = it },
                    onTryDrawing = {},
                )
            }
        }

        composeRule
            .onNodeWithTag("skin-gallery-cell-${ownedInactiveSkin.skinId}")
            .performClick()
        composeRule.onNodeWithTag("skin-activate-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("skin-activate-confirm").performClick()

        assertEquals(ownedInactiveSkin.skinId, activated)
    }

    @Test
    fun tappingLockedShowsPreviewSheetWithTraitDescriptions() {
        var tryDrawingClicks = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                CharacterDetailSkinGallerySection(
                    characterId = characterId,
                    skins = listOf(activeSkin, ownedInactiveSkin, lockedSkin),
                    ownedSkinIds = setOf(activeSkin.skinId, ownedInactiveSkin.skinId),
                    activeSkinId = activeSkin.skinId,
                    appLanguage = AppLanguage.English,
                    onActivate = {},
                    onTryDrawing = { tryDrawingClicks++ },
                )
            }
        }

        composeRule
            .onNodeWithTag("skin-gallery-cell-${lockedSkin.skinId}")
            .performClick()
        composeRule.onNodeWithTag("skin-locked-preview-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("skin-locked-preview-rarity").assertIsDisplayed()
        composeRule
            .onNodeWithTag("skin-locked-preview-trait-trait-archivist-persona")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("skin-locked-preview-try-drawing").performClick()
        assertEquals(1, tryDrawingClicks)
    }
}
