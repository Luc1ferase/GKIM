package com.gkim.im.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.resolve

data class CompanionGreetingOption(
    val index: Int,
    val label: String,
    val body: String,
)

internal fun resolveCompanionGreetings(
    card: CompanionCharacterCard?,
    language: AppLanguage,
): List<CompanionGreetingOption> {
    if (card == null) return emptyList()
    val resolved = card.resolve(language)
    val options = mutableListOf<CompanionGreetingOption>()
    if (resolved.firstMes.isNotBlank()) {
        options += CompanionGreetingOption(
            index = options.size,
            label = "Greeting",
            body = resolved.firstMes,
        )
    }
    resolved.alternateGreetings.forEachIndexed { i, greeting ->
        if (greeting.isNotBlank()) {
            options += CompanionGreetingOption(
                index = options.size,
                label = "Alt ${i + 1}",
                body = greeting,
            )
        }
    }
    return options
}

internal fun shouldShowGreetingPicker(
    companionPathIsEmpty: Boolean,
    options: List<CompanionGreetingOption>,
): Boolean = companionPathIsEmpty && options.isNotEmpty()

@Composable
fun CompanionGreetingPicker(
    options: List<CompanionGreetingOption>,
    onSelect: (CompanionGreetingOption) -> Unit,
) {
    if (options.isEmpty()) return
    GlassCard(modifier = Modifier.testTag("chat-companion-greeting-picker")) {
        Text(
            text = "Pick an opening line",
            style = MaterialTheme.typography.labelLarge,
            color = AetherColors.Primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AetherColors.SurfaceContainerHigh, RoundedCornerShape(18.dp))
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("chat-companion-greeting-option-${option.index}"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = AetherColors.OnSurfaceVariant,
                    )
                    Text(
                        text = option.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AetherColors.OnSurface,
                    )
                }
            }
        }
    }
}
