package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.interop.SillyTavernCardCodec
import com.gkim.im.android.core.interop.SillyTavernCardFormat

sealed interface TavernImportSelectionResult {
    data class Accepted(
        val format: SillyTavernCardFormat,
        val filename: String,
        val bytes: ByteArray,
    ) : TavernImportSelectionResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Accepted) return false
            return format == other.format &&
                filename == other.filename &&
                bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = format.hashCode()
            result = 31 * result + filename.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    data class Rejected(val code: String) : TavernImportSelectionResult
}

internal fun evaluateImportSelection(
    bytes: ByteArray,
    filename: String,
): TavernImportSelectionResult {
    if (bytes.isEmpty()) return TavernImportSelectionResult.Rejected("empty_payload")
    val format = SillyTavernCardCodec.detectFormat(bytes)
    if (format == SillyTavernCardFormat.Unknown) {
        return TavernImportSelectionResult.Rejected("unsupported_format")
    }
    if (format == SillyTavernCardFormat.Png && !SillyTavernCardCodec.fitsPngSizeLimit(bytes)) {
        return TavernImportSelectionResult.Rejected("payload_too_large")
    }
    if (format == SillyTavernCardFormat.Json && !SillyTavernCardCodec.fitsJsonSizeLimit(bytes)) {
        return TavernImportSelectionResult.Rejected("payload_too_large")
    }
    return TavernImportSelectionResult.Accepted(format, filename, bytes)
}

data class TavernImportEntryState(
    val errorCode: String? = null,
)

internal fun importErrorCopy(code: String, englishLocale: Boolean): String = when (code) {
    "empty_payload" -> if (englishLocale) "The selected file is empty." else "所选文件为空。"
    "unsupported_format" -> if (englishLocale) {
        "Unsupported file — only SillyTavern PNG or JSON card files are accepted."
    } else {
        "文件格式不支持——仅支持 SillyTavern 的 PNG 或 JSON 角色卡。"
    }
    "payload_too_large" -> if (englishLocale) {
        "This card is larger than the 8 MiB PNG / 1 MiB JSON import limit."
    } else {
        "这张卡超过了 8 MiB（PNG）/ 1 MiB（JSON）的导入上限。"
    }
    else -> if (englishLocale) "Import failed: $code" else "导入失败：$code"
}
