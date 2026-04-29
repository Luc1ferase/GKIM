package com.gkim.im.android.core.assets

// R1.4 — companion-skin-gacha asset URL contract.
//
// Locked to the design.md key contract:
//   character-skins/{characterId}/{skinId}/v{n}/{variant}.webp
//
// Versioned keys are immutable; updates ship as v{n+1}. The custom CDN
// domain `cdn.lastxuans.sbs` fronts the Cloudflare R2 bucket `gkim-assets`.

const val SkinAssetCdnHost: String = "cdn.lastxuans.sbs"
const val SkinAssetKeyPrefix: String = "character-skins"

enum class SkinVariant(val fileName: String) {
    Thumb("thumb.png"),
    Avatar("avatar.png"),
    Portrait("portrait.png"),
    Banner("banner.png"),
}

fun skinAssetUrl(
    characterId: String,
    skinId: String,
    version: Int,
    variant: SkinVariant,
): String {
    require(characterId.isNotBlank()) { "characterId must not be blank" }
    require(skinId.isNotBlank()) { "skinId must not be blank" }
    require(version >= 1) { "version must be >= 1, was $version" }
    return "https://$SkinAssetCdnHost/$SkinAssetKeyPrefix/$characterId/$skinId/v$version/${variant.fileName}"
}
