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
}
