package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.ui.tavernCardAvatarUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class TavernCardAvatarLoaderTest {

    @Test
    fun `tavern card avatar resolves the default skin thumb url`() {
        // R1.5 — until R2.5 lands the real flow, every tavern card asks the
        // singleton ImageLoader for the same key shape: the character's
        // 'default' skin v1 thumb.webp.
        assertEquals(
            "https://cdn.lastxuans.sbs/character-skins/architect-oracle/default/v1/thumb.webp",
            tavernCardAvatarUrl("architect-oracle"),
        )
    }

    @Test
    fun `tavern card avatar url is parameterized only on character id`() {
        // Two different characters get two different URLs that differ only
        // in the {characterId} segment. Locks the per-card-id contract.
        val a = tavernCardAvatarUrl("sunlit-almoner")
        val b = tavernCardAvatarUrl("midnight-sutler")
        assertEquals(a.replace("sunlit-almoner", "midnight-sutler"), b)
    }
}
