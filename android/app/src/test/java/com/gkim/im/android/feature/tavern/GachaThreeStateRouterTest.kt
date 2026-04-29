package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.CompanionDrawResult
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.SkinDrawResultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R4.3 — companion-skin-gacha three-state router contract.
 *
 * The `/api/v1/skins/draw` endpoint returns one of NEW_CHARACTER / NEW_SKIN /
 * DUPLICATE_SKIN per result entry; the post-draw surface routes off that value
 * to render the matching variant. This test pins:
 *   - every state maps to a distinct [GachaResultVariant]
 *   - the three labels live in [GachaResultAccents] alongside the bonus CTA
 *   - pill discipline holds: every accent binding still uses primary/tertiary
 */
class GachaThreeStateRouterTest {

    @Test
    fun `NEW_CHARACTER routes to the NewCard surface`() {
        assertEquals(
            GachaResultVariant.NewCard,
            gachaResultVariantForState(SkinDrawResultState.NewCharacter),
        )
    }

    @Test
    fun `NEW_SKIN routes to the NewSkin surface`() {
        assertEquals(
            GachaResultVariant.NewSkin,
            gachaResultVariantForState(SkinDrawResultState.NewSkin),
        )
    }

    @Test
    fun `DUPLICATE_SKIN routes to the AlreadyOwned surface`() {
        assertEquals(
            GachaResultVariant.AlreadyOwned,
            gachaResultVariantForState(SkinDrawResultState.DuplicateSkin),
        )
    }

    @Test
    fun `every state maps to a distinct variant`() {
        val variants = SkinDrawResultState.values().map { gachaResultVariantForState(it) }.toSet()
        assertEquals(
            "Each state must route to a distinct surface variant",
            SkinDrawResultState.values().size,
            variants.size,
        )
    }

    @Test
    fun `result with drawResultState routes via the state path`() {
        // Anti-regression: when the backend ships drawResultState, the router
        // MUST use it — even if `wasNew` would have produced a different
        // variant. Concrete case: NEW_SKIN ships wasNew=false (character was
        // already owned) but the variant must still be NewSkin, not AlreadyOwned.
        val result = CompanionDrawResult(
            card = sampleCard(),
            wasNew = false,
            drawResultState = SkinDrawResultState.NewSkin,
        )
        assertEquals(GachaResultVariant.NewSkin, gachaResultVariant(result))
    }

    @Test
    fun `result without drawResultState falls back to wasNew heuristic`() {
        // Backwards compatibility: legacy /draw + in-memory roster repo emit
        // results without drawResultState. The router must still split
        // wasNew → NewCard, !wasNew → AlreadyOwned for those callers.
        val newCard = CompanionDrawResult(card = sampleCard(), wasNew = true)
        assertEquals(GachaResultVariant.NewCard, gachaResultVariant(newCard))

        val duplicate = CompanionDrawResult(card = sampleCard(), wasNew = false)
        assertEquals(GachaResultVariant.AlreadyOwned, gachaResultVariant(duplicate))
    }

    @Test
    fun `accents manifest still satisfies pill-discipline constraint`() {
        // R4.3 must not regress R1.1's pill-discipline gate: every accent
        // binding (including the new NEW_SKIN entry) sticks to the
        // primary / tertiary token pair.
        val tokens = GachaResultAccents.map { it.paletteToken }.toSet()
        assertTrue(
            "Gacha accents must only reference primary or tertiary tokens, got $tokens",
            tokens.all { it == "primary" || it == "tertiary" },
        )
    }

    @Test
    fun `accents manifest pins NEW_SKIN label to tertiary`() {
        val newSkin = GachaResultAccents.firstOrNull { it.testTag == "tavern-draw-result-new-skin-label" }
        assertEquals("tertiary", newSkin?.paletteToken)
    }

    private fun sampleCard(id: String = "tavern-keeper"): CompanionCharacterCard =
        CompanionCharacterCard(
            id = id,
            displayName = LocalizedText("Keeper", "酒保"),
            roleLabel = LocalizedText("Quiet Host", "静默主人"),
            summary = LocalizedText("", ""),
            firstMes = LocalizedText("", ""),
            avatarText = "TK",
            accent = AccentTone.Primary,
            source = CompanionCharacterSource.Preset,
        )
}
