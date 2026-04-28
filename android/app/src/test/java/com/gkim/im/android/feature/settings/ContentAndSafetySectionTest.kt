package com.gkim.im.android.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ContentAndSafetySectionTest {

    @Test
    fun `content and safety section ships exactly ack status and verbosity rows in that order`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val safety = sections.first { it.id == SettingsSectionId.ContentSafety }
        assertEquals(
            listOf(
                SettingsDestination.ContentPolicy,
                SettingsDestination.ContentSafety,
            ),
            safety.items.map { it.destination },
        )
    }

    @Test
    fun `ack status row uses bilingual labels and routes to ContentPolicy`() {
        val item = buildSettingsMenuItems(SettingsUiState())
            .first { it.testTag == "settings-menu-content-policy" }
        assertEquals(SettingsDestination.ContentPolicy, item.destination)
        assertEquals("Acknowledgment status", item.englishLabel)
        assertEquals("内容政策确认", item.chineseLabel)
    }

    @Test
    fun `verbosity row uses bilingual labels and routes to ContentSafety`() {
        val item = buildSettingsMenuItems(SettingsUiState())
            .first { it.testTag == "settings-menu-block-reason-verbosity" }
        assertEquals(SettingsDestination.ContentSafety, item.destination)
        assertEquals("Block reason verbosity", item.englishLabel)
        assertEquals("屏蔽原因详细度", item.chineseLabel)
    }

    @Test
    fun `ack status summary reports not accepted when acknowledged at is null`() {
        val uiState = SettingsUiState(contentPolicyAcknowledgedAtMillis = null)
        val item = buildSettingsMenuItems(uiState).first { it.testTag == "settings-menu-content-policy" }
        assertEquals("Not accepted — read policy", item.englishSummary)
        assertEquals("未确认 — 请阅读政策", item.chineseSummary)
    }

    @Test
    fun `ack status summary reports acceptance date when acknowledged at is set`() {
        val acceptedAt = LocalDate.of(2026, 4, 23)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val uiState = SettingsUiState(contentPolicyAcknowledgedAtMillis = acceptedAt)
        val item = buildSettingsMenuItems(uiState).first { it.testTag == "settings-menu-content-policy" }
        assertEquals("Accepted on 2026-04-23", item.englishSummary)
        assertEquals("2026-04-23 已确认", item.chineseSummary)
    }

    @Test
    fun `verbosity summary reflects On state bilingually`() {
        val uiState = SettingsUiState(blockReasonVerbosity = true)
        val item = buildSettingsMenuItems(uiState).first { it.testTag == "settings-menu-block-reason-verbosity" }
        assertEquals("On", item.englishSummary)
        assertEquals("开", item.chineseSummary)
    }

    @Test
    fun `verbosity summary reflects Off state bilingually`() {
        val uiState = SettingsUiState(blockReasonVerbosity = false)
        val item = buildSettingsMenuItems(uiState).first { it.testTag == "settings-menu-block-reason-verbosity" }
        assertEquals("Off", item.englishSummary)
        assertEquals("关", item.chineseSummary)
    }

    @Test
    fun `ui state defaults block reason verbosity to on`() {
        assertTrue(SettingsUiState().blockReasonVerbosity)
    }

    @Test
    fun `ui state defaults acknowledgment to not accepted`() {
        val state = SettingsUiState()
        assertEquals(null, state.contentPolicyAcknowledgedAtMillis)
        assertEquals("", state.contentPolicyAcknowledgedVersion)
    }

    @Test
    fun `formatAcknowledgmentEnglishSummary handles null as Not accepted hint`() {
        assertEquals("Not accepted — read policy", formatAcknowledgmentEnglishSummary(null))
    }

    @Test
    fun `formatAcknowledgmentChineseSummary handles null as not accepted hint`() {
        assertEquals("未确认 — 请阅读政策", formatAcknowledgmentChineseSummary(null))
    }

    @Test
    fun `formatAcknowledgment formats iso local date stably across both languages`() {
        val millis = LocalDate.of(2025, 1, 3)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals("Accepted on 2025-01-03", formatAcknowledgmentEnglishSummary(millis))
        assertEquals("2025-01-03 已确认", formatAcknowledgmentChineseSummary(millis))
    }

    @Test
    fun `ack status row sits before verbosity row inside the content and safety section`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val safety = sections.first { it.id == SettingsSectionId.ContentSafety }
        val ackIndex = safety.items.indexOfFirst { it.testTag == "settings-menu-content-policy" }
        val verbosityIndex = safety.items.indexOfFirst { it.testTag == "settings-menu-block-reason-verbosity" }
        assertTrue("ack status must be present", ackIndex >= 0)
        assertTrue("verbosity row must be present", verbosityIndex >= 0)
        assertTrue("ack status must come first", ackIndex < verbosityIndex)
    }

    @Test
    fun `content safety menu items are present in debug and release builds`() {
        val debug = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.ContentSafety }
        val release = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = false)
            .first { it.id == SettingsSectionId.ContentSafety }
        assertEquals(
            listOf(SettingsDestination.ContentPolicy, SettingsDestination.ContentSafety),
            debug.items.map { it.destination },
        )
        assertEquals(
            listOf(SettingsDestination.ContentPolicy, SettingsDestination.ContentSafety),
            release.items.map { it.destination },
        )
    }

    @Test
    fun `acknowledgment summary distinguishes different accepted dates`() {
        val instantA = Instant.parse("2025-06-15T10:00:00Z").toEpochMilli()
        val instantB = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()
        val summaryA = formatAcknowledgmentEnglishSummary(instantA)
        val summaryB = formatAcknowledgmentEnglishSummary(instantB)
        assertFalse("distinct millis must produce distinct summaries", summaryA == summaryB)
    }

    @Test
    fun `content policy destination is distinct from content safety destination`() {
        assertFalse(SettingsDestination.ContentPolicy == SettingsDestination.ContentSafety)
        assertEquals("ContentPolicy", SettingsDestination.ContentPolicy.name)
        assertEquals("ContentSafety", SettingsDestination.ContentSafety.name)
    }
}
