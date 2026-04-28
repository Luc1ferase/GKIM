package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AigcImageProviderSectionTest {

    private fun provider(
        id: String,
        label: String = "Hunyuan",
        model: String = "hunyuan-turbo",
        vendor: String = "preset",
    ) = AigcProvider(
        id = id,
        label = label,
        vendor = vendor,
        description = "",
        model = model,
        accent = AccentTone.Primary,
        preset = true,
        capabilities = setOf(AigcMode.TextToImage),
    )

    @Test
    fun `section uses bilingual label AIGC Image Provider`() {
        val section = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
        assertEquals("AIGC Image Provider", section.englishLabel)
        assertEquals("AIGC 图像提供商", section.chineseLabel)
    }

    @Test
    fun `section uses test tag settings-section-aigc-image-provider`() {
        val section = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
        assertEquals("settings-section-aigc-image-provider", section.testTag)
    }

    @Test
    fun `english caption scopes to image generation and disambiguates from companion chat`() {
        val caption = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
            .englishCaption
        assertNotNull("english caption required", caption)
        assertTrue(
            "english caption must mention image generation",
            caption!!.contains("image", ignoreCase = true),
        )
        assertTrue(
            "english caption must mention companion to disambiguate from chat",
            caption.contains("companion", ignoreCase = true),
        )
    }

    @Test
    fun `chinese caption scopes to image generation and disambiguates from companion chat`() {
        val caption = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
            .chineseCaption
        assertNotNull("chinese caption required", caption)
        assertTrue(
            "chinese caption must mention 图像 (image)",
            caption!!.contains("图像"),
        )
        assertTrue(
            "chinese caption must mention 陪伴 (companion)",
            caption.contains("陪伴"),
        )
    }

    @Test
    fun `section contains exactly the AiProvider menu item`() {
        val section = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
        assertEquals(
            listOf(SettingsDestination.AiProvider),
            section.items.map { it.destination },
        )
    }

    @Test
    fun `provider menu item keeps the AIGC Image Provider bilingual labels`() {
        val item = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
            .items
            .single()
        assertEquals("AIGC Image Provider", item.englishLabel)
        assertEquals("AIGC 图像提供商", item.chineseLabel)
    }

    @Test
    fun `provider summary reflects active provider label and model when one is selected`() {
        val hunyuan = provider(id = "preset-hunyuan", label = "Hunyuan", model = "hunyuan-turbo")
        val uiState = SettingsUiState(
            providers = listOf(hunyuan),
            activeProviderId = hunyuan.id,
        )
        val item = buildSettingsMenuSections(uiState, isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
            .items
            .single()
        assertEquals("Hunyuan · hunyuan-turbo", item.englishSummary)
        assertEquals("Hunyuan · hunyuan-turbo", item.chineseSummary)
    }

    @Test
    fun `provider summary falls back when no provider is selected`() {
        val item = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
            .items
            .single()
        assertEquals("Choose a provider", item.englishSummary)
        assertEquals("选择提供商", item.chineseSummary)
    }

    @Test
    fun `section is present in both debug and release builds`() {
        val debugSections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val releaseSections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = false)
        assertTrue(
            "AIGC image provider must be present in debug",
            debugSections.any { it.id == SettingsSectionId.AigcImageProvider },
        )
        assertTrue(
            "AIGC image provider must be present in release",
            releaseSections.any { it.id == SettingsSectionId.AigcImageProvider },
        )
    }

    @Test
    fun `section caption is not the companion section caption`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val aigc = sections.first { it.id == SettingsSectionId.AigcImageProvider }
        val companion = sections.first { it.id == SettingsSectionId.Companion }
        assertFalse(
            "AIGC caption must not collide with companion caption",
            aigc.englishCaption == companion.englishCaption,
        )
    }

    @Test
    fun `section caption does not lean on chat-oriented vocabulary`() {
        val caption = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
            .englishCaption!!
        assertTrue(
            "caption must scope to image generation",
            caption.contains("image", ignoreCase = true) || caption.contains("generation", ignoreCase = true),
        )
    }

    @Test
    fun `provider selection logic is unchanged by the caption rewrite`() {
        val hunyuan = provider(id = "preset-hunyuan", label = "Hunyuan", model = "hunyuan-turbo")
        val custom = provider(id = "custom", label = "Custom", model = "gpt-image-1", vendor = "custom")
        val uiState = SettingsUiState(
            providers = listOf(hunyuan, custom),
            activeProviderId = hunyuan.id,
        )
        val flatItem = buildSettingsMenuItems(uiState).first { it.testTag == "settings-menu-ai-provider" }
        val sectionItem = buildSettingsMenuSections(uiState, isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
            .items
            .single()
        assertEquals(
            "flat item and section item must resolve to the same destination",
            flatItem.destination,
            sectionItem.destination,
        )
        assertEquals(
            "flat item and section item must carry the same english summary",
            flatItem.englishSummary,
            sectionItem.englishSummary,
        )
    }
}
