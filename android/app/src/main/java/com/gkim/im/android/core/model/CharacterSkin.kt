package com.gkim.im.android.core.model

// R2.4 — companion-skin-gacha skin model.
//
// Mirrors the catalog endpoint's shape per design.md so the eventual
// network deserialization is a 1:1 map. Until R2.2 lands the real
// /api/v1/skins/catalog endpoint, the in-memory CompanionSkinCatalog
// holds the five seeded default skins.

enum class SkinRarity(val value: Int) {
    Common(1),
    Rare(2),
    Epic(3),
    Legendary(4);

    companion object {
        fun fromValue(value: Int): SkinRarity = values().firstOrNull { it.value == value }
            ?: error("Unknown SkinRarity value: $value")
    }
}

data class CharacterSkin(
    val skinId: String,
    val characterId: String,
    val displayName: LocalizedText,
    val rarity: SkinRarity,
    val artVersion: Int,
    val isDefault: Boolean,
    val traits: List<SkinTrait> = emptyList(),
)
