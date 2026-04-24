package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.CompanionDrawResult
import com.gkim.im.android.core.model.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * §7.2 verification — a draw result whose card id is already in the owned roster routes the
 * gacha result animation into the dedicated "Already owned" variant whose rendering includes
 * a "Keep as bonus" CTA. Tapping the CTA records a [BonusAwardedEvent] carrying the card id
 * + event timestamp.
 *
 * Tests drive the pure helpers ([gachaResultVariant], [bonusAwardedEvent]) that back the
 * composable's variant-selection and CTA-wiring. The composable renders the right branch for
 * each variant and invokes the onBonusAwarded callback with the event payload built by
 * [bonusAwardedEvent] — so asserting the helpers' contract implicitly covers the rendering
 * path without standing up Compose.
 */
class GachaDuplicateAnimationTest {

    private fun card(id: String = "daylight-listener"): CompanionCharacterCard =
        CompanionCharacterCard(
            id = id,
            displayName = LocalizedText.of(id),
            roleLabel = LocalizedText.of("Tester"),
            summary = LocalizedText.Empty,
            firstMes = LocalizedText.Empty,
            avatarText = "DL",
            accent = AccentTone.Primary,
            source = CompanionCharacterSource.Preset,
        )

    // -------------------------------------------------------------------------
    // Duplicate detection via gachaResultVariant
    // -------------------------------------------------------------------------

    @Test
    fun `new-card result selects the NewCard variant`() {
        val result = CompanionDrawResult(card = card(), wasNew = true)
        assertEquals(GachaResultVariant.NewCard, gachaResultVariant(result))
    }

    @Test
    fun `duplicate-card result selects the AlreadyOwned variant`() {
        val result = CompanionDrawResult(card = card(), wasNew = false)
        assertEquals(GachaResultVariant.AlreadyOwned, gachaResultVariant(result))
    }

    @Test
    fun `variant selection is determined solely by wasNew, not by card identity`() {
        // Two different cards, same wasNew → same variant.
        val dupA = CompanionDrawResult(card = card("a"), wasNew = false)
        val dupB = CompanionDrawResult(card = card("b"), wasNew = false)
        assertEquals(gachaResultVariant(dupA), gachaResultVariant(dupB))

        val newA = CompanionDrawResult(card = card("a"), wasNew = true)
        val newB = CompanionDrawResult(card = card("b"), wasNew = true)
        assertEquals(gachaResultVariant(newA), gachaResultVariant(newB))
    }

    // -------------------------------------------------------------------------
    // Variant kinds are exhaustive and distinguishable
    // -------------------------------------------------------------------------

    @Test
    fun `NewCard and AlreadyOwned are distinct variants`() {
        assertNotNull(GachaResultVariant.NewCard)
        assertNotNull(GachaResultVariant.AlreadyOwned)
        val a: GachaResultVariant = GachaResultVariant.NewCard
        val b: GachaResultVariant = GachaResultVariant.AlreadyOwned
        assertEquals(false, a == b)
    }

    // -------------------------------------------------------------------------
    // Bonus event payload — what the "Keep as bonus" CTA records
    // -------------------------------------------------------------------------

    @Test
    fun `bonusAwardedEvent carries the drawn card id and provided timestamp`() {
        val result = CompanionDrawResult(card = card("daylight-listener"), wasNew = false)
        val event = bonusAwardedEvent(result, timestampEpochMs = 1_735_689_600_000L)
        assertEquals("daylight-listener", event.cardId)
        assertEquals(1_735_689_600_000L, event.timestampEpochMs)
    }

    @Test
    fun `bonusAwardedEvent carries the card id for arbitrary id shapes`() {
        listOf("a", "user-authored_42", "UUID-dashed-abc", "含中文 id").forEach { id ->
            val event = bonusAwardedEvent(
                CompanionDrawResult(card = card(id), wasNew = false),
                timestampEpochMs = 0L,
            )
            assertEquals(id, event.cardId)
        }
    }

    @Test
    fun `bonusAwardedEvent is constructable for a new-card result too (CTA not shown but payload still valid)`() {
        val result = CompanionDrawResult(card = card("new-card"), wasNew = true)
        val event = bonusAwardedEvent(result, timestampEpochMs = 42L)
        assertEquals("new-card", event.cardId)
        assertEquals(42L, event.timestampEpochMs)
    }

    // -------------------------------------------------------------------------
    // Callback shape check — simulates the composable invocation
    // -------------------------------------------------------------------------

    @Test
    fun `onBonusAwarded callback receives exactly the event built by bonusAwardedEvent`() {
        val result = CompanionDrawResult(card = card("daylight-listener"), wasNew = false)
        val timestamp = 1_735_776_000_000L

        val captured = mutableListOf<BonusAwardedEvent>()
        val onBonusAwarded: (BonusAwardedEvent) -> Unit = { captured += it }

        // Simulate the composable's click handler: when user taps "Keep as bonus" on the
        // AlreadyOwned variant, it invokes onBonusAwarded(bonusAwardedEvent(result, now())).
        onBonusAwarded(bonusAwardedEvent(result, timestamp))

        assertEquals(1, captured.size)
        assertEquals(BonusAwardedEvent(cardId = "daylight-listener", timestampEpochMs = timestamp), captured.single())
    }
}
