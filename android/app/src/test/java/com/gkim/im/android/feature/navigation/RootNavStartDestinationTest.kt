package com.gkim.im.android.feature.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RootNavStartDestinationTest {

    @Test
    fun `authenticated root nav start destination is tavern`() {
        assertEquals("tavern", RootStartDestination)
    }

    @Test
    fun `start destination is not the prior messages route`() {
        // R2.1 explicit anti-regression: the prior IM-residue default route
        // was "messages"; the Tavern visual direction requires authenticated
        // startup to land on the tavern surface instead.
        assert(RootStartDestination != "messages") {
            "RootStartDestination must not regress to 'messages'"
        }
    }

    @Test
    fun `start destination matches tavern bottom-nav route key`() {
        // The bottom nav exposes the tavern surface under route key "tavern"
        // (not the legacy "space" key). The start destination MUST point at
        // the same key so the auth-restored back stack and the Tavern tab
        // resolve to the same NavBackStackEntry.
        assertEquals("tavern", RootStartDestination)
    }
}
