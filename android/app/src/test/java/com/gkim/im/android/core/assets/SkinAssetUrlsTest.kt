package com.gkim.im.android.core.assets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinAssetUrlsTest {

    @Test
    fun `portrait url for known skin matches the design contract example`() {
        // Mirrors the design.md `Asset URL resolves to the contract-shaped
        // path` scenario so the spec invariant is locked in code.
        val url = skinAssetUrl(
            characterId = "founder-architect",
            skinId = "lantern-keeper",
            version = 1,
            variant = SkinVariant.Portrait,
        )
        assertEquals(
            "https://cdn.lastxuans.sbs/character-skins/founder-architect/lantern-keeper/v1/portrait.png",
            url,
        )
    }

    @Test
    fun `each variant maps to the documented webp filename`() {
        val expected = mapOf(
            SkinVariant.Thumb to "thumb.png",
            SkinVariant.Avatar to "avatar.png",
            SkinVariant.Portrait to "portrait.png",
            SkinVariant.Banner to "banner.png",
        )
        for ((variant, fileName) in expected) {
            assertEquals(fileName, variant.fileName)
            val url = skinAssetUrl("c", "s", 1, variant)
            assertTrue("url for $variant must end with $fileName: $url", url.endsWith("/$fileName"))
        }
    }

    @Test
    fun `version is rendered as v-prefixed segment`() {
        val url = skinAssetUrl("c", "s", 7, SkinVariant.Thumb)
        assertTrue("version segment must be v7, got $url", url.contains("/v7/"))
        assertTrue("must not contain bare 7 segment", !url.contains("/7/"))
    }

    @Test
    fun `cdn host and key prefix are pinned to the contract`() {
        assertEquals("cdn.lastxuans.sbs", SkinAssetCdnHost)
        assertEquals("character-skins", SkinAssetKeyPrefix)
    }

    @Test
    fun `blank character id is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            skinAssetUrl("", "default", 1, SkinVariant.Thumb)
        }
    }

    @Test
    fun `blank skin id is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            skinAssetUrl("c", "", 1, SkinVariant.Thumb)
        }
    }

    @Test
    fun `version less than one is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            skinAssetUrl("c", "s", 0, SkinVariant.Thumb)
        }
    }
}
