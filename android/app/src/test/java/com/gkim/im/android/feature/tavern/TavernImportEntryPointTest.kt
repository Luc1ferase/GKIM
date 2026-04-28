package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.interop.SillyTavernCardCodec
import com.gkim.im.android.core.interop.SillyTavernCardFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernImportEntryPointTest {
    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    @Test
    fun `evaluateImportSelection accepts a valid PNG`() {
        val bytes = pngSignature + ByteArray(16)
        val result = evaluateImportSelection(bytes, "aria.png")
        assertTrue(result is TavernImportSelectionResult.Accepted)
        val accepted = result as TavernImportSelectionResult.Accepted
        assertEquals(SillyTavernCardFormat.Png, accepted.format)
        assertEquals("aria.png", accepted.filename)
    }

    @Test
    fun `evaluateImportSelection accepts a valid JSON`() {
        val bytes = "{\"name\":\"Aria\"}".toByteArray(Charsets.UTF_8)
        val result = evaluateImportSelection(bytes, "aria.json")
        assertTrue(result is TavernImportSelectionResult.Accepted)
        assertEquals(SillyTavernCardFormat.Json, (result as TavernImportSelectionResult.Accepted).format)
    }

    @Test
    fun `evaluateImportSelection rejects empty selections as empty_payload`() {
        val result = evaluateImportSelection(ByteArray(0), "empty.png")
        assertEquals(TavernImportSelectionResult.Rejected("empty_payload"), result)
    }

    @Test
    fun `evaluateImportSelection rejects unrecognized binaries as unsupported_format`() {
        val result = evaluateImportSelection("not a card".toByteArray(Charsets.UTF_8), "mystery.bin")
        assertEquals(TavernImportSelectionResult.Rejected("unsupported_format"), result)
    }

    @Test
    fun `evaluateImportSelection rejects oversized PNG as payload_too_large`() {
        val huge = pngSignature + ByteArray(SillyTavernCardCodec.MaxPngBytes)
        val result = evaluateImportSelection(huge, "huge.png")
        assertEquals(TavernImportSelectionResult.Rejected("payload_too_large"), result)
    }

    @Test
    fun `evaluateImportSelection rejects oversized JSON as payload_too_large`() {
        val big = ByteArray(SillyTavernCardCodec.MaxJsonBytes + 1) { 0x20 }
        big[0] = '{'.code.toByte()
        val result = evaluateImportSelection(big, "fat.json")
        assertEquals(TavernImportSelectionResult.Rejected("payload_too_large"), result)
    }

    @Test
    fun `importErrorCopy has distinct English and Chinese translations for every supported code`() {
        for (code in listOf("empty_payload", "unsupported_format", "payload_too_large")) {
            val english = importErrorCopy(code, englishLocale = true)
            val chinese = importErrorCopy(code, englishLocale = false)
            assertTrue("english for $code is blank", english.isNotBlank())
            assertTrue("chinese for $code is blank", chinese.isNotBlank())
            assertTrue("english and chinese are identical for $code", english != chinese)
        }
    }

    @Test
    fun `importErrorCopy surfaces an unknown code with a clear message`() {
        val copy = importErrorCopy("surprise_code", englishLocale = true)
        assertTrue(copy.contains("surprise_code"))
    }

    @Test
    fun `TavernImportEntryState defaults to no error`() {
        val state = TavernImportEntryState()
        assertEquals(null, state.errorCode)
    }
}
