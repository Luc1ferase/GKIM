package com.gkim.im.android.feature.tavern

import com.gkim.im.android.data.remote.im.CardExportResponseDto
import com.gkim.im.android.data.remote.im.CardExportWarningDto
import com.gkim.im.android.data.repository.CardExportWarning
import com.gkim.im.android.data.repository.ExportedCardFormat
import com.gkim.im.android.data.repository.ExportedCardPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class CardExportLorebookRoundTripTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `CardExportResponseDto decodes an empty warnings list by default`() {
        val minimal = """
            {
              "format": "json",
              "filename": "card.json",
              "contentType": "application/json",
              "encoding": "utf-8",
              "payload": "{}"
            }
        """.trimIndent()
        val dto = json.decodeFromString(CardExportResponseDto.serializer(), minimal)
        assertTrue(dto.warnings.isEmpty())
    }

    @Test
    fun `CardExportResponseDto surfaces a multiple_bindings warning over the wire`() {
        val payload = """
            {
              "format": "json",
              "filename": "card.json",
              "contentType": "application/json",
              "encoding": "utf-8",
              "payload": "{}",
              "warnings": [
                {
                  "code": "multiple_bindings",
                  "field": "character_book",
                  "detail": "lb-extra-1,lb-extra-2"
                }
              ]
            }
        """.trimIndent()
        val dto = json.decodeFromString(CardExportResponseDto.serializer(), payload)
        assertEquals(1, dto.warnings.size)
        assertEquals("multiple_bindings", dto.warnings.single().code)
        assertEquals("character_book", dto.warnings.single().field)
        assertEquals("lb-extra-1,lb-extra-2", dto.warnings.single().detail)
    }

    @Test
    fun `CardExportWarningDto tolerates optional field and detail`() {
        val payload = """
            { "code": "lorebook_truncated" }
        """.trimIndent()
        val dto = json.decodeFromString(CardExportWarningDto.serializer(), payload)
        assertEquals("lorebook_truncated", dto.code)
        assertEquals(null, dto.field)
        assertEquals(null, dto.detail)
    }

    @Test
    fun `ExportedCardPayload carries warnings from the domain layer`() {
        val payload = ExportedCardPayload(
            format = ExportedCardFormat.Json,
            filename = "aria.json",
            contentType = "application/json",
            bytes = "{}".toByteArray(),
            warnings = listOf(
                CardExportWarning("multiple_bindings", "character_book", "lb-extra-1"),
            ),
        )
        assertEquals(1, payload.warnings.size)
        assertEquals("multiple_bindings", payload.warnings.single().code)
    }

    @Test
    fun `ExportedCardPayload equals distinguishes payloads with different warnings`() {
        val a = ExportedCardPayload(
            format = ExportedCardFormat.Json,
            filename = "x.json",
            contentType = "application/json",
            bytes = "{}".toByteArray(),
            warnings = emptyList(),
        )
        val b = a.copy(warnings = listOf(CardExportWarning("multiple_bindings")))
        assertFalse(a == b)
    }

    @Test
    fun `character_book export payload round-trips its entries through the bytes boundary`() {
        // Simulate server-produced JSON carrying a `character_book` with two entries — the
        // client's contract is to deliver the payload bytes to re-import unchanged.
        val serverPayload = """
            {
              "name": "Aria",
              "first_mes": "Hello.",
              "data": {
                "character_book": {
                  "name": "Aria lore",
                  "entries": [
                    {
                      "id": 1,
                      "keys": ["palace"],
                      "secondary_keys": [],
                      "content": "The palace overlooks the sea.",
                      "enabled": true,
                      "constant": false,
                      "case_sensitive": false,
                      "insertion_order": 10,
                      "extensions": {
                        "st": { "probability": 75 },
                        "stTranslationAlt": { "content": "宫殿俯瞰大海。" }
                      }
                    },
                    {
                      "id": 2,
                      "keys": [],
                      "secondary_keys": [],
                      "content": "Always include this.",
                      "enabled": true,
                      "constant": true,
                      "case_sensitive": false,
                      "insertion_order": 5,
                      "extensions": { "st": { "selective": true } }
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        val payload = ExportedCardPayload(
            format = ExportedCardFormat.Json,
            filename = "aria.json",
            contentType = "application/json",
            bytes = serverPayload.toByteArray(),
            warnings = emptyList(),
        )

        // Simulate a subsequent re-import by parsing the bytes unchanged — entry set and
        // `extensions.st.*` / `extensions.stTranslationAlt.*` must all survive the trip.
        val reimported = json.parseToJsonElement(String(payload.bytes, Charsets.UTF_8))
        val entries = reimported.jsonObject["data"]!!.jsonObject["character_book"]!!.jsonObject["entries"]!!.jsonArray
        assertEquals(2, entries.size)

        val first = entries[0].jsonObject
        assertEquals(1, first["id"]!!.jsonPrimitive.content.toInt())
        assertEquals("palace", first["keys"]!!.jsonArray.single().jsonPrimitive.content)
        assertEquals("The palace overlooks the sea.", first["content"]!!.jsonPrimitive.content)
        assertEquals(10, first["insertion_order"]!!.jsonPrimitive.content.toInt())
        assertEquals(
            "75",
            first["extensions"]!!.jsonObject["st"]!!.jsonObject["probability"]!!.jsonPrimitive.content,
        )
        assertEquals(
            "宫殿俯瞰大海。",
            first["extensions"]!!.jsonObject["stTranslationAlt"]!!.jsonObject["content"]!!.jsonPrimitive.content,
        )

        val second = entries[1].jsonObject
        assertEquals(2, second["id"]!!.jsonPrimitive.content.toInt())
        assertEquals(true, second["constant"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(5, second["insertion_order"]!!.jsonPrimitive.content.toInt())
        assertEquals(
            "true",
            second["extensions"]!!.jsonObject["st"]!!.jsonObject["selective"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `multiple_bindings warning is distinguishable by code so the UI can surface it`() {
        val payload = ExportedCardPayload(
            format = ExportedCardFormat.Png,
            filename = "aria.png",
            contentType = "image/png",
            bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
            warnings = listOf(
                CardExportWarning("multiple_bindings", "character_book", "lb-a,lb-b"),
                CardExportWarning("field_truncated", "personality"),
            ),
        )
        val multipleBindings = payload.warnings.firstOrNull { it.code == "multiple_bindings" }
        assertNotNull(multipleBindings)
        assertEquals("lb-a,lb-b", multipleBindings!!.detail)
    }

    @Test
    fun `warnings list preserves order from the server wire format`() {
        val dto = json.decodeFromString(
            CardExportResponseDto.serializer(),
            """
            {
              "format": "png",
              "filename": "card.png",
              "contentType": "image/png",
              "encoding": "base64",
              "payload": "",
              "warnings": [
                { "code": "a" }, { "code": "b" }, { "code": "c" }
              ]
            }
            """.trimIndent(),
        )
        assertEquals(listOf("a", "b", "c"), dto.warnings.map { it.code })
    }

    @Test
    fun `ExportedCardPayload defaults warnings to an empty list for backwards compatibility`() {
        val payload = ExportedCardPayload(
            format = ExportedCardFormat.Json,
            filename = "x.json",
            contentType = "application/json",
            bytes = "{}".toByteArray(),
        )
        assertTrue(payload.warnings.isEmpty())
    }
}
