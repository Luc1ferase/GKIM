package com.gkim.im.android.core.rendering

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.model.RichBlock
import com.gkim.im.android.core.model.RichDocument

@Composable
fun RichContentRenderer(document: RichDocument, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        document.blocks.forEach { block ->
            when (block) {
                is RichBlock.Heading -> Text(
                    text = block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineLarge
                        2 -> MaterialTheme.typography.headlineMedium
                        else -> MaterialTheme.typography.titleLarge
                    }
                )

                is RichBlock.Paragraph -> Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AetherColors.OnSurfaceVariant
                )

                is RichBlock.Quote -> Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    color = AetherColors.OnSurface
                )

                is RichBlock.Code -> Text(
                    text = block.code,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AetherColors.SurfaceLowest, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    color = AetherColors.Primary
                )

                is RichBlock.BulletList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    block.items.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AetherColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
