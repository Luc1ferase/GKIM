package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMenuPresentationTest {

    @Test
    fun `menu includes world info entry routing to WorldInfo destination`() {
        val items = buildSettingsMenuItems(SettingsUiState())

        val worldInfo = items.firstOrNull { it.testTag == "settings-menu-worldinfo" }
        assertNotNull("world info entry must be present", worldInfo)
        assertEquals(SettingsDestination.WorldInfo, worldInfo!!.destination)
        assertEquals("World Info", worldInfo.englishLabel)
        assertEquals("世界信息", worldInfo.chineseLabel)
        assertEquals("Manage lorebooks bound to companion characters.", worldInfo.englishSummary)
        assertEquals("管理绑定到伙伴角色的世界书。", worldInfo.chineseSummary)
    }

    @Test
    fun `menu preserves base entries alongside world info`() {
        val items = buildSettingsMenuItems(SettingsUiState())
        val tags = items.map { it.testTag }

        assertTrue(tags.contains("settings-menu-appearance"))
        assertTrue(tags.contains("settings-menu-ai-provider"))
        assertTrue(tags.contains("settings-menu-im-validation"))
        assertTrue(tags.contains("settings-menu-personas"))
        assertTrue(tags.contains("settings-menu-worldinfo"))
        assertTrue(tags.contains("settings-menu-account"))
    }

    @Test
    fun `world info entry sits between personas and account for Companion grouping`() {
        val items = buildSettingsMenuItems(SettingsUiState())
        val tags = items.map { it.testTag }
        val personasIndex = tags.indexOf("settings-menu-personas")
        val worldInfoIndex = tags.indexOf("settings-menu-worldinfo")
        val accountIndex = tags.indexOf("settings-menu-account")

        assertTrue("personas must appear", personasIndex >= 0)
        assertTrue("world info must appear after personas", worldInfoIndex > personasIndex)
        assertTrue("account must appear after world info", accountIndex > worldInfoIndex)
    }

    @Test
    fun `destination enum is usable from tests`() {
        val destination = SettingsDestination.WorldInfo
        assertEquals("WorldInfo", destination.name)
    }

    @Test
    fun `provider summary reflects active provider and falls back cleanly`() {
        val provider = AigcProvider(
            id = "preset-hunyuan",
            label = "Hunyuan",
            vendor = "preset",
            description = "",
            model = "hunyuan-turbo",
            accent = AccentTone.Primary,
            preset = true,
            capabilities = setOf(AigcMode.TextToImage),
        )
        val activeState = SettingsUiState(
            providers = listOf(provider),
            activeProviderId = provider.id,
        )
        val activeItems = buildSettingsMenuItems(activeState)
        val providerItem = activeItems.first { it.testTag == "settings-menu-ai-provider" }
        assertEquals("Hunyuan · hunyuan-turbo", providerItem.englishSummary)
        assertEquals("Hunyuan · hunyuan-turbo", providerItem.chineseSummary)

        val emptyItems = buildSettingsMenuItems(SettingsUiState())
        val emptyProviderItem = emptyItems.first { it.testTag == "settings-menu-ai-provider" }
        assertEquals("Choose a provider", emptyProviderItem.englishSummary)
        assertEquals("选择提供商", emptyProviderItem.chineseSummary)
    }

    @Test
    fun `connection summary surfaces validation error when present`() {
        val withError = SettingsUiState(
            imResolvedBackendOrigin = "https://api.example.com/",
            imValidationError = "Origin invalid",
        )
        val errorItems = buildSettingsMenuItems(withError)
        val validationWithError = errorItems.first { it.testTag == "settings-menu-im-validation" }
        assertEquals("Origin invalid", validationWithError.englishSummary)
        assertEquals("Origin invalid", validationWithError.chineseSummary)

        val healthy = SettingsUiState(imResolvedBackendOrigin = "https://api.example.com/")
        val healthyItems = buildSettingsMenuItems(healthy)
        val validationHealthy = healthyItems.first { it.testTag == "settings-menu-im-validation" }
        assertEquals("Backend https://api.example.com", validationHealthy.englishSummary)
        assertEquals("后端 https://api.example.com", validationHealthy.chineseSummary)
        assertNull("sanity", healthy.imValidationError)
    }

    @Test
    fun `aigc image provider item uses renamed bilingual label`() {
        val item = buildSettingsMenuItems(SettingsUiState())
            .first { it.testTag == "settings-menu-ai-provider" }
        assertEquals("AIGC Image Provider", item.englishLabel)
        assertEquals("AIGC 图像提供商", item.chineseLabel)
    }

    @Test
    fun `developer connection item uses renamed bilingual label`() {
        val item = buildSettingsMenuItems(SettingsUiState())
            .first { it.testTag == "settings-menu-im-validation" }
        assertEquals("Connection & Developer Tools", item.englishLabel)
        assertEquals("连接与开发者工具", item.chineseLabel)
    }

    @Test
    fun `persona and preset items use library bilingual labels`() {
        val items = buildSettingsMenuItems(SettingsUiState())
        val personas = items.first { it.testTag == "settings-menu-personas" }
        assertEquals("Persona library", personas.englishLabel)
        assertEquals("用户角色库", personas.chineseLabel)
        val presets = items.first { it.testTag == "settings-menu-presets" }
        assertEquals("Preset library", presets.englishLabel)
        assertEquals("预设库", presets.chineseLabel)
    }

    @Test
    fun `sections render in the six-section order when debug build is true`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val ids = sections.map { it.id }
        assertEquals(
            listOf(
                SettingsSectionId.Companion,
                SettingsSectionId.Appearance,
                SettingsSectionId.ContentSafety,
                SettingsSectionId.AigcImageProvider,
                SettingsSectionId.DeveloperConnection,
                SettingsSectionId.Account,
            ),
            ids,
        )
    }

    @Test
    fun `developer and connection section is omitted in release builds`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = false)
        val ids = sections.map { it.id }
        assertEquals(
            listOf(
                SettingsSectionId.Companion,
                SettingsSectionId.Appearance,
                SettingsSectionId.ContentSafety,
                SettingsSectionId.AigcImageProvider,
                SettingsSectionId.Account,
            ),
            ids,
        )
    }

    @Test
    fun `companion section contains persona library preset library and world info`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val companion = sections.first { it.id == SettingsSectionId.Companion }
        val destinations = companion.items.map { it.destination }
        assertEquals(
            listOf(
                SettingsDestination.Personas,
                SettingsDestination.Presets,
                SettingsDestination.WorldInfo,
            ),
            destinations,
        )
    }

    @Test
    fun `appearance section contains only the appearance item`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val appearance = sections.first { it.id == SettingsSectionId.Appearance }
        assertEquals(
            listOf(SettingsDestination.Appearance),
            appearance.items.map { it.destination },
        )
    }

    @Test
    fun `content and safety section is structurally present with zero items in this slice`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val safety = sections.first { it.id == SettingsSectionId.ContentSafety }
        assertEquals(0, safety.items.size)
    }

    @Test
    fun `aigc image provider section groups only the provider destination`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val aigc = sections.first { it.id == SettingsSectionId.AigcImageProvider }
        assertEquals(
            listOf(SettingsDestination.AiProvider),
            aigc.items.map { it.destination },
        )
    }

    @Test
    fun `developer and connection section groups only the im validation destination`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val developer = sections.first { it.id == SettingsSectionId.DeveloperConnection }
        assertEquals(
            listOf(SettingsDestination.ImValidation),
            developer.items.map { it.destination },
        )
    }

    @Test
    fun `account section groups only the account destination`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val account = sections.first { it.id == SettingsSectionId.Account }
        assertEquals(
            listOf(SettingsDestination.Account),
            account.items.map { it.destination },
        )
    }

    @Test
    fun `section labels render in both english and chinese`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        val byId = sections.associateBy { it.id }

        val companion = byId.getValue(SettingsSectionId.Companion)
        assertEquals("Companion", companion.englishLabel)
        assertEquals("陪伴", companion.chineseLabel)

        val appearance = byId.getValue(SettingsSectionId.Appearance)
        assertEquals("Appearance", appearance.englishLabel)
        assertEquals("外观", appearance.chineseLabel)

        val safety = byId.getValue(SettingsSectionId.ContentSafety)
        assertEquals("Content & Safety", safety.englishLabel)
        assertEquals("内容与安全", safety.chineseLabel)

        val aigc = byId.getValue(SettingsSectionId.AigcImageProvider)
        assertEquals("AIGC Image Provider", aigc.englishLabel)
        assertEquals("AIGC 图像提供商", aigc.chineseLabel)

        val developer = byId.getValue(SettingsSectionId.DeveloperConnection)
        assertEquals("Developer & Connection", developer.englishLabel)
        assertEquals("开发者与连接", developer.chineseLabel)

        val account = byId.getValue(SettingsSectionId.Account)
        assertEquals("Account", account.englishLabel)
        assertEquals("账号", account.chineseLabel)
    }

    @Test
    fun `aigc image provider section caption scopes to image generation only`() {
        val section = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.AigcImageProvider }
        val englishCaption = section.englishCaption
        assertNotNull("english caption required to scope to image generation", englishCaption)
        assertTrue(
            "english caption must mention image generation",
            englishCaption!!.contains("image", ignoreCase = true),
        )
        assertTrue(
            "english caption must disambiguate from companion chat",
            englishCaption.contains("companion", ignoreCase = true),
        )
        val chineseCaption = section.chineseCaption
        assertNotNull("chinese caption required to scope to image generation", chineseCaption)
        assertTrue(
            "chinese caption must mention image generation",
            chineseCaption!!.contains("图像"),
        )
        assertTrue(
            "chinese caption must disambiguate from companion chat",
            chineseCaption.contains("陪伴"),
        )
    }

    @Test
    fun `companion section has a caption describing its grouping`() {
        val section = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.Companion }
        assertNotNull(section.englishCaption)
        assertNotNull(section.chineseCaption)
    }

    @Test
    fun `developer connection section has a caption describing its scope`() {
        val section = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.DeveloperConnection }
        assertNotNull(section.englishCaption)
        assertNotNull(section.chineseCaption)
    }

    @Test
    fun `content and safety section has a caption describing its scope`() {
        val section = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
            .first { it.id == SettingsSectionId.ContentSafety }
        assertNotNull(section.englishCaption)
        assertNotNull(section.chineseCaption)
    }

    @Test
    fun `section test tags use settings-section prefix`() {
        val sections = buildSettingsMenuSections(SettingsUiState(), isDebugBuild = true)
        sections.forEach { section ->
            assertTrue(
                "section ${section.id} must have settings-section- prefix on its tag",
                section.testTag.startsWith("settings-section-"),
            )
        }
    }
}
