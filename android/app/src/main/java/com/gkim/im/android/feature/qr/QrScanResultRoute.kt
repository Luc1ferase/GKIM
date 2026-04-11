package com.gkim.im.android.feature.qr

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick

const val QrScanRoutePath = "qr-scan"
const val QrResultRoutePattern = "qr-scan-result/{payload}"

fun qrResultRoute(payload: String): String = "qr-scan-result/${Uri.encode(payload)}"

@Composable
fun QrScanResultRoute(
    payload: String,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("qr-result-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            eyebrow = appLanguage.pick("Messages", "消息"),
            title = appLanguage.pick("QR result", "二维码内容"),
            description = appLanguage.pick(
                "Decoded content is shown here without triggering any jump or account action.",
                "这里只展示已解析内容，不会自动跳转，也不会触发账号动作。",
            ),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
            modifier = Modifier.testTag("qr-result-back"),
        )

        GlassCard {
            Text(
                text = appLanguage.pick("DECODED PAYLOAD", "解析结果"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
            )
            Text(
                text = payload,
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.OnSurface,
                modifier = Modifier.testTag("qr-result-payload"),
            )
            Text(
                text = appLanguage.pick(
                    "No navigation or friend action has been triggered.",
                    "当前不会触发跳转，也不会发起好友动作。",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
            )
        }
    }
}
