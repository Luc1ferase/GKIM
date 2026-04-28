package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * §8.1 verification — the "About this card" sub-section extracts creator attribution and ST
 * metadata from [CompanionCharacterCard] with the contract:
 *
 * - Blank top-level fields (`creator`, `creatorNotes`, `characterVersion`) surface as null.
 * - ST extension fields (`extensions.st.stSource` / `stCreationDate` / `stModificationDate`)
 *   surface as strings; blank / missing / non-primitive shapes all degrade to null.
 * - Dates accept epoch seconds, epoch milliseconds, or pre-formatted strings — numeric inputs
 *   are normalized to ISO yyyy-MM-dd in the system timezone; unrecognizable shapes pass through.
 *
 * These assertions drive [aboutCardData] (the pure function backing the composable) so the
 * contract can be exercised without standing up Compose.
 */
class CharacterDetailCreatorSubSectionTest {

    private fun cardWith(
        creator: String = "",
        creatorNotes: String = "",
        characterVersion: String = "",
        extensions: JsonObject = JsonObject(emptyMap()),
    ): CompanionCharacterCard = CompanionCharacterCard(
        id = "card-1",
        displayName = LocalizedText.of("Test"),
        roleLabel = LocalizedText.of("Tester"),
        summary = LocalizedText.Empty,
        firstMes = LocalizedText.Empty,
        creator = creator,
        creatorNotes = creatorNotes,
        characterVersion = characterVersion,
        avatarText = "T",
        accent = AccentTone.Primary,
        source = CompanionCharacterSource.Preset,
        extensions = extensions,
    )

    @Test
    fun `all six fields populated surface the full About panel`() {
        val card = cardWith(
            creator = "Author Example",
            creatorNotes = "Inspired by a dream",
            characterVersion = "1.2.3",
            extensions = buildJsonObject {
                put("st", buildJsonObject {
                    put("stSource", JsonPrimitive("https://example.org/card"))
                    // 2026-04-25 00:00 UTC ≈ epoch seconds 1777680000
                    put("stCreationDate", JsonPrimitive(1777680000))
                    put("stModificationDate", JsonPrimitive(1777766400))
                })
            },
        )

        val data = aboutCardData(card)

        assertEquals("Author Example", data.creator)
        assertEquals("Inspired by a dream", data.creatorNotes)
        assertEquals("1.2.3", data.characterVersion)
        assertEquals("https://example.org/card", data.stSource)
        assertNotNull(data.stCreationDate)
        assertNotNull(data.stModificationDate)
        assertTrue("data.isEmpty should be false when fields present", !data.isEmpty)
    }

    @Test
    fun `all fields blank or missing surface an empty AboutCardData with isEmpty true`() {
        val data = aboutCardData(cardWith())

        assertNull(data.creator)
        assertNull(data.creatorNotes)
        assertNull(data.characterVersion)
        assertNull(data.stSource)
        assertNull(data.stCreationDate)
        assertNull(data.stModificationDate)
        assertTrue("isEmpty must be true for a blank card", data.isEmpty)
    }

    @Test
    fun `partially populated card hides missing rows`() {
        val card = cardWith(
            creator = "Author Example",
            creatorNotes = "   ",  // whitespace counts as blank
            extensions = buildJsonObject {
                put("st", buildJsonObject {
                    put("stSource", JsonPrimitive(""))  // empty primitive → hidden
                })
            },
        )

        val data = aboutCardData(card)

        assertEquals("Author Example", data.creator)
        assertNull("blank creatorNotes must collapse to null", data.creatorNotes)
        assertNull(data.characterVersion)
        assertNull("empty stSource primitive must collapse to null", data.stSource)
        assertNull(data.stCreationDate)
        assertNull(data.stModificationDate)
    }

    @Test
    fun `numeric stCreationDate in epoch seconds formats to ISO local date`() {
        // 1735689600 → 2025-01-01 UTC. Using system-local formatting: assert via the same math.
        val expected = LocalDate.ofEpochDay(
            java.time.Instant.ofEpochSecond(1735689600L)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toEpochDay()
        ).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val card = cardWith(extensions = buildJsonObject {
            put("st", buildJsonObject {
                put("stCreationDate", JsonPrimitive(1735689600))
            })
        })

        assertEquals(expected, aboutCardData(card).stCreationDate)
    }

    @Test
    fun `numeric stCreationDate in epoch millis also formats to ISO local date`() {
        // 1735689600000 ms = 2025-01-01. A heuristic in formatStDate treats > 10B as millis.
        val expectedMs = java.time.Instant.ofEpochMilli(1735689600000L)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

        val card = cardWith(extensions = buildJsonObject {
            put("st", buildJsonObject {
                put("stCreationDate", JsonPrimitive(1735689600000L))
            })
        })

        assertEquals(expectedMs, aboutCardData(card).stCreationDate)
    }

    @Test
    fun `non-numeric stCreationDate passes through untouched`() {
        val card = cardWith(extensions = buildJsonObject {
            put("st", buildJsonObject {
                put("stCreationDate", JsonPrimitive("Jan 2025"))
            })
        })

        assertEquals("Jan 2025", aboutCardData(card).stCreationDate)
    }

    @Test
    fun `extensions without an st object hide all ST fields`() {
        val card = cardWith(
            creator = "Still Visible",
            extensions = buildJsonObject {
                put("custom-namespace", buildJsonObject { put("x", JsonPrimitive(1)) })
            },
        )

        val data = aboutCardData(card)

        assertEquals("Still Visible", data.creator)
        assertNull(data.stSource)
        assertNull(data.stCreationDate)
        assertNull(data.stModificationDate)
    }

    @Test
    fun `non-primitive st field value degrades to null instead of crashing`() {
        val card = cardWith(extensions = buildJsonObject {
            put("st", buildJsonObject {
                // stSource is an object instead of a string — defensive parsing must tolerate it.
                put("stSource", buildJsonObject { put("nested", JsonPrimitive("oops")) })
            })
        })

        val data = aboutCardData(card)
        assertNull("non-primitive stSource must collapse to null", data.stSource)
    }
}
