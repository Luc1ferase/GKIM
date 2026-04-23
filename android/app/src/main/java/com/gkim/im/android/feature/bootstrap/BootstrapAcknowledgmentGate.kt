package com.gkim.im.android.feature.bootstrap

enum class BootstrapAcknowledgmentDecision {
    Allow,
    RequireAcknowledgment,
}

sealed interface BootstrapAcknowledgmentSnapshot {
    object Unknown : BootstrapAcknowledgmentSnapshot
    data class Known(
        val accepted: Boolean,
        val version: String,
    ) : BootstrapAcknowledgmentSnapshot
}

object BootstrapAcknowledgmentGate {

    fun decide(
        isDebugBuild: Boolean,
        backendSnapshot: BootstrapAcknowledgmentSnapshot,
        localAcceptedAtMillis: Long?,
        localAcceptedVersion: String,
        currentVersion: String,
    ): BootstrapAcknowledgmentDecision {
        if (isDebugBuild) return BootstrapAcknowledgmentDecision.Allow

        return when (backendSnapshot) {
            is BootstrapAcknowledgmentSnapshot.Known ->
                if (backendSnapshot.accepted && backendSnapshot.version == currentVersion) {
                    BootstrapAcknowledgmentDecision.Allow
                } else {
                    BootstrapAcknowledgmentDecision.RequireAcknowledgment
                }

            BootstrapAcknowledgmentSnapshot.Unknown ->
                if (localAcceptedAtMillis != null && localAcceptedVersion == currentVersion) {
                    BootstrapAcknowledgmentDecision.Allow
                } else {
                    BootstrapAcknowledgmentDecision.RequireAcknowledgment
                }
        }
    }
}
