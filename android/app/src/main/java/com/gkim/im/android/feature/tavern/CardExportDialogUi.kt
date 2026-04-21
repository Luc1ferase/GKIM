package com.gkim.im.android.feature.tavern

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.data.repository.CardInteropRepository
import com.gkim.im.android.data.repository.ExportedCardFormat
import com.gkim.im.android.data.repository.ExportedCardPayload
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val CardExportFileProviderAuthority = "com.gkim.im.android.cardexport.fileprovider"
private const val CardExportSubdir = "card-exports"
private const val DownloadsRelativeSubdir = "SillyTavernCards"

internal fun exportErrorCopy(code: String, englishLocale: Boolean): String = when (code) {
    "share_cancelled" -> if (englishLocale) "Share was cancelled." else "分享已取消。"
    "no_share_target" -> if (englishLocale) {
        "No app available to receive this file."
    } else {
        "未找到可接收该文件的应用。"
    }
    "downloads_unavailable" -> if (englishLocale) {
        "Downloads folder is unavailable on this device."
    } else {
        "当前设备无法访问下载目录。"
    }
    "write_failed" -> if (englishLocale) {
        "Could not write the exported card. Please try again."
    } else {
        "写入导出文件失败，请稍后重试。"
    }
    "404_unknown_card" -> if (englishLocale) {
        "The card is no longer available."
    } else {
        "该角色卡已不可用。"
    }
    else -> if (englishLocale) "Export failed: $code" else "导出失败：$code"
}

@Composable
internal fun rememberCardExportDispatcher(): CardExportDispatcher {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        CardExportDispatcher { payload, target ->
            when (target) {
                CardExportTarget.Share -> shareExportedCard(context, payload)
                CardExportTarget.Downloads -> saveExportedCardToDownloads(context, payload)
            }
        }
    }
}

private fun shareExportedCard(context: Context, payload: ExportedCardPayload): Result<Unit> =
    runCatching {
        val cacheDir = File(context.cacheDir, CardExportSubdir).apply { mkdirs() }
        val outFile = File(cacheDir, payload.filename)
        FileOutputStream(outFile).use { it.write(payload.bytes) }
        val uri: Uri =
            FileProvider.getUriForFile(context, CardExportFileProviderAuthority, outFile)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = payload.contentType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, payload.filename).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (send.resolveActivity(context.packageManager) == null) {
            throw IllegalStateException("no_share_target")
        }
        context.startActivity(chooser)
    }.recoverCatching { throwable ->
        throw IllegalStateException(throwable.message ?: "share_failed", throwable)
    }

private fun saveExportedCardToDownloads(
    context: Context,
    payload: ExportedCardPayload,
): Result<Unit> = runCatching<Unit> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, payload.filename)
            put(MediaStore.Downloads.MIME_TYPE, payload.contentType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/" + DownloadsRelativeSubdir,
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("downloads_unavailable")
        resolver.openOutputStream(uri)?.use { out -> out.write(payload.bytes) }
            ?: throw IllegalStateException("write_failed")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    } else {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IllegalStateException("downloads_unavailable")
        val dir = File(base, DownloadsRelativeSubdir).apply { mkdirs() }
        val target = File(dir, payload.filename)
        FileOutputStream(target).use { it.write(payload.bytes) }
    }
}.recoverCatching { throwable ->
    throw IllegalStateException(throwable.message ?: "write_failed", throwable)
}

@Composable
fun CardExportDialog(
    cardId: String,
    initialFormat: ExportedCardFormat,
    repository: CardInteropRepository,
    dispatcher: CardExportDispatcher,
    onDismiss: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var state by remember(cardId, initialFormat) {
        mutableStateOf(initialExportDialogState(initialFormat, appLanguage))
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.completed) {
        if (state.completed) onDismiss()
    }

    Dialog(onDismissRequest = { if (!state.inFlight) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AetherColors.SurfaceContainerHigh, shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .testTag("card-export-dialog"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = appLanguage.pick(
                    when (initialFormat) {
                        ExportedCardFormat.Png -> "Export as PNG"
                        ExportedCardFormat.Json -> "Export as JSON"
                    },
                    when (initialFormat) {
                        ExportedCardFormat.Png -> "导出为 PNG"
                        ExportedCardFormat.Json -> "导出为 JSON"
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                color = AetherColors.OnSurface,
                modifier = Modifier.testTag("card-export-dialog-title"),
            )
            Text(
                text = appLanguage.pick(
                    "Language bundled with the card.",
                    "随卡片导出的语言。",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AetherColors.OnSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectionPill(
                    label = "EN",
                    active = state.language == "en",
                    enabled = !state.inFlight,
                    onClick = { state = state.withLanguage("en") },
                    modifier = Modifier.testTag("card-export-dialog-language-en"),
                )
                SelectionPill(
                    label = "ZH",
                    active = state.language == "zh",
                    enabled = !state.inFlight,
                    onClick = { state = state.withLanguage("zh") },
                    modifier = Modifier.testTag("card-export-dialog-language-zh"),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SelectionPill(
                    label = appLanguage.pick("With alt text", "包含另一语言"),
                    active = state.includeTranslationAlt,
                    enabled = !state.inFlight,
                    onClick = {
                        state = state.withIncludeTranslationAlt(!state.includeTranslationAlt)
                    },
                    modifier = Modifier.testTag("card-export-dialog-translation-alt"),
                )
            }
            Text(
                text = appLanguage.pick("Delivery", "导出方式"),
                style = MaterialTheme.typography.bodySmall,
                color = AetherColors.OnSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectionPill(
                    label = appLanguage.pick("Share sheet", "系统分享"),
                    active = state.target == CardExportTarget.Share,
                    enabled = !state.inFlight,
                    onClick = { state = state.withTarget(CardExportTarget.Share) },
                    modifier = Modifier.testTag("card-export-dialog-target-share"),
                )
                SelectionPill(
                    label = appLanguage.pick("Save to Downloads", "保存到下载"),
                    active = state.target == CardExportTarget.Downloads,
                    enabled = !state.inFlight,
                    onClick = { state = state.withTarget(CardExportTarget.Downloads) },
                    modifier = Modifier.testTag("card-export-dialog-target-downloads"),
                )
            }
            state.errorCode?.let { code ->
                Text(
                    text = exportErrorCopy(code, englishLocale = appLanguage == AppLanguage.English),
                    color = AetherColors.Danger,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("card-export-dialog-error"),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = appLanguage.pick("Cancel", "取消"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.OnSurfaceVariant,
                    modifier = Modifier
                        .background(AetherColors.SurfaceContainerLow, shape = CircleShape)
                        .clickable(enabled = !state.inFlight, onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("card-export-dialog-cancel"),
                )
                Text(
                    text = if (state.inFlight) {
                        appLanguage.pick("Exporting…", "导出中…")
                    } else {
                        appLanguage.pick("Export", "导出")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.OnSurface,
                    modifier = Modifier
                        .background(AetherColors.PrimaryContainer, shape = CircleShape)
                        .clickable(enabled = !state.inFlight) {
                            val current = state
                            state = current.markInFlight()
                            coroutineScope.launch {
                                val outcome = invokeCardExport(
                                    cardId = cardId,
                                    state = current,
                                    repository = repository,
                                    dispatcher = dispatcher,
                                )
                                state = when (outcome) {
                                    is CardExportInvocationOutcome.Success -> state.markCompleted()
                                    is CardExportInvocationOutcome.Failed -> state.markFailed(outcome.code)
                                }
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .testTag("card-export-dialog-submit"),
                )
            }
        }
    }
}

@Composable
private fun SelectionPill(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (active) "[$label]" else label,
        style = MaterialTheme.typography.labelLarge,
        color = if (active) AetherColors.Primary else AetherColors.OnSurface,
        modifier = modifier
            .background(
                if (active) AetherColors.SurfaceContainerHighest else AetherColors.SurfaceContainerLow,
                shape = CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
