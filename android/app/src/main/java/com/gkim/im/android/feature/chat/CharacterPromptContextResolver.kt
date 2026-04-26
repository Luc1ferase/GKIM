package com.gkim.im.android.feature.chat

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.core.model.resolve
import com.gkim.im.android.data.remote.im.CharacterPromptContextDto

/**
 * §3.1 of `companion-turn-character-prompt-context`. Resolves the active card +
 * active persona into the wire shape consumed by the paired backend slice's
 * prompt-assembly module. Returns `null` when no companion card is in scope so
 * callers can short-circuit before the wire call (matches the existing
 * `ChatViewModel.sendMessage` companion-only gate).
 *
 * Macros (`{{user}}` / `{{char}}`) are NOT pre-substituted here. The backend's
 * `assemble_messages` is the single substitution point per the paired backend
 * slice's `design.md` §2 — letting two implementations race on substitution
 * rules is exactly the source-of-truth split this slice is designed to avoid.
 */
internal fun resolveCharacterPromptContext(
    card: CompanionCharacterCard?,
    persona: UserPersona?,
    language: AppLanguage,
): CharacterPromptContextDto? {
    if (card == null) return null
    val resolved = card.resolve(language)
    val personaName = persona?.displayName?.resolve(language)?.takeIf { it.isNotBlank() }
        ?: defaultPersonaName(language)
    return CharacterPromptContextDto(
        systemPrompt = resolved.systemPrompt,
        personality = resolved.personality,
        scenario = resolved.scenario,
        exampleDialogue = resolved.exampleDialogue,
        userPersonaName = personaName,
        companionDisplayName = resolved.displayName,
    )
}

private fun defaultPersonaName(language: AppLanguage): String = when (language) {
    AppLanguage.English -> "User"
    AppLanguage.Chinese -> "用户"
}
