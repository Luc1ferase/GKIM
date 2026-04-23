package com.gkim.im.android.feature.bootstrap

import org.junit.Assert.assertEquals
import org.junit.Test

class BootstrapAcknowledgmentGatingTest {

    private val currentVersion = "2026-04-23-v1"

    @Test
    fun `debug build always allows regardless of backend or local state`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = true,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Unknown,
            localAcceptedAtMillis = null,
            localAcceptedVersion = "",
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.Allow, decision)
    }

    @Test
    fun `debug build allows even if backend explicitly says not accepted`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = true,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                accepted = false,
                version = currentVersion,
            ),
            localAcceptedAtMillis = null,
            localAcceptedVersion = "",
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.Allow, decision)
    }

    @Test
    fun `release build with backend accepted + matching version allows`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                accepted = true,
                version = currentVersion,
            ),
            localAcceptedAtMillis = 1_700_000_000_000L,
            localAcceptedVersion = currentVersion,
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.Allow, decision)
    }

    @Test
    fun `release build with backend not accepted requires acknowledgment (first launch)`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                accepted = false,
                version = "",
            ),
            localAcceptedAtMillis = null,
            localAcceptedVersion = "",
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.RequireAcknowledgment, decision)
    }

    @Test
    fun `release build with backend accepted at stale version requires acknowledgment (version bump)`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                accepted = true,
                version = "2025-01-01-v0",
            ),
            localAcceptedAtMillis = 1_600_000_000_000L,
            localAcceptedVersion = "2025-01-01-v0",
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.RequireAcknowledgment, decision)
    }

    @Test
    fun `backend snapshot unknown falls back to local accepted state`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Unknown,
            localAcceptedAtMillis = 1_700_000_000_000L,
            localAcceptedVersion = currentVersion,
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.Allow, decision)
    }

    @Test
    fun `backend snapshot unknown with no local acceptance requires acknowledgment`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Unknown,
            localAcceptedAtMillis = null,
            localAcceptedVersion = "",
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.RequireAcknowledgment, decision)
    }

    @Test
    fun `backend snapshot unknown with local accepted at stale version requires acknowledgment`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Unknown,
            localAcceptedAtMillis = 1_600_000_000_000L,
            localAcceptedVersion = "2025-01-01-v0",
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.RequireAcknowledgment, decision)
    }

    @Test
    fun `backend says accepted=true but version empty still requires acknowledgment (malformed backend)`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                accepted = true,
                version = "",
            ),
            localAcceptedAtMillis = null,
            localAcceptedVersion = "",
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.RequireAcknowledgment, decision)
    }

    @Test
    fun `subsequent-launch bypass backend accepted matching version is the happy path`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                accepted = true,
                version = currentVersion,
            ),
            localAcceptedAtMillis = 1_700_000_000_000L,
            localAcceptedVersion = currentVersion,
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.Allow, decision)
    }

    @Test
    fun `gate does not allow on release when local millis is zero and backend says not accepted`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                accepted = false,
                version = "",
            ),
            localAcceptedAtMillis = 0L,
            localAcceptedVersion = "",
            currentVersion = currentVersion,
        )
        assertEquals(BootstrapAcknowledgmentDecision.RequireAcknowledgment, decision)
    }

    @Test
    fun `backend authoritative says accepted=false overrides local acceptance of current version`() {
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = false,
            backendSnapshot = BootstrapAcknowledgmentSnapshot.Known(
                accepted = false,
                version = currentVersion,
            ),
            localAcceptedAtMillis = 1_700_000_000_000L,
            localAcceptedVersion = currentVersion,
            currentVersion = currentVersion,
        )
        assertEquals(
            "backend is source of truth; it saying not-accepted must force re-acknowledgment",
            BootstrapAcknowledgmentDecision.RequireAcknowledgment,
            decision,
        )
    }
}
