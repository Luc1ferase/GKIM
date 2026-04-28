package com.gkim.im.android.core.model

// R2.4 — companion-skin-gacha trait model.
//
// SkinTraitKind is the closed set defined in design.md. Payload is a
// sealed type so the system-prompt-assembly layer can branch on it
// safely. The user-facing `description` is what the LOCKED preview
// shows; the `payload` is never exposed to UI (especially PersonaMod
// system-prompt appendices).

enum class SkinTraitKind {
    PersonaMod,
    Greeting,
    VoiceTone,
    RelationshipBoost,
}

sealed interface SkinTraitPayload {
    data class PersonaMod(val systemPromptAppendix: LocalizedText) : SkinTraitPayload
    data class Greeting(val opener: LocalizedText) : SkinTraitPayload
    data class VoiceTone(val toneTag: String) : SkinTraitPayload
    data class RelationshipBoost(val multiplier: Float) : SkinTraitPayload
}

data class SkinTrait(
    val traitId: String,
    val kind: SkinTraitKind,
    val description: LocalizedText,
    val payload: SkinTraitPayload,
)
