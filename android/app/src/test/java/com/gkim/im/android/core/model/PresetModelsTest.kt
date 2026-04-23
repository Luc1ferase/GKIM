package com.gkim.im.android.core.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetModelsTest {

    private val roleplayImmersive = Preset(
        id = "preset-roleplay",
        displayName = LocalizedText("Roleplay Immersive", "沉浸角色扮演"),
        description = LocalizedText("Deep roleplay continuity.", "沉浸角色扮演预设。"),
        template = PresetTemplate(
            systemPrefix = LocalizedText("Stay in character.", "保持角色。"),
            systemSuffix = LocalizedText("Never break the fourth wall.", "不要跳戏。"),
            formatInstructions = LocalizedText("Prefer prose over meta commentary.", "少用旁白。"),
            postHistoryInstructions = LocalizedText(
                english = "Continue the scene with emotional continuity.",
                chinese = "延续场景的情绪连贯性。",
            ),
        ),
        params = PresetParams(temperature = 0.9, topP = 0.95, maxReplyTokens = 1024),
        isBuiltIn = true,
        isActive = true,
        createdAt = 1_700_000_000L,
        updatedAt = 1_700_000_500L,
        extensions = buildJsonObject {
            put("st", buildJsonObject { put("legacy", "passthrough") })
            put("impersonation", "off")
        },
    )

    @Test
    fun presetRequiresIdDisplayNameAndProvidesDefaultsForTheRest() {
        val minimal = Preset(
            id = "preset-minimal",
            displayName = LocalizedText("Minimal", "极简"),
        )
        assertEquals(LocalizedText.Empty, minimal.description)
        assertEquals(PresetTemplate(), minimal.template)
        assertEquals(PresetParams(), minimal.params)
        assertFalse(minimal.isBuiltIn)
        assertFalse(minimal.isActive)
        assertEquals(0L, minimal.createdAt)
        assertEquals(0L, minimal.updatedAt)
        assertEquals(JsonObject(emptyMap()), minimal.extensions)
    }

    @Test
    fun presetEqualityConsidersEveryField() {
        val twin = roleplayImmersive.copy()
        assertEquals(roleplayImmersive, twin)

        assertNotEquals(roleplayImmersive, roleplayImmersive.copy(id = "preset-other"))
        assertNotEquals(
            roleplayImmersive,
            roleplayImmersive.copy(displayName = LocalizedText("Other", "其他")),
        )
        assertNotEquals(
            roleplayImmersive,
            roleplayImmersive.copy(description = LocalizedText.Empty),
        )
        assertNotEquals(
            roleplayImmersive,
            roleplayImmersive.copy(template = PresetTemplate()),
        )
        assertNotEquals(
            roleplayImmersive,
            roleplayImmersive.copy(params = PresetParams(temperature = 0.5)),
        )
        assertNotEquals(roleplayImmersive, roleplayImmersive.copy(isBuiltIn = false))
        assertNotEquals(roleplayImmersive, roleplayImmersive.copy(isActive = false))
        assertNotEquals(roleplayImmersive, roleplayImmersive.copy(createdAt = 1L))
        assertNotEquals(roleplayImmersive, roleplayImmersive.copy(updatedAt = 1L))
    }

    @Test
    fun presetTemplateDefaultsMatchLocalizedTextEmpty() {
        val template = PresetTemplate()
        assertEquals(LocalizedText.Empty, template.systemPrefix)
        assertEquals(LocalizedText.Empty, template.systemSuffix)
        assertEquals(LocalizedText.Empty, template.formatInstructions)
        assertEquals(LocalizedText.Empty, template.postHistoryInstructions)
    }

    @Test
    fun presetTemplateEqualityConsidersEverySlot() {
        val base = PresetTemplate(
            systemPrefix = LocalizedText.of("prefix"),
            systemSuffix = LocalizedText.of("suffix"),
            formatInstructions = LocalizedText.of("format"),
            postHistoryInstructions = LocalizedText.of("post"),
        )
        assertEquals(base, base.copy())
        assertNotEquals(base, base.copy(systemPrefix = LocalizedText.Empty))
        assertNotEquals(base, base.copy(systemSuffix = LocalizedText.Empty))
        assertNotEquals(base, base.copy(formatInstructions = LocalizedText.Empty))
        assertNotEquals(base, base.copy(postHistoryInstructions = LocalizedText.Empty))
    }

    @Test
    fun presetParamsDefaultToNullForProviderDefault() {
        val defaults = PresetParams()
        assertNull(defaults.temperature)
        assertNull(defaults.topP)
        assertNull(defaults.maxReplyTokens)
    }

    @Test
    fun presetParamsAcceptExplicitValues() {
        val tuned = PresetParams(temperature = 0.7, topP = 0.9, maxReplyTokens = 512)
        assertEquals(0.7, tuned.temperature!!, 1e-9)
        assertEquals(0.9, tuned.topP!!, 1e-9)
        assertEquals(512, tuned.maxReplyTokens)
    }

    @Test
    fun presetExtensionsBagSurvivesCopyAndExposesUnknownKeys() {
        val original = roleplayImmersive.extensions
        val renamed = roleplayImmersive.copy(
            displayName = LocalizedText("Renamed", "重命名"),
        )
        assertEquals(original, renamed.extensions)
        assertEquals(JsonPrimitive("off"), renamed.extensions["impersonation"])
        assertTrue(
            "forward-compat bag preserves nested ST payload",
            renamed.extensions["st"] is JsonObject,
        )
    }

    @Test
    fun isDeletableRequiresUserOwnedAndInactive() {
        assertFalse("active built-in is not deletable", roleplayImmersive.isDeletable)
        assertFalse(
            "built-in preset cannot be deleted even when inactive",
            roleplayImmersive.copy(isActive = false).isDeletable,
        )
        assertFalse(
            "active user preset cannot be deleted",
            roleplayImmersive.copy(isBuiltIn = false).isDeletable,
        )
        assertTrue(
            "inactive user-owned preset is deletable",
            roleplayImmersive.copy(isBuiltIn = false, isActive = false).isDeletable,
        )
    }
}
