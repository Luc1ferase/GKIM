package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.ui.chatHeaderAvatarUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatHeaderActiveSkinTest {

    @Test
    fun `chat header avatar resolves the active skin avatar url for a companion thread`() {
        // R2.5 — the chat header constructs the AVATAR variant URL from
        // (companion card id, active skin id) so the brand-correct face
        // shows on chat enter. Locked: skinId is the active skin, not
        // hardcoded "default".
        assertEquals(
            "https://cdn.lastxuans.sbs/character-skins/architect-oracle/default/v1/avatar.png",
            chatHeaderAvatarUrl(
                characterId = "architect-oracle",
                activeSkinId = "default",
            ),
        )
    }

    @Test
    fun `chat header avatar uses AVATAR variant, not THUMB`() {
        // The chat header surface is 40 dp; thumb (96 px) would visibly
        // pixelate, so design.md pins the chat header to AVATAR (256 px).
        val url = chatHeaderAvatarUrl(
            characterId = "sunlit-almoner",
            activeSkinId = "default",
        )
        assert(url.endsWith("/avatar.png")) { "chat header URL must use AVATAR variant: $url" }
        assert(!url.endsWith("/thumb.png")) { "chat header must NOT use THUMB: $url" }
    }

    @Test
    fun `chat header avatar follows active skin switches`() {
        val withDefault = chatHeaderAvatarUrl(
            characterId = "midnight-sutler",
            activeSkinId = "default",
        )
        val withAlternate = chatHeaderAvatarUrl(
            characterId = "midnight-sutler",
            activeSkinId = "midnight-sutler-courier-cloak",
        )
        val expected = withDefault.replace(
            "/default/v1/",
            "/midnight-sutler-courier-cloak/v1/",
        )
        assertEquals(expected, withAlternate)
    }
}
