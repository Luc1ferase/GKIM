package com.gkim.im.android.core.model

enum class FailedSubtype(val wireKey: String) {
    Transient("transient"),
    PromptBudgetExceeded("prompt_budget_exceeded"),
    AuthenticationFailed("authentication_failed"),
    ProviderUnavailable("provider_unavailable"),
    NetworkError("network_error"),
    Unknown("unknown");

    companion object {
        fun fromWireKey(key: String?): FailedSubtype =
            key?.let { raw -> entries.firstOrNull { it.wireKey == raw } } ?: Unknown
    }
}
