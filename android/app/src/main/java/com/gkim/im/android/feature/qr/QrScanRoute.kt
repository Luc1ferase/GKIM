package com.gkim.im.android.feature.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GradientPrimaryButton
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick

@Composable
fun QrScanRoute(
    onBack: () -> Unit,
    onPayloadScanned: (String) -> Unit,
    qrScannerControllerFactory: QrScannerControllerFactory? = null,
) {
    val appLanguage = LocalAppLanguage.current
    val qrScannerController = qrScannerControllerFactory?.invoke(onPayloadScanned) ?: rememberQrScannerController(onPayloadScanned)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("qr-scan-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            eyebrow = appLanguage.pick("Messages", "消息"),
            title = appLanguage.pick("Scan QR code", "扫描二维码"),
            description = appLanguage.pick(
                "Scan a QR code and review the decoded content before anything else happens.",
                "扫描二维码后，先查看解析内容，再决定后续操作。",
            ),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
        )

        if (!qrScannerController.hasCameraPermission) {
            GlassCard {
                Text(
                    text = appLanguage.pick(
                        "Camera permission is required to scan QR content in-app.",
                        "需要相机权限才能在应用内扫描二维码。",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AetherColors.OnSurface,
                )
                GradientPrimaryButton(
                    label = appLanguage.pick("Grant camera access", "授权相机"),
                    onClick = qrScannerController.requestPermission,
                    modifier = Modifier.testTag("qr-scan-request-permission"),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(AetherColors.SurfaceContainerHigh)
                    .testTag("qr-scan-preview"),
            ) {
                qrScannerController.previewContent(Modifier.fillMaxSize())
            }
            Text(
                text = appLanguage.pick(
                    "Point the camera at a QR code. GKIM will only show the decoded content in this phase.",
                    "将相机对准二维码。当前阶段 GKIM 只会展示解析内容。",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.OnSurfaceVariant,
            )
        }
    }
}
