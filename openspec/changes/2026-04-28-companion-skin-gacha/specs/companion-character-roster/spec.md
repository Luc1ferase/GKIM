## MODIFIED Requirements

### Requirement: Character draw behavior remains explicit and non-deceptive

The system SHALL support a draw flow that can grant companion-character skins to the user, and it MUST represent the outcome clearly enough that users can tell whether they obtained a usable skin, whether the underlying character is newly added to their owned roster, and whether the skin itself is new or a duplicate of one they already own. The draw response MUST carry an explicit `state` discriminator equal to `NEW_CHARACTER`, `NEW_SKIN`, or `DUPLICATE_SKIN`, and the user-visible surface MUST reflect that distinction (full-screen reveal for `NEW_CHARACTER`, mid-card reveal for `NEW_SKIN`, inline summary for `DUPLICATE_SKIN`).

#### Scenario: User performs a character draw and the response carries the state discriminator

- **WHEN** the user triggers the draw flow from the tavern surface
- **THEN** the system returns an explicit draw outcome that identifies the resulting skin, the character it belongs to, and a `state` value drawn from the closed set `{NEW_CHARACTER, NEW_SKIN, DUPLICATE_SKIN}`

#### Scenario: A new character draw is reflected in the owned roster

- **WHEN** a draw resolves with `state = "NEW_CHARACTER"`
- **THEN** the user's owned roster gains the drawn skin's character on next view, and that character's active skin is set to the freshly drawn skin

#### Scenario: A duplicate-skin draw does not lose value

- **WHEN** a draw resolves with `state = "DUPLICATE_SKIN"`
- **THEN** the system credits a positive number of story shards equal to `rarity_value(rarity)` to `user_story_shards`, and the user-visible result names the contributing skin

### Requirement: User activates a roster skin, not a roster character, for companion chat

The system SHALL persist the user's active skin per character (not per character alone) in `user_active_skin`. When the user opens a chat with a companion, the system MUST resolve the active skin first and use that skin's `avatar` variant for the chat header, the skin's `PERSONA_MOD` and `GREETING` traits (if any) for the system-prompt assembly, and the skin's `VOICE_TONE` (if any) as a tone hint to the LLM. Switching the active skin MUST take effect for the next composed message in that thread, not for messages already sent.

#### Scenario: User activates a skin for chat

- **WHEN** the user selects an owned skin via the gallery's activate-confirm flow
- **THEN** the system upserts a row into `user_active_skin` for the `(user_id, character_id)` pair pointing to the selected `skin_id`, and subsequent chat-header avatar URLs for that character use the new skin's `avatar` variant

#### Scenario: Activation only takes effect on next composed message

- **WHEN** the user activates a different skin while a chat thread is open with messages already sent
- **THEN** previously-sent messages keep the avatar of the skin that was active when they were sent, and the next composed message picks up the new active skin's avatar + traits

### Requirement: The companion roster distinguishes preset, owned, and locked skins for browsability

The system SHALL present the roster such that the user can distinguish three categories at a glance: presets (every character's `is_default` skin, available without drawing), owned non-default skins (drawn from the gacha), and locked non-default skins (visible at the catalog level so the user knows they exist, but rendered as silhouettes with trait descriptions and not as the skins' actual art). The roster MUST NOT hide the existence of unowned skins behind a paywall — transparency about what is available is part of the non-deceptive contract.

#### Scenario: Preset skins are reachable without drawing

- **WHEN** a user has performed zero draws
- **THEN** every character's `is_default` skin appears in the roster as `OWNED_INACTIVE` (or `OWNED_ACTIVE` for the bootstrap-active selection), and every non-default skin appears as `LOCKED`

#### Scenario: A drawn non-default skin moves from LOCKED to OWNED_INACTIVE

- **WHEN** a draw resolves with `state = "NEW_SKIN"` for a previously-locked skin
- **THEN** the gallery cell for that skin transitions from `LOCKED` to `OWNED_INACTIVE` on the next character-detail open, retaining the existing active skin for that character until the user explicitly activates the new one

#### Scenario: Locked skins reveal trait descriptions, never trait payloads

- **WHEN** the user opens the locked-preview bottom sheet on a `LOCKED` cell
- **THEN** the sheet shows each trait's `description` text (for example "Greets you with a fortune-teller's opener"), and MUST NOT show the underlying `payload_json` (in particular, MUST NOT show any `PersonaMod.systemPromptAppendix` text)
