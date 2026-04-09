package com.gkim.im.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AetherColors.SurfaceContainerLow.copy(alpha = 0.88f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun PageHeader(
    eyebrow: String? = null,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    leadingLabel: String? = null,
    onLeading: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if ((leadingLabel != null && onLeading != null) || !eyebrow.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (leadingLabel != null && onLeading != null) {
                    PillAction(label = leadingLabel, onClick = onLeading)
                }
                if (!eyebrow.isNullOrBlank()) {
                    Text(text = eyebrow.uppercase(), style = MaterialTheme.typography.labelLarge, color = AetherColors.Primary)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge, color = AetherColors.OnSurface, modifier = Modifier.weight(1f))
            if (actionLabel != null && onAction != null) {
                PillAction(label = actionLabel, onClick = onAction)
            }
        }
        if (!description.isNullOrBlank()) {
            Text(text = description, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
        }
    }
}

@Composable
fun PillAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = AetherColors.OnSurface,
        modifier = Modifier
            .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun GradientPrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleLarge,
        color = AetherColors.Surface,
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(AetherColors.Primary, AetherColors.PrimaryContainer)),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    )
}

