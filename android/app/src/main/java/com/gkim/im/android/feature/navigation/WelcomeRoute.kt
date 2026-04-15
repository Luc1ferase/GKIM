package com.gkim.im.android.feature.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick

@Composable
internal fun WelcomeRoute(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    val (surfaceScrimAlpha, primaryScrimAlpha, onSurfaceScrimAlpha) = WelcomeVideoOverlayStyle.scrimAlphas()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .testTag("welcome-screen"),
    ) {
        WelcomeVideoBackdrop()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("welcome-native-atmosphere")
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AetherColors.Surface.copy(alpha = surfaceScrimAlpha),
                            AetherColors.Primary.copy(alpha = primaryScrimAlpha),
                            AetherColors.OnSurface.copy(alpha = onSurfaceScrimAlpha),
                        ),
                    ),
                ),
        ) {
            WelcomeAtmosphereAccentCatalog.visibleAccentSlots().forEach { accentSlot ->
                when (accentSlot) {
                    WelcomeAtmosphereAccentSlot.TopStartOrb -> AtmosphereAccent(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 20.dp, top = 88.dp),
                        backgroundColor = AetherColors.Surface.copy(alpha = 0.12f),
                        horizontalPadding = 26.dp,
                        verticalPadding = 26.dp,
                    )
                    WelcomeAtmosphereAccentSlot.CenterEndOrb -> AtmosphereAccent(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 28.dp),
                        backgroundColor = AetherColors.PrimaryContainer.copy(alpha = 0.18f),
                        horizontalPadding = 56.dp,
                        verticalPadding = 56.dp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.testTag("welcome-hero-content"),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = appLanguage.pick("Next-Gen IM Protocol", "新一代 IM 协议"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.Surface.copy(alpha = 0.86f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(AetherColors.Surface.copy(alpha = 0.14f))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
                Text(
                    text = "GKIM",
                    style = MaterialTheme.typography.displayLarge,
                    color = AetherColors.Surface,
                )
                Text(
                    text = appLanguage.pick("Welcome to GKIM", "欢迎来到 GKIM"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AetherColors.Surface.copy(alpha = 0.92f),
                )
                Text(
                    text = appLanguage.pick(
                        "Chat with friends, share ideas, and keep everyday conversations in one place.",
                        "和朋友聊天、分享灵感，把常用的沟通都放在一个地方。",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AetherColors.Surface.copy(alpha = 0.76f),
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AetherColors.Surface.copy(alpha = 0f),
                                    AetherColors.Primary.copy(alpha = 0.85f),
                                    AetherColors.Surface.copy(alpha = 0f),
                                ),
                            ),
                        ),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    WelcomeGhostButton(
                        label = "注册",
                        testTag = "welcome-register-button",
                        modifier = Modifier.weight(1f),
                        onClick = onRegister,
                    )
                    WelcomePrimaryButton(
                        label = "登录",
                        testTag = "welcome-login-button",
                        modifier = Modifier.weight(1f),
                        onClick = onLogin,
                    )
                }
                Text(
                    text = appLanguage.pick("V 2.0.4", "V 2.0.4"),
                    style = MaterialTheme.typography.labelMedium,
                    color = AetherColors.Surface.copy(alpha = 0.54f),
                )
            }
        }
    }
}

@Composable
private fun AtmosphereAccent(
    modifier: Modifier,
    backgroundColor: Color,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    )
}

internal enum class WelcomeAtmosphereAccentSlot {
    TopStartOrb,
    CenterEndOrb,
}

internal object WelcomeAtmosphereAccentCatalog {
    fun visibleAccentSlots(): List<WelcomeAtmosphereAccentSlot> = listOf(
        WelcomeAtmosphereAccentSlot.TopStartOrb,
        WelcomeAtmosphereAccentSlot.CenterEndOrb,
    )
}

internal object WelcomeVideoOverlayStyle {
    fun scrimAlphas(): List<Float> = listOf(0.03f, 0.09f, 0.36f)
}

@Composable
private fun WelcomeGhostButton(
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = AetherColors.Surface,
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(999.dp))
            .background(AetherColors.Surface.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    )
}

@Composable
private fun WelcomePrimaryButton(
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = AetherColors.Surface,
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(999.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AetherColors.Primary, AetherColors.PrimaryContainer),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    )
}
