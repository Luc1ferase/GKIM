package com.gkim.im.android.core.model

import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterCardPresetOverrideTest {

    @Test
    fun `default characterPresetId is null and extensions are not mutated`() {
        val card = baseCard()
        assertNull(card.characterPresetId)

        val dto = CompanionCharacterCardDto.fromCompanionCharacterCard(card)
        assertFalse(
            "extensions.st should not appear when characterPresetId is null and no other st keys exist",
            dto.extensions.containsKey("st"),
        )

        val resolved = card.resolve(AppLanguage.English)
        assertNull(resolved.characterPresetId)

        val rehydrated = dto.toCompanionCharacterCard()
        assertNull(rehydrated.characterPresetId)
    }

    @Test
    fun `non-null characterPresetId round-trips through extensions st charPresetId`() {
        val card = baseCard().copy(characterPresetId = "preset-foo")

        val dto = CompanionCharacterCardDto.fromCompanionCharacterCard(card)
        val st = dto.extensions["st"] as? JsonObject
        assertNotNull("extensions.st should be present after writing characterPresetId", st)
        val written = st!!["charPresetId"] as? JsonPrimitive
        assertNotNull("extensions.st.charPresetId should be a JsonPrimitive", written)
        assertEquals("preset-foo", written!!.content)

        val rehydrated = dto.toCompanionCharacterCard()
        assertEquals("preset-foo", rehydrated.characterPresetId)
        assertEquals("preset-foo", card.resolve(AppLanguage.English).characterPresetId)
    }

    @Test
    fun `incoming dto with extensions st charPresetId hydrates the typed field and preserves siblings`() {
        val incomingExtensions = JsonObject(
            mapOf(
                "st" to JsonObject(
                    mapOf(
                        "stSource" to JsonPrimitive("https://example.com/card"),
                        "charPresetId" to JsonPrimitive("preset-bar"),
                    ),
                ),
            ),
        )
        val dto = baseDto().copy(extensions = incomingExtensions)

        val card = dto.toCompanionCharacterCard()
        assertEquals("preset-bar", card.characterPresetId)

        val keptSt = card.extensions["st"] as? JsonObject
        assertNotNull(keptSt)
        assertEquals(
            "https://example.com/card",
            (keptSt!!["stSource"] as? JsonPrimitive)?.content,
        )

        val redto = CompanionCharacterCardDto.fromCompanionCharacterCard(card)
        val redoneSt = redto.extensions["st"] as? JsonObject
        assertNotNull(redoneSt)
        assertEquals("preset-bar", (redoneSt!!["charPresetId"] as? JsonPrimitive)?.content)
        assertEquals(
            "https://example.com/card",
            (redoneSt["stSource"] as? JsonPrimitive)?.content,
        )
    }

    @Test
    fun `clearing characterPresetId removes only that key from extensions st`() {
        val cardWithSibling = baseCard().copy(
            characterPresetId = "preset-foo",
            extensions = JsonObject(
                mapOf(
                    "st" to JsonObject(
                        mapOf(
                            "stSource" to JsonPrimitive("https://example.com/card"),
                            "charPresetId" to JsonPrimitive("preset-foo"),
                        ),
                    ),
                ),
            ),
        )
        val cleared = cardWithSibling.copy(characterPresetId = null)

        val dto = CompanionCharacterCardDto.fromCompanionCharacterCard(cleared)
        val st = dto.extensions["st"] as? JsonObject
        assertNotNull("st should remain because stSource is still set", st)
        assertEquals(
            "https://example.com/card",
            (st!!["stSource"] as? JsonPrimitive)?.content,
        )
        assertFalse(
            "charPresetId should be removed when typed field is cleared",
            st.containsKey("charPresetId"),
        )

        val rehydrated = dto.toCompanionCharacterCard()
        assertNull(rehydrated.characterPresetId)
    }

    @Test
    fun `clearing characterPresetId drops the st object when it becomes empty`() {
        val cardOnlyHadCharPresetId = baseCard().copy(
            characterPresetId = "preset-foo",
            extensions = JsonObject(
                mapOf(
                    "st" to JsonObject(
                        mapOf("charPresetId" to JsonPrimitive("preset-foo")),
                    ),
                ),
            ),
        )
        val cleared = cardOnlyHadCharPresetId.copy(characterPresetId = null)

        val dto = CompanionCharacterCardDto.fromCompanionCharacterCard(cleared)
        assertFalse(
            "extensions.st should be dropped entirely when only charPresetId was inside it",
            dto.extensions.containsKey("st"),
        )
        assertNull(dto.toCompanionCharacterCard().characterPresetId)
    }

    @Test
    fun `blank charPresetId in incoming extensions is treated as null`() {
        val dto = baseDto().copy(
            extensions = JsonObject(
                mapOf(
                    "st" to JsonObject(
                        mapOf("charPresetId" to JsonPrimitive("   ")),
                    ),
                ),
            ),
        )
        assertNull(dto.toCompanionCharacterCard().characterPresetId)
    }

    @Test
    fun `non-object extensions st is ignored when reading and overwritten when writing`() {
        val dto = baseDto().copy(
            extensions = JsonObject(
                mapOf("st" to JsonPrimitive("not-an-object")),
            ),
        )
        val card = dto.toCompanionCharacterCard()
        assertNull(card.characterPresetId)

        val withOverride = card.copy(characterPresetId = "preset-baz")
        val redto = CompanionCharacterCardDto.fromCompanionCharacterCard(withOverride)
        val st = redto.extensions["st"] as? JsonObject
        assertNotNull("st must be replaced with a proper JsonObject when we need to write", st)
        assertEquals("preset-baz", (st!!["charPresetId"] as? JsonPrimitive)?.content)
    }

    @Test
    fun `resolve forwards characterPresetId for both null and non-null values`() {
        val nullCard = baseCard()
        assertNull(nullCard.resolve(AppLanguage.English).characterPresetId)
        assertNull(nullCard.resolve(AppLanguage.Chinese).characterPresetId)

        val withOverride = baseCard().copy(characterPresetId = "preset-resolve")
        assertEquals("preset-resolve", withOverride.resolve(AppLanguage.English).characterPresetId)
        assertEquals("preset-resolve", withOverride.resolve(AppLanguage.Chinese).characterPresetId)
    }

    @Test
    fun `repeated round-trips are stable when characterPresetId is set`() {
        val original = baseCard().copy(
            characterPresetId = "preset-stable",
            extensions = JsonObject(
                mapOf(
                    "st" to JsonObject(
                        mapOf("stSource" to JsonPrimitive("https://example.com/x")),
                    ),
                ),
            ),
        )

        val r1 = CompanionCharacterCardDto.fromCompanionCharacterCard(original).toCompanionCharacterCard()
        val r2 = CompanionCharacterCardDto.fromCompanionCharacterCard(r1).toCompanionCharacterCard()

        assertEquals("preset-stable", r1.characterPresetId)
        assertEquals("preset-stable", r2.characterPresetId)

        val r2St = r2.extensions["st"] as? JsonObject
        assertNotNull(r2St)
        assertTrue(r2St!!.containsKey("charPresetId"))
        assertTrue(r2St.containsKey("stSource"))
    }

    private fun baseCard(): CompanionCharacterCard = CompanionCharacterCard(
        id = "card-test",
        displayName = LocalizedText("Test Card", "测试卡"),
        roleLabel = LocalizedText("Companion", "伙伴"),
        summary = LocalizedText("A card for testing", "测试用卡片"),
        firstMes = LocalizedText("Hello!", "你好！"),
        avatarText = "T",
        accent = AccentTone.Primary,
        source = CompanionCharacterSource.UserAuthored,
    )

    private fun baseDto(): CompanionCharacterCardDto = CompanionCharacterCardDto(
        id = "card-test",
        displayName = LocalizedTextDto("Test Card", "测试卡"),
        roleLabel = LocalizedTextDto("Companion", "伙伴"),
        summary = LocalizedTextDto("A card for testing", "测试用卡片"),
        firstMes = LocalizedTextDto("Hello!", "你好！"),
        avatarText = "T",
        accent = "primary",
        source = "user_authored",
    )
}
