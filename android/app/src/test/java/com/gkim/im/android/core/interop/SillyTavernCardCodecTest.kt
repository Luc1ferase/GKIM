package com.gkim.im.android.core.interop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SillyTavernCardCodecTest {
    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    @Test
    fun `detectFormat recognizes PNG signature`() {
        val bytes = pngSignature + ByteArray(32)
        assertEquals(SillyTavernCardFormat.Png, SillyTavernCardCodec.detectFormat(bytes))
    }

    @Test
    fun `detectFormat recognizes JSON object`() {
        val bytes = "  \n{\"name\":\"Aria\"}".toByteArray(Charsets.UTF_8)
        assertEquals(SillyTavernCardFormat.Json, SillyTavernCardCodec.detectFormat(bytes))
    }

    @Test
    fun `detectFormat recognizes JSON with UTF-8 BOM`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bytes = bom + "{\"x\":1}".toByteArray(Charsets.UTF_8)
        assertEquals(SillyTavernCardFormat.Json, SillyTavernCardCodec.detectFormat(bytes))
    }

    @Test
    fun `detectFormat recognizes JSON array at top level`() {
        val bytes = "[1,2,3]".toByteArray(Charsets.UTF_8)
        assertEquals(SillyTavernCardFormat.Json, SillyTavernCardCodec.detectFormat(bytes))
    }

    @Test
    fun `detectFormat rejects arbitrary binary as Unknown`() {
        val bytes = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)
        assertEquals(SillyTavernCardFormat.Unknown, SillyTavernCardCodec.detectFormat(bytes))
    }

    @Test
    fun `detectFormat rejects plain prose as Unknown`() {
        val bytes = "this is not a card".toByteArray(Charsets.UTF_8)
        assertEquals(SillyTavernCardFormat.Unknown, SillyTavernCardCodec.detectFormat(bytes))
    }

    @Test
    fun `detectFormat rejects empty input as Unknown`() {
        assertEquals(SillyTavernCardFormat.Unknown, SillyTavernCardCodec.detectFormat(ByteArray(0)))
    }

    @Test
    fun `hasPngTextChunk finds a tEXt chunk after the signature`() {
        val payload = "chara".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x00) +
            "eyJuYW1lIjoiQXJpYSJ9".toByteArray(Charsets.US_ASCII)
        val chunk = encodeChunk("tEXt", payload)
        val bytes = pngSignature + chunk + encodeChunk("IEND", ByteArray(0))
        assertTrue(SillyTavernCardCodec.hasPngTextChunk(bytes))
    }

    @Test
    fun `hasPngTextChunk returns false when no tEXt chunk is present`() {
        val bytes = pngSignature + encodeChunk("IHDR", ByteArray(13)) + encodeChunk("IEND", ByteArray(0))
        assertFalse(SillyTavernCardCodec.hasPngTextChunk(bytes))
    }

    @Test
    fun `hasPngTextChunk returns false when signature is missing`() {
        val bytes = "{\"hello\":1}".toByteArray(Charsets.UTF_8)
        assertFalse(SillyTavernCardCodec.hasPngTextChunk(bytes))
    }

    @Test
    fun `hasPngTextChunk is robust against malformed chunk lengths`() {
        val bogus = byteArrayOf(
            0x7F.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0x74, 0x45, 0x58, 0x74,
        )
        val bytes = pngSignature + bogus
        assertFalse(SillyTavernCardCodec.hasPngTextChunk(bytes))
    }

    @Test
    fun `estimateSize reports the byte length`() {
        val bytes = ByteArray(4321)
        assertEquals(4321, SillyTavernCardCodec.estimateSize(bytes))
    }

    @Test
    fun `size limits cap PNG at 8 MiB and JSON at 1 MiB`() {
        assertEquals(8 * 1024 * 1024, SillyTavernCardCodec.MaxPngBytes)
        assertEquals(1 * 1024 * 1024, SillyTavernCardCodec.MaxJsonBytes)

        assertTrue(SillyTavernCardCodec.fitsPngSizeLimit(ByteArray(1)))
        assertTrue(SillyTavernCardCodec.fitsPngSizeLimit(ByteArray(SillyTavernCardCodec.MaxPngBytes)))
        assertFalse(SillyTavernCardCodec.fitsPngSizeLimit(ByteArray(0)))
        assertFalse(SillyTavernCardCodec.fitsPngSizeLimit(ByteArray(SillyTavernCardCodec.MaxPngBytes + 1)))

        assertTrue(SillyTavernCardCodec.fitsJsonSizeLimit(ByteArray(1)))
        assertTrue(SillyTavernCardCodec.fitsJsonSizeLimit(ByteArray(SillyTavernCardCodec.MaxJsonBytes)))
        assertFalse(SillyTavernCardCodec.fitsJsonSizeLimit(ByteArray(0)))
        assertFalse(SillyTavernCardCodec.fitsJsonSizeLimit(ByteArray(SillyTavernCardCodec.MaxJsonBytes + 1)))
    }

    private fun encodeChunk(type: String, data: ByteArray): ByteArray {
        val length = data.size
        val lengthBytes = byteArrayOf(
            ((length ushr 24) and 0xFF).toByte(),
            ((length ushr 16) and 0xFF).toByte(),
            ((length ushr 8) and 0xFF).toByte(),
            (length and 0xFF).toByte(),
        )
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        val crcBytes = byteArrayOf(0, 0, 0, 0)
        return lengthBytes + typeBytes + data + crcBytes
    }
}
