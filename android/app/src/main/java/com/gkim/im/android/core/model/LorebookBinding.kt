package com.gkim.im.android.core.model

data class LorebookBinding(
    val lorebookId: String,
    val characterId: String,
    val isPrimary: Boolean = false,
)

fun List<LorebookBinding>.primaryFor(characterId: String): LorebookBinding? =
    firstOrNull { it.characterId == characterId && it.isPrimary }

fun List<LorebookBinding>.lorebookIdsBoundTo(characterId: String): List<String> =
    filter { it.characterId == characterId }.map { it.lorebookId }
