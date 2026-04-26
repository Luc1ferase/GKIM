package com.gkim.im.android.feature.chat

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
import com.gkim.im.android.data.repository.CompanionTurnRepository
import com.gkim.im.android.data.repository.ExportedChatPayload
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val ChatExportFileProviderAuthority = "com.gkim.im.android.chatexport.fileprovider"
private const val ChatExportSubdir = "chat-exports"
private const val DownloadsRelativeSubdir = "GKIMChats"

internal fun chatExportErrorCopy(code: String, englishLocale: Boolean): String = when (code) {
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
        "Could not write the exported chat. Please try again."
    } else {
        "写入导出文件失败，请稍后重试。"
    }
    "404_unknown_conversation" -> if (englishLocale) {
        "The conversation is no longer available."
    } else {
        "该会话已不可用。"
    }
    "unsupported_format" -> if (englishLocale) {
        "Unsupported export format."
    } else {
        "不支持的导出格式。"
    }
    "network_failure" -> if (englishLocale) {
        "Network error while exporting. Please try again."
    } else {
        "导出时遇到网络错误，请稍后重试。"
    }
    else -> if (englishLocale) "Export failed: $code" else "导出失败：$code"
}

@Composable
internal fun rememberChatExportDispatcher(): ChatExportDispatcher {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        ChatExportDispatcher { payload, target ->
            when (target) {
                ChatExportTarget.Share -> shareExportedChat(context, payload)
                ChatExportTarget.Downloads -> saveExportedChatToDownloads(context, payload)
            }
        }
    }
}

private fun shareExportedChat(context: Context, payload: ExportedChatPayload): Result<Unit> =
    runCatching {
        val cacheDir = File(context.cacheDir, ChatExportSubdir).apply { mkdirs() }
        val outFile = File(cacheDir, payload.filename)
        FileOutputStream(outFile).use { it.write(payload.bytes) }
        val uri: Uri =
            FileProvider.getUriForFile(context, ChatExportFileProviderAuthority, outFile)
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

private fun saveExportedChatToDownloads(
    context: Context,
    payload: ExportedChatPayload,
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
fun ChatExportDialog(
    conversationId: String,
    repository: CompanionTurnRepository,
    dispatcher: ChatExportDispatcher,
    onDismiss: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    var state by remember(conversationId) {
        mutableStateOf(initialChatExportDialogState(appLanguage))
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
                .testTag("chat-export-dialog"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = appLanguage.pick("Export chat as JSONL", "导出对话为 JSONL"),
                style = MaterialTheme.typography.titleMedium,
                color = AetherColors.OnSurface,
                modifier = Modifier.testTag("chat-export-dialog-title"),
            )
            Text(
                text = appLanguage.pick(
                    "Active path keeps the visible turns; full tree includes alternates.",
                    "「当前路径」仅保留可见对话；「完整树」包含所有备选回复。",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AetherColors.OnSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChatExportPill(
                    label = appLanguage.pick("Active path", "当前路径"),
                    active = state.pathOnly,
                    enabled = !state.inFlight,
                    onClick = { state = state.withPathOnly(true) },
                    modifier = Modifier.testTag("chat-export-dialog-path-only-active"),
                )
                ChatExportPill(
                    label = appLanguage.pick("Full tree", "完整树"),
                    active = !state.pathOnly,
                    enabled = !state.inFlight,
                    onClick = { state = state.withPathOnly(false) },
                    modifier = Modifier.testTag("chat-export-dialog-path-only-full"),
                )
            }
            Text(
                text = appLanguage.pick("Language", "语言"),
                style = MaterialTheme.typography.bodySmall,
                color = AetherColors.OnSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChatExportPill(
                    label = "EN",
                    active = state.language == "en",
                    enabled = !state.inFlight,
                    onClick = { state = state.withLanguage("en") },
                    modifier = Modifier.testTag("chat-export-dialog-language-en"),
                )
                ChatExportPill(
                    label = "ZH",
                    active = state.language == "zh",
                    enabled = !state.inFlight,
                    onClick = { state = state.withLanguage("zh") },
                    modifier = Modifier.testTag("chat-export-dialog-language-zh"),
                )
            }
            Text(
                text = appLanguage.pick("Delivery", "导出方式"),
                style = MaterialTheme.typography.bodySmall,
                color = AetherColors.OnSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChatExportPill(
                    label = appLanguage.pick("Share sheet", "系统分享"),
                    active = state.target == ChatExportTarget.Share,
                    enabled = !state.inFlight,
                    onClick = { state = state.withTarget(ChatExportTarget.Share) },
                    modifier = Modifier.testTag("chat-export-dialog-target-share"),
                )
                ChatExportPill(
                    label = appLanguage.pick("Save to Downloads", "保存到下载"),
                    active = state.target == ChatExportTarget.Downloads,
                    enabled = !state.inFlight,
                    onClick = { state = state.withTarget(ChatExportTarget.Downloads) },
                    modifier = Modifier.testTag("chat-export-dialog-target-downloads"),
                )
            }
            state.errorCode?.let { code ->
                Text(
                    text = chatExportErrorCopy(code, englishLocale = appLanguage == AppLanguage.English),
                    color = AetherColors.Danger,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("chat-export-dialog-error"),
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
                        .testTag("chat-export-dialog-cancel"),
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
                                val outcome = invokeChatExport(
                                    conversationId = conversationId,
                                    state = current,
                                    repository = repository,
                                    dispatcher = dispatcher,
                                )
                                state = when (outcome) {
                                    is ChatExportInvocationOutcome.Success -> state.markCompleted()
                                    is ChatExportInvocationOutcome.Failed -> state.markFailed(outcome.code)
                                }
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .testTag("chat-export-dialog-submit"),
                )
            }
        }
    }
}

@Composable
private fun ChatExportPill(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (active) AetherColors.PrimaryContainer else AetherColors.SurfaceContainerLow
    val color = if (active) AetherColors.OnSurface else AetherColors.OnSurfaceVariant
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier
            .background(background, shape = CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
