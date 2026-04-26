## ADDED Requirements

### Requirement: Client passes resolved character prompt context with every companion-turn submit, edit, and regenerate-at request

The system SHALL include a resolved `characterPromptContext` payload on every companion-turn `submit`, `edit`, and `regenerate-at` HTTP request whenever an active companion card is in scope at the call site. The payload SHALL carry six string values resolved in the active language: `systemPrompt`, `personality`, `scenario`, `exampleDialogue`, `userPersonaName`, `companionDisplayName`. The payload's source of truth is the active `CompanionCharacterCard.resolve(language)` projection plus the active `UserPersona.displayName.resolve(language)`; the client MUST NOT pre-substitute `{{user}}` / `{{char}}` macros in any of the four character-content fields — substitution is the backend prompt-assembly module's responsibility, so a single audit log row reflects the substituted prompt without the client and backend racing on substitution rules.

The payload field SHALL be optional on the wire. When the active card cannot be resolved (e.g., transient roster lookup miss, conversation has no companion card id), the request SHALL omit the field rather than send a half-populated payload. This preserves forward-compatibility with deployments of the S1 backend that do not yet read the field.

#### Scenario: Active card and active persona produce a fully populated payload

- **WHEN** an authenticated user sends a companion turn with `activeCompanionId` resolvable to a card whose `systemPrompt`, `personality`, `scenario`, `exampleDialogue`, and `displayName` are all populated, and the active persona's `displayName` resolves to a non-empty string in the active language
- **THEN** the outgoing request body MUST carry `characterPromptContext` with all six fields equal to the resolved values; the four character-content fields MUST contain any `{{user}}` / `{{char}}` macros literally without substitution; `userPersonaName` MUST equal the active persona's display name resolved in the active language; `companionDisplayName` MUST equal the card's `displayName` resolved in the active language

#### Scenario: Missing card omits the payload

- **WHEN** an authenticated user sends a companion turn for a conversation whose `companionCardId` is `null` or whose card cannot be resolved at submit time
- **THEN** the outgoing request body MUST omit `characterPromptContext` (or send `null`, depending on the kotlinx.serialization encoding convention configured for the project); the request MUST otherwise be byte-identical to the pre-slice payload so the deployed S1 backend continues to accept it

#### Scenario: Missing persona substitutes a localized default name

- **WHEN** the user has not yet configured a custom `UserPersona` so the active persona is `null`
- **THEN** `userPersonaName` MUST be `"User"` for `AppLanguage.English` or `"用户"` for `AppLanguage.Chinese`; the other five fields are unaffected; the request MUST still be sent rather than skipped

#### Scenario: Edit and regenerate-at carry the payload identically to submit

- **WHEN** the user edits a user bubble or regenerates a companion turn at a target message id
- **THEN** the outgoing `EditUserTurnRequestDto` and `RegenerateAtRequestDto` MUST each carry the same `characterPromptContext` shape resolved against the same active card and active persona, with the same macro-non-substitution and missing-card / missing-persona rules; the active-language source MUST be the same `LocalAppLanguage.current` value the chat surface is rendering in

### Requirement: LiveCompanionTurnRepository carries character prompt context through the retry path

The system SHALL retain the `characterPromptContext` payload across submit retries so a re-sent failed turn produces a request byte-equivalent to the original (modulo a fresh `clientTurnId`). The retry path SHALL NOT re-resolve the card or persona at retry time; if the card or persona has changed between the failed submit and the retry, the retry MUST still carry the payload captured at the original submit so the backend prompt-assembly remains consistent within a single user-perceived send-and-retry attempt.

#### Scenario: Retry replays the original character prompt context

- **WHEN** a user's companion-turn submit fails (network loss, backend 5xx, etc.) and the user invokes the retry affordance from the failed-bubble UI
- **THEN** the retry request body MUST carry the same `characterPromptContext` field values as the original submit, including any macro-bearing strings; the `clientTurnId` MUST be a fresh value per the existing idempotency contract

#### Scenario: Retry without a captured payload sends no payload

- **WHEN** the original failed submit had no `characterPromptContext` (the active card was unresolvable at original-submit time)
- **THEN** the retry request body MUST also omit `characterPromptContext`; the retry MUST NOT attempt a fresh resolution of the card at retry time
