package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.ui.tavernCardAvatarUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class TavernCardAvatarLoaderTest {

    @Test
    fun `tavern card avatar resolves the active skin thumb url`() {
        // R2.5 — every tavern card asks the singleton ImageLoader for the
        // (characterId, activeSkinId) pair's v1 thumb.webp. The activeSkinId
        // comes from the resolved card; tests pin both values explicitly.
        assertEquals(
            "https://cdn.lastxuans.sbs/character-skins/architect-oracle/default/v1/thumb.webp",
            tavernCardAvatarUrl(
                characterId = "architect-oracle",
                activeSkinId = "default",
            ),
        )
    }

    @Test
    fun `tavern card avatar uses the active skin id, not a hardcoded default`() {
        // Locks the R2.5 contract: switching active skin changes the URL.
        val withDefault = tavernCardAvatarUrl(
            characterId = "sunlit-almoner",
            activeSkinId = "default",
        )
        val withAlternate = tavernCardAvatarUrl(
            characterId = "sunlit-almoner",
            activeSkinId = "sunlit-almoner-lantern-keeper",
        )
        // The two URLs must differ in exactly the {skinId} segment.
        val expectedAlternate = withDefault.replace(
            "/default/v1/",
            "/sunlit-almoner-lantern-keeper/v1/",
        )
        assertEquals(expectedAlternate, withAlternate)
    }

    @Test
    fun `tavern card avatar URL stays parameterized only on character id when skin is constant`() {
        val a = tavernCardAvatarUrl(characterId = "sunlit-almoner",  activeSkinId = "default")
        val b = tavernCardAvatarUrl(characterId = "midnight-sutler", activeSkinId = "default")
        assertEquals(a.replace("sunlit-almoner", "midnight-sutler"), b)
    }
}
