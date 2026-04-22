## Why

The `llm-text-companion-chat` slice ships `{{user}}` macro substitution in its simplest form — the authenticated account's `displayName` is the only thing the backend knows to inject. The tavern ecosystem (SillyTavern, Lobe-chat) treats the user's identity as a first-class persona: a named bundle of display name + self-description, swappable per conversation or per intent, rendered into `{{user}}` slots and as a context block the companion can refer to. Without that, the companion cannot meaningfully address the user, cannot remember who the user said they were, and every import from ST loses the `persona.description` field.

This slice introduces the `UserPersona` capability so users can (1) describe themselves once as a durable persona, (2) maintain several personas and switch between them, (3) see their persona's name substitute into companion replies, and (4) let the companion's prompt assembly include a persona description alongside the active card's deep persona fields. It also finalizes the `{{char}}` macro for symmetry with `{{user}}`.

## What Changes

- **BREAKING** Every companion turn prompt is assembled with the active user persona's description injected into the token-budget allocator, and `{{user}}` / `{user}` / `<user>` macros substitute the active persona's display name rather than the authenticated account's display name.
- Introduce a dedicated capability covering the `UserPersona` model, persona library CRUD, active-persona selection, and `{{user}}` / `{{char}}` macro substitution semantics.
- Modify the Android app contract so Settings exposes a persona library (list, create, edit, duplicate, activate, delete), and the companion chat chrome surfaces the active persona alongside the active preset.
- Modify the backend contract so personas persist server-side, macro substitution resolves `{{user}}` to the active persona's display name and `{{char}}` to the active companion card's display name, and the prompt assembler folds the active persona's description into the deterministic token budget defined by `companion-memory-and-preset`.
- Reuse the companion turn submit contract from `llm-text-companion-chat`; this slice does not add new submit fields because the active persona is resolved server-side from the authenticated account's profile.

## Capabilities

### New Capabilities
- `user-persona`: Defines the `UserPersona` model, persona library semantics, active-persona rules, and the `{{user}}` / `{{char}}` macro substitution contract.

### Modified Capabilities
- `core/im-app`: The Android app requirements change so Settings exposes persona library management + active-persona selection, and so the companion chat chrome surfaces the active persona's display name.
- `im-backend`: The backend requirements change so persona state persists server-side, participates in prompt assembly and macro substitution, and is managed through dedicated HTTP endpoints.
- `companion-memory-and-preset`: The deterministic token-budget allocator gains a persona-description section in its priority ordering (additive; no requirements removed).

## Impact

- Affected Android code: new `core/model/UserPersonaModels.kt`, `data/remote/im/ImBackendClient.kt` + DTOs for persona endpoints, new `data/repository/UserPersonaRepository.kt` (+ `LiveUserPersonaRepository`), `feature/settings/*` (persona library UI + editor), minor extensions to the companion chat chrome to expose an active-persona pill alongside the active-preset pill, and macro-substitution adjustments in shared presentation layer where `{{user}}` appears in chat preview text (e.g., card greetings shown before submit).
- Affected backend contract: new `user_personas` table or equivalent, active-persona column on the user profile, macro-substitution rules in the prompt assembler, persona-library endpoints. The allocator section defined by `companion-memory-and-preset` gains a persona-description slot with a documented priority and drop position.
- Affected specs: new `user-persona`, plus deltas for `core/im-app`, `im-backend`, and `companion-memory-and-preset`.
- Affected UX: users can create multiple named personas with self-descriptions, switch the active persona, see their persona name appear in companion replies, and observe the active persona in the companion chat chrome. Imports from SillyTavern that carry a persona description now have a place to land.
- Non-goals (scoped out of this slice):
  - Per-companion persona override (→ `tavern-experience-polish`)
  - Per-conversation persona pinning (→ `tavern-experience-polish`)
  - Persona avatar upload / image hosting (text + initials placeholder only in this slice)
  - Voice persona / pronoun pronunciation rules beyond display-name substitution
  - Automatic persona suggestion based on conversation topic
  - Cross-user persona sharing or a persona marketplace
  - Multi-persona fusion (multiple active personas at once)
  - SillyTavern persona JSON interop schema (→ `sillytavern-card-interop`; this slice models the shape, that slice maps it)
  - Persona-level provider parameter overrides (temperature, etc. — owned by `companion-memory-and-preset`)
