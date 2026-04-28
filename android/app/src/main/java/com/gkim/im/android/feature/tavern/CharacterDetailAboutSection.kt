package com.gkim.im.android.feature.tavern

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.CompanionCharacterCard
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Data shape backing the "About this card" sub-section on the character detail surface.
 *
 * All fields are nullable: a null value means the underlying source was missing or blank, and
 * the corresponding UI row is hidden. This keeps the pure-data layer trivially testable — a
 * unit test can drive [aboutCardData] with fixtures and assert exact field presence without
 * standing up a Compose tree.
 */
internal data class AboutCardData(
    val creator: String? = null,
    val creatorNotes: String? = null,
    val characterVersion: String? = null,
    val stSource: String? = null,
    val stCreationDate: String? = null,
    val stModificationDate: String? = null,
) {
    val isEmpty: Boolean
        get() = creator == null &&
            creatorNotes == null &&
            characterVersion == null &&
            stSource == null &&
            stCreationDate == null &&
            stModificationDate == null
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/**
 * Derives [AboutCardData] from a [CompanionCharacterCard].
 *
 * Top-level card fields (`creator`, `creatorNotes`, `characterVersion`) are blank-filtered.
 * The three ST-extension fields are read from `extensions.st.<key>`; numeric dates are
 * formatted as ISO yyyy-MM-dd in the system timezone, string dates pass through unchanged,
 * and unexpected shapes produce null (hidden) rather than a crash.
 */
internal fun aboutCardData(card: CompanionCharacterCard): AboutCardData {
    val st = card.extensions["st"] as? JsonObject

    fun stString(key: String): String? {
        val prim = st?.get(key) as? JsonPrimitive ?: return null
        return prim.content.takeUnless { it.isBlank() }
    }

    return AboutCardData(
        creator = card.creator.takeUnless { it.isBlank() },
        creatorNotes = card.creatorNotes.takeUnless { it.isBlank() },
        characterVersion = card.characterVersion.takeUnless { it.isBlank() },
        stSource = stString("stSource"),
        stCreationDate = stString("stCreationDate")?.let(::formatStDate),
        stModificationDate = stString("stModificationDate")?.let(::formatStDate),
    )
}

/**
 * Format an ST card date field.
 *
 * SillyTavern cards store creation/modification dates as epoch seconds (integers) in most
 * V2/V3 exports, but hand-authored cards sometimes carry ISO strings instead. We accept both:
 * numeric → epoch seconds → ISO local date; non-numeric → passed through as the author wrote it.
 */
internal fun formatStDate(raw: String): String {
    val epochSeconds = raw.toLongOrNull() ?: return raw
    val instant = if (epochSeconds > 10_000_000_000L) {
        Instant.ofEpochMilli(epochSeconds)
    } else {
        Instant.ofEpochSecond(epochSeconds)
    }
    return instant.atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT)
}

@Composable
internal fun AboutCardSection(
    card: CompanionCharacterCard,
    onOpenStSource: (String) -> Unit = defaultOpenStSource(LocalContext.current),
) {
    val data = aboutCardData(card)
    if (data.isEmpty) return

    val appLanguage = LocalAppLanguage.current
    GlassCard(modifier = Modifier.testTag("character-detail-about")) {
        Text(
            text = appLanguage.pick("About this card", "关于此卡"),
            style = MaterialTheme.typography.titleMedium,
            color = AetherColors.OnSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            data.creator?.let { value ->
                AboutRow(label = appLanguage.pick("Creator", "作者"), value = value, tag = "character-detail-about-creator")
            }
            data.characterVersion?.let { value ->
                AboutRow(label = appLanguage.pick("Version", "版本"), value = value, tag = "character-detail-about-version")
            }
            data.creatorNotes?.let { value ->
                AboutRow(label = appLanguage.pick("Notes", "备注"), value = value, tag = "character-detail-about-notes")
            }
            data.stCreationDate?.let { value ->
                AboutRow(label = appLanguage.pick("Created", "创建日期"), value = value, tag = "character-detail-about-created")
            }
            data.stModificationDate?.let { value ->
                AboutRow(label = appLanguage.pick("Modified", "修改日期"), value = value, tag = "character-detail-about-modified")
            }
            data.stSource?.let { value ->
                val label = appLanguage.pick("Source", "来源")
                Text(
                    text = "$label: $value",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.Primary,
                    modifier = Modifier
                        .clickable { onOpenStSource(value) }
                        .padding(vertical = 2.dp)
                        .testTag("character-detail-about-source"),
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String, tag: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = AetherColors.OnSurfaceVariant,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .testTag(tag),
    )
}

internal fun defaultOpenStSource(context: Context): (String) -> Unit = { url ->
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
