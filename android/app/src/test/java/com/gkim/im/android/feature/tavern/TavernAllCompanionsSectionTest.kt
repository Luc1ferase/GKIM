package com.gkim.im.android.feature.tavern

import org.junit.Assert.assertEquals
import org.junit.Test

class TavernAllCompanionsSectionTest {

    @Test
    fun `all-companions section testTag is exposed for instrumentation tests`() {
        // The Tavern home renders a SectionTitle with this testTag above the
        // preset / owned / custom companion sub-sections. Together they
        // contain every companion the user has acquired (preset + drawn +
        // user-imported), which is the spec-required umbrella that survives
        // the contacts-tab removal.
        assertEquals("tavern-all-companions-section", TavernAllCompanionsSectionTestTag)
    }

    @Test
    fun `testTag does not collide with the preset sub-section`() {
        // Belt-and-suspenders: ensure the umbrella tag is distinct from the
        // existing sub-section testTags ("tavern-preset-section",
        // "tavern-owned-section", "tavern-user-section") so layout tests
        // can target the umbrella header without aliasing.
        assert(TavernAllCompanionsSectionTestTag != "tavern-preset-section")
        assert(TavernAllCompanionsSectionTestTag != "tavern-owned-section")
        assert(TavernAllCompanionsSectionTestTag != "tavern-user-section")
    }
}
