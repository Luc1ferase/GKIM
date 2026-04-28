package com.gkim.im.android.core.designsystem

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AetherFontsAssetsTest {

    @Test
    fun `every declared font asset file exists in res font dir`() {
        val fontDir = File("src/main/res/font").absoluteFile
        AetherFontAssets.forEach { asset ->
            val file = File(fontDir, asset.fileName)
            assertTrue("Missing font file: ${asset.fileName} (looked under ${fontDir})", file.exists())
            assertTrue("Empty font file: ${asset.fileName} (size=${file.length()})", file.length() > 1024L)
        }
    }

    @Test
    fun `every declared font asset has a non-zero resource id`() {
        AetherFontAssets.forEach { asset ->
            assertTrue("Resource id zero for ${asset.fileName}", asset.resId != 0)
        }
    }

    @Test
    fun `display serif covers regular semibold bold weights`() {
        val weights = AetherFontAssets
            .filter { it.family == AetherFontFamilyId.DisplaySerif }
            .map { it.weight }
            .toSet()
        assertEquals(
            setOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold),
            weights,
        )
    }

    @Test
    fun `ui sans covers regular medium weights`() {
        val weights = AetherFontAssets
            .filter { it.family == AetherFontFamilyId.UiSans }
            .map { it.weight }
            .toSet()
        assertEquals(
            setOf(FontWeight.Normal, FontWeight.Medium),
            weights,
        )
    }

    @Test
    fun `display serif filenames are newsreader latin endpoints`() {
        val names = AetherFontAssets
            .filter { it.family == AetherFontFamilyId.DisplaySerif }
            .map { it.fileName }
            .toSet()
        assertEquals(
            setOf("newsreader_regular.ttf", "newsreader_semibold.ttf", "newsreader_bold.ttf"),
            names,
        )
    }

    @Test
    fun `ui sans filenames are inter latin endpoints`() {
        val names = AetherFontAssets
            .filter { it.family == AetherFontFamilyId.UiSans }
            .map { it.fileName }
            .toSet()
        assertEquals(
            setOf("inter_regular.ttf", "inter_medium.ttf"),
            names,
        )
    }

    @Test
    fun `OFL license texts are bundled under assets licenses`() {
        val licensesDir = File("src/main/assets/licenses").absoluteFile
        listOf("newsreader_OFL.txt", "inter_OFL.txt").forEach { name ->
            val file = File(licensesDir, name)
            assertTrue("Missing OFL license: $name (looked under ${licensesDir})", file.exists())
            assertTrue("Empty OFL license: $name", file.length() > 256L)
            assertTrue(
                "License file does not look like OFL: $name",
                file.readText().contains("SIL Open Font License", ignoreCase = false),
            )
        }
    }
}
