package com.gkim.im.android.core.strings

import com.gkim.im.android.core.model.AppLanguage

/**
 * In-character bilingual empty-state copy for the Tavern visual direction.
 *
 * Each entry pairs English + Chinese strings; resolve via [pick] when the
 * caller has an [AppLanguage] in hand. The copy intentionally drops the
 * IM-app residue ("active conversations", "sign in to start chatting")
 * in favor of the cocktail-bar metaphor that R1 + R2 establish.
 */
data class BilingualCopy(val english: String, val chinese: String) {
    fun pick(language: AppLanguage): String = when (language) {
        AppLanguage.English -> english
        AppLanguage.Chinese -> chinese
    }
}

object CompanionStrings {

    /** Messages-list empty state (no active companion conversations yet). */
    val MessagesListEmpty: BilingualCopy = BilingualCopy(
        english = "The bar is empty. Pull up a stool and pick a card to start a conversation.",
        chinese = "还没有人来过你的酒馆。挑一张角色牌坐下，让对话开始。",
    )

    /** Contact-search no-results state (query non-blank, server returned 0 hits). */
    val ContactSearchNoResults: BilingualCopy = BilingualCopy(
        english = "Nobody by that name has walked in tonight. Try another spelling, or invite them another time.",
        chinese = "今晚没有这个名字的客人到过。换种拼法再找，或下回再请他来。",
    )

    /** Post-relationship-reset transient state (Completed phase of the reset flow). */
    val PostRelationshipReset: BilingualCopy = BilingualCopy(
        english = "The seat is wiped clean. Take a breath, then pull up a fresh card when you're ready.",
        chinese = "座位已经擦干净了。喘口气，准备好就再翻一张新角色牌坐下。",
    )
}
