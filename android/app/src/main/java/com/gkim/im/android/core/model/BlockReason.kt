package com.gkim.im.android.core.model

enum class BlockReason(val wireKey: String) {
    SelfHarm("self_harm"),
    Illegal("illegal"),
    NsfwDenied("nsfw_denied"),
    MinorSafety("minor_safety"),
    ProviderRefusal("provider_refusal"),
    Other("other");

    companion object {
        fun fromWireKey(key: String?): BlockReason =
            key?.let { raw -> entries.firstOrNull { it.wireKey == raw } } ?: Other
    }
}
