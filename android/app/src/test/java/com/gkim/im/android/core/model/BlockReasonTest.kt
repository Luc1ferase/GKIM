package com.gkim.im.android.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockReasonTest {

    @Test
    fun `enum values enumerate the closed set in design-doc order`() {
        val expected = listOf(
            BlockReason.SelfHarm,
            BlockReason.Illegal,
            BlockReason.NsfwDenied,
            BlockReason.MinorSafety,
            BlockReason.ProviderRefusal,
            BlockReason.Other,
        )
        assertEquals(expected, BlockReason.entries.toList())
    }

    @Test
    fun `each enum value exposes the lowercase snake_case wire key`() {
        assertEquals("self_harm", BlockReason.SelfHarm.wireKey)
        assertEquals("illegal", BlockReason.Illegal.wireKey)
        assertEquals("nsfw_denied", BlockReason.NsfwDenied.wireKey)
        assertEquals("minor_safety", BlockReason.MinorSafety.wireKey)
        assertEquals("provider_refusal", BlockReason.ProviderRefusal.wireKey)
        assertEquals("other", BlockReason.Other.wireKey)
    }

    @Test
    fun `fromWireKey round trips every known wire key back to its enum`() {
        BlockReason.entries.forEach { reason ->
            assertEquals(reason, BlockReason.fromWireKey(reason.wireKey))
        }
    }

    @Test
    fun `fromWireKey falls back to Other on unknown wire key`() {
        assertEquals(BlockReason.Other, BlockReason.fromWireKey("something_new_server_added"))
        assertEquals(BlockReason.Other, BlockReason.fromWireKey("SELF_HARM"))
        assertEquals(BlockReason.Other, BlockReason.fromWireKey(""))
    }

    @Test
    fun `fromWireKey falls back to Other on null`() {
        assertEquals(BlockReason.Other, BlockReason.fromWireKey(null))
    }
}
