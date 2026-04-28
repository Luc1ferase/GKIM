package com.gkim.im.android.core.interop

enum class SillyTavernCardFormat {
    Png,
    Json,
    Unknown,
}

object SillyTavernCardCodec {
    const val MaxPngBytes: Int = 8 * 1024 * 1024
    const val MaxJsonBytes: Int = 1 * 1024 * 1024

    private val PngSignature: ByteArray = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    private val TextChunkType: ByteArray = byteArrayOf(0x74, 0x45, 0x58, 0x74)

    fun detectFormat(bytes: ByteArray): SillyTavernCardFormat {
        if (hasPngSignature(bytes)) return SillyTavernCardFormat.Png
        if (looksLikeJson(bytes)) return SillyTavernCardFormat.Json
        return SillyTavernCardFormat.Unknown
    }

    fun estimateSize(bytes: ByteArray): Int = bytes.size

    fun hasPngTextChunk(bytes: ByteArray): Boolean {
        if (!hasPngSignature(bytes)) return false
        var pos = PngSignature.size
        while (pos + 8 <= bytes.size) {
            val length = readUInt32BigEndian(bytes, pos) ?: return false
            if (length < 0) return false
            val typeStart = pos + 4
            val dataStart = typeStart + 4
            val crcStartL = dataStart.toLong() + length.toLong()
            val nextPosL = crcStartL + 4
            if (dataStart > bytes.size || crcStartL > bytes.size || nextPosL > bytes.size) {
                return false
            }
            if (matchesAt(bytes, typeStart, TextChunkType)) return true
            pos = nextPosL.toInt()
        }
        return false
    }

    fun fitsPngSizeLimit(bytes: ByteArray): Boolean = bytes.size in 1..MaxPngBytes

    fun fitsJsonSizeLimit(bytes: ByteArray): Boolean = bytes.size in 1..MaxJsonBytes

    private fun hasPngSignature(bytes: ByteArray): Boolean {
        if (bytes.size < PngSignature.size) return false
        for (i in PngSignature.indices) {
            if (bytes[i] != PngSignature[i]) return false
        }
        return true
    }

    private fun looksLikeJson(bytes: ByteArray): Boolean {
        var index = 0
        if (index + 2 < bytes.size &&
            bytes[index] == 0xEF.toByte() &&
            bytes[index + 1] == 0xBB.toByte() &&
            bytes[index + 2] == 0xBF.toByte()
        ) {
            index += 3
        }
        while (index < bytes.size && bytes[index].isWhitespace()) index++
        if (index >= bytes.size) return false
        val first = bytes[index]
        return first == '{'.code.toByte() || first == '['.code.toByte()
    }

    private fun readUInt32BigEndian(bytes: ByteArray, pos: Int): Int? {
        if (pos + 4 > bytes.size) return null
        val b0 = bytes[pos].toInt() and 0xFF
        val b1 = bytes[pos + 1].toInt() and 0xFF
        val b2 = bytes[pos + 2].toInt() and 0xFF
        val b3 = bytes[pos + 3].toInt() and 0xFF
        val combined = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        if (combined < 0) return null
        return combined
    }

    private fun matchesAt(bytes: ByteArray, pos: Int, expected: ByteArray): Boolean {
        if (pos + expected.size > bytes.size) return false
        for (i in expected.indices) {
            if (bytes[pos + i] != expected[i]) return false
        }
        return true
    }

    private fun Byte.isWhitespace(): Boolean {
        val v = this.toInt()
        return v == 0x20 || v == 0x09 || v == 0x0A || v == 0x0D
    }
}
