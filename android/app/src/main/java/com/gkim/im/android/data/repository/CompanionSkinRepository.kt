package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.CharacterSkin
import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.SkinRarity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * R2.4 — companion-skin-gacha repository.
 *
 * Until R2.2 lands the real `/api/v1/skins/catalog` endpoint, this
 * repository holds the five seeded default skins as in-memory rows.
 * The endpoint will become a remote source later; the consumers (R3
 * SkinGallery + R2.5 avatar URL construction) only see the catalog
 * flow, not the source.
 *
 * Active skin per (user, character) is held in-memory for now too;
 * R2.3 swaps the persistence layer for `user_active_skin` rows on
 * the backend. The contract is "default" until the user explicitly
 * activates an alternate skin they own.
 */
interface CompanionSkinRepository {
    /** Every skin the catalog knows about (5 defaults today; grows as R3.1 adds non-default skins). */
    val catalog: StateFlow<List<CharacterSkin>>

    /** The set of skin ids the user has acquired. Default skins are owned by every user implicitly. */
    val ownedSkinIds: StateFlow<Set<String>>

    /** Active skin id keyed by characterId. Lookup falls back to "default". */
    val activeSkinByCharacter: StateFlow<Map<String, String>>

    /** Sets the active skin for a character. Caller must verify ownership before calling. */
    fun activateSkin(characterId: String, skinId: String)

    fun activeSkinIdFor(characterId: String): String =
        activeSkinByCharacter.value[characterId] ?: CompanionCharacterCard.DEFAULT_SKIN_ID
}

class InMemoryCompanionSkinRepository(
    catalogSeed: List<CharacterSkin> = SeedDefaultSkins,
) : CompanionSkinRepository {

    private val _catalog = MutableStateFlow(catalogSeed)
    override val catalog: StateFlow<List<CharacterSkin>> = _catalog.asStateFlow()

    private val _ownedSkinIds = MutableStateFlow(catalogSeed.filter { it.isDefault }.map { it.skinId }.toSet())
    override val ownedSkinIds: StateFlow<Set<String>> = _ownedSkinIds.asStateFlow()

    private val _activeSkinByCharacter = MutableStateFlow<Map<String, String>>(emptyMap())
    override val activeSkinByCharacter: StateFlow<Map<String, String>> = _activeSkinByCharacter.asStateFlow()

    override fun activateSkin(characterId: String, skinId: String) {
        require(_ownedSkinIds.value.contains(skinId)) {
            "Cannot activate unowned skin $skinId for $characterId"
        }
        _activeSkinByCharacter.value = _activeSkinByCharacter.value.toMutableMap().apply {
            this[characterId] = skinId
        }
    }

    @Suppress("unused") // exposed for future R4.2 skin-draw integration
    fun grantSkin(skinId: String) {
        _ownedSkinIds.value = _ownedSkinIds.value + skinId
    }
}

// R2.4 seed data — five default skins for the seeded characters. The
// per-character `skinId` is the qualified id `{characterId}-default`
// per design.md (PK is unique across the catalog). When R3.1 lands
// the EPIC + LEGENDARY skins, those rows append here until R2.2
// swaps the source for the network catalog.
internal val SeedDefaultSkins: List<CharacterSkin> = listOf(
    "architect-oracle" to LocalizedText(english = "Default", chinese = "默认"),
    "sunlit-almoner"   to LocalizedText(english = "Default", chinese = "默认"),
    "midnight-sutler"  to LocalizedText(english = "Default", chinese = "默认"),
    "opal-lantern"     to LocalizedText(english = "Default", chinese = "默认"),
    "glass-mariner"    to LocalizedText(english = "Default", chinese = "默认"),
).map { (cid, name) ->
    CharacterSkin(
        skinId = "$cid-default",
        characterId = cid,
        displayName = name,
        rarity = SkinRarity.Common,
        artVersion = 1,
        isDefault = true,
        traits = emptyList(),
    )
}

// Convenience accessor used by FlowOf-style callers that don't need
// the StateFlow itself (e.g. one-shot lookups during tests).
@Suppress("unused")
fun CompanionSkinRepository.catalogFlow(): Flow<List<CharacterSkin>> = catalog
