package com.gkim.im.android.feature.tavern

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.repository.CardImportPreview
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.data.repository.DefaultCardInteropRepository
import com.gkim.im.android.data.repository.ExportedCardFormat
import com.gkim.im.android.data.repository.ExportedCardPayload
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class CardInteropRoundTripTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun roundTripPreservesEveryDeepPersonaFieldAndStarNamespace() = runBlocking {
        val fakeDelegate = InMemoryRoundTripBackend(json)
        val repository = DefaultCardInteropRepository(fakeDelegate)

        val original = CompanionCharacterCard(
            id = "staging-id",
            displayName = LocalizedText("Aria", "艾莉亚"),
            roleLabel = LocalizedText("Guide", "向导"),
            summary = LocalizedText("Calm guide through the archive.", "博物馆的沉稳向导。"),
            firstMes = LocalizedText("Hello traveller.", "你好，旅人。"),
            alternateGreetings = listOf(
                LocalizedText("Welcome back.", "欢迎回来。"),
                LocalizedText("The stacks remember you.", "书架记得你。"),
            ),
            systemPrompt = LocalizedText("Warm, patient librarian.", "温和而耐心的图书管理员。"),
            personality = LocalizedText("Patient, curious, bilingual.", "有耐心、好奇、精通双语。"),
            scenario = LocalizedText("A quiet night in the archive.", "档案馆的宁静夜晚。"),
            exampleDialogue = LocalizedText(
                "{{user}}: hello\\nAria: welcome",
                "{{user}}: 你好\\nAria: 欢迎",
            ),
            tags = listOf("guide", "archive", "bilingual"),
            creator = "roundtrip-fixture",
            creatorNotes = "For interop testing.",
            characterVersion = "1.0.0",
            avatarText = "AR",
            avatarUri = null,
            accent = AccentTone.Primary,
            source = CompanionCharacterSource.UserAuthored,
            extensions = buildJsonObject {
                put("stPostHistoryInstructions", "stay in character")
                put("stDepthPrompt", "a quiet corner of the archive")
                put("stNickname", "Aria the Archivist")
                put(
                    "st",
                    buildJsonObject { put("custom_flag", "preserve-me") },
                )
            },
        )

        val originalBytes = fakeDelegate.serializeForImport(original)
        val preview1 = repository.previewImport(originalBytes, "aria.json").getOrThrow()
        val persistedA = repository.commitImport(preview1, null, null).getOrThrow()

        val exportedA = repository.exportCard(
            cardId = persistedA.id,
            format = ExportedCardFormat.Json,
            language = "en",
            includeTranslationAlt = true,
        ).getOrThrow()

        val preview2 = repository.previewImport(exportedA.bytes, exportedA.filename).getOrThrow()
        val persistedB = repository.commitImport(preview2, null, null).getOrThrow()

        assertNotEquals(
            "Commit MUST assign fresh ids on re-import",
            persistedA.id,
            persistedB.id,
        )

        val idNormalizedA = persistedA.copy(id = "NORMALIZED")
        val idNormalizedB = persistedB.copy(id = "NORMALIZED")
        assertEquals(idNormalizedA, idNormalizedB)

        assertEquals(original.displayName, persistedB.displayName)
        assertEquals(original.personality, persistedB.personality)
        assertEquals(original.firstMes, persistedB.firstMes)
        assertEquals(original.alternateGreetings, persistedB.alternateGreetings)
        assertEquals(original.tags, persistedB.tags)
        assertEquals(original.extensions, persistedB.extensions)
    }

    @Test
    fun sizeGuardsRejectOversizedPngAndJsonBeforeHittingDelegate() = runBlocking {
        val fakeDelegate = InMemoryRoundTripBackend(json)
        val repository = DefaultCardInteropRepository(fakeDelegate)

        val oversizeJson = ByteArray(2 * 1024 * 1024).apply {
            this[0] = '{'.code.toByte()
        }
        val jsonResult = repository.previewImport(oversizeJson, "huge.json")
        assertTrue(jsonResult.isFailure)
        assertEquals("payload_too_large", jsonResult.exceptionOrNull()?.message)

        val oversizePng = ByteArray(9 * 1024 * 1024).apply {
            val sig = byteArrayOf(
                0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            )
            System.arraycopy(sig, 0, this, 0, sig.size)
        }
        val pngResult = repository.previewImport(oversizePng, "huge.png")
        assertTrue(pngResult.isFailure)
        assertEquals("payload_too_large", pngResult.exceptionOrNull()?.message)
    }

    @Test
    fun emptyAndUnsupportedPayloadsReturnTypedRejection() = runBlocking {
        val fakeDelegate = InMemoryRoundTripBackend(json)
        val repository = DefaultCardInteropRepository(fakeDelegate)

        val emptyResult = repository.previewImport(ByteArray(0), "empty.png")
        assertEquals("empty_payload", emptyResult.exceptionOrNull()?.message)

        val mysteryResult = repository.previewImport(
            "not a card payload".toByteArray(Charsets.UTF_8),
            "mystery.bin",
        )
        assertEquals("unsupported_format", mysteryResult.exceptionOrNull()?.message)
    }

    private class InMemoryRoundTripBackend(private val json: Json) : CardInteropRepository {
        private val store = mutableMapOf<String, CompanionCharacterCard>()
        private val previewsByToken = mutableMapOf<String, CompanionCharacterCard>()
        private val idCounter = AtomicInteger(0)

        fun serializeForImport(card: CompanionCharacterCard): ByteArray {
            val dto = CompanionCharacterCardDto.fromCompanionCharacterCard(card)
            return json.encodeToString(CompanionCharacterCardDto.serializer(), dto)
                .toByteArray(Charsets.UTF_8)
        }

        override suspend fun previewImport(
            bytes: ByteArray,
            filename: String,
        ): Result<CardImportPreview> = runCatching {
            val raw = json.decodeFromString(
                CompanionCharacterCardDto.serializer(),
                String(bytes, Charsets.UTF_8),
            )
            val card = raw.toCompanionCharacterCard()
            val previewToken = "preview-${idCounter.incrementAndGet()}"
            previewsByToken[previewToken] = card
            val stKeys = card.extensions.keys.filter { it.startsWith("st") }
            CardImportPreview(
                previewToken = previewToken,
                card = card,
                rawCardDto = raw,
                detectedLanguage = "en",
                warnings = emptyList(),
                stExtensionKeys = stKeys,
            )
        }

        override suspend fun commitImport(
            preview: CardImportPreview,
            overrides: CompanionCharacterCard?,
            languageOverride: String?,
        ): Result<CompanionCharacterCard> = runCatching {
            val cardSource = overrides ?: previewsByToken[preview.previewToken]
                ?: throw IllegalStateException("preview_not_found")
            val newId = "persisted-${idCounter.incrementAndGet()}"
            val persisted = cardSource.copy(id = newId)
            store[newId] = persisted
            previewsByToken.remove(preview.previewToken)
            persisted
        }

        override suspend fun exportCard(
            cardId: String,
            format: ExportedCardFormat,
            language: String,
            includeTranslationAlt: Boolean,
        ): Result<ExportedCardPayload> = runCatching {
            val card = store[cardId] ?: throw IllegalStateException("404_unknown_card")
            val dto = CompanionCharacterCardDto.fromCompanionCharacterCard(card)
            val bytes = json.encodeToString(CompanionCharacterCardDto.serializer(), dto)
                .toByteArray(Charsets.UTF_8)
            ExportedCardPayload(
                format = format,
                filename = when (format) {
                    ExportedCardFormat.Png -> "${card.avatarText}.png"
                    ExportedCardFormat.Json -> "${card.avatarText}.json"
                },
                contentType = when (format) {
                    ExportedCardFormat.Png -> "image/png"
                    ExportedCardFormat.Json -> "application/json"
                },
                bytes = bytes,
            ).also { assertNotNull(it) }
        }
    }
}
