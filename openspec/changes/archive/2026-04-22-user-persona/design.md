## Context

The `feature/ai-companion-im` branch already has deep companion cards, memory/preset foundation, and a running LLM conversation layer. The one asymmetry remaining in the persona story is the user side: the backend knows the user by `displayName` only, so every `{{user}}` macro resolves to a single fixed string and the companion has no structured self-description of the user to reason over. That works for single-use chat but fails the tavern expectation that the user can say "today I'm playing as Alex, a medieval scholar" once and have every conversation reflect it.

SillyTavern ships `personas` as a named library per user, with exactly one active persona per chat, and a description block that gets injected right before or after the character persona in the prompt. Lobe-chat treats the user's role/tone as a first-class chat setting. Both substitute `{{user}}` via the active persona.

This slice ships the minimum viable version of that: server-persisted persona library, exactly one active persona globally, active-persona's `displayName` substituting `{{user}}` macros, active-persona's `description` injected as a new section in the deterministic token-budget allocator introduced by `companion-memory-and-preset`. Per-companion and per-conversation override are deferred to `tavern-experience-polish`.

Constraints:
- Public repo boundary: backend source stays private; this slice defines the contract.
- Service boundary (from `core/im-app`): persona state and macro resolution are server-owned; the APK carries no persona-resolution logic that would let a malicious client spoof the active persona.
- Bilingual contract (from `localize-companion-tavern-copy`): persona display name and description are bilingual; the active `AppLanguage` selects the rendered variant.
- Additive to `llm-text-companion-chat`: this slice does not change the submit request shape. The backend resolves the active persona from the authenticated account.
- Additive to `companion-memory-and-preset`: the allocator already has a priority order; this slice inserts one new section with a documented priority + drop position.

## Goals / Non-Goals

**Goals:**
- Persist a user persona library server-side with CRUD, seeded with a single built-in persona derived from the account's `displayName` on first boot.
- Guarantee exactly one active persona per user at any moment.
- Substitute `{{user}}` / `{user}` / `<user>` macros with the active persona's display name, and substitute `{{char}}` / `{char}` / `<char>` with the active companion card's display name, in every assembled prompt.
- Inject the active persona's `description` into the companion turn prompt as a dedicated section in the token-budget allocator, with a fixed priority and drop position.
- Expose persona library management in Settings and expose the active persona in the companion chat chrome.
- Let imported SillyTavern cards that carry a `persona.description` land into a `UserPersona` automatically (scoped through `sillytavern-card-interop` — this slice models the data shape only).

**Non-Goals:**
- Per-companion persona override (→ `tavern-experience-polish`).
- Per-conversation persona pinning (→ `tavern-experience-polish`).
- Persona avatar upload (text + initials placeholder only).
- Automatic persona suggestion based on topic.
- Voice persona / pronoun pronunciation.
- Multi-persona fusion or simultaneous active personas.
- Persona-level provider parameter overrides.
- Cross-user sharing / marketplace.
- SillyTavern preset JSON interop (that is `sillytavern-card-interop`'s job; this slice ships the domain model).

## Decisions

### 1. UserPersona shape

```
data class UserPersona(
    val id: String,
    val displayName: LocalizedText,
    val description: LocalizedText,       // self-description injected into prompt assembly
    val isBuiltIn: Boolean,               // true for the account-derived default persona
    val isActive: Boolean,                // server-enforced singleton
    val createdAt: Instant,
    val updatedAt: Instant,
    val extensions: JsonObject,           // forward-compat bag (pronouns, tags, pronoun rules — later)
)
```

Rationale:
- Matches the shape of `Preset` (`id`, bilingual name, `isBuiltIn`, `isActive`, `extensions`). Users learning Settings see a uniform mental model.
- `description` is bilingual because companion replies steer to the active `AppLanguage`; a persona-description stuck in the wrong language drags the reply out of language.
- `extensions` parks SillyTavern persona JSON fields that we do not yet model (pronoun rules, avatar url, tags). `sillytavern-card-interop` will populate it.

Alternatives considered:
- **Freeform `personaText: String`**: rejected — loses the displayName / description split that macro substitution needs.
- **Reuse `CompanionCharacterCard` as UserPersona**: rejected — user personas do not need `firstMes`, `alternateGreetings`, `systemPrompt`, etc. Sharing the type would drag unused fields into the user library.

### 2. Exactly one active persona per user, global scope

Per user, exactly one persona is active at any moment. There is no per-conversation or per-companion override in this slice. The active persona id is stored as a user-profile column and returned on bootstrap. Activating a different persona does not rewrite prior conversation history; it only affects future turn assemblies.

Rationale:
- Mirrors the active-preset contract from `companion-memory-and-preset` — consistent UX.
- Per-companion override is a product polish concern, owned by `tavern-experience-polish`.

### 3. Built-in default persona seeded from account displayName

On first bootstrap, the backend creates a built-in persona for the user:
- `displayName`: both-language variants filled with the account's current `displayName` (single-language accounts store the same text on both sides).
- `description`: both-language variants filled with a neutral placeholder ("A user interacting with the companion.") so the substitution + injection never produce an empty string.
- `isBuiltIn: true`, `isActive: true`.

The built-in persona is editable (the user can write a real self-description) but cannot be deleted. The user can duplicate it into a user-owned persona and edit freely.

Rationale:
- Guarantees every account has at least one valid persona. No "no active persona" edge case.
- `displayName` starts from the account so the substitution result is immediately reasonable.
- Built-in editability lets users flesh out the description without duplicating; deleting is forbidden so `{{user}}` substitution cannot resolve to nothing.

Alternatives considered:
- **Built-in is immutable** (like `Preset` built-ins): rejected — a user cannot be expected to duplicate just to set their own description. The default persona IS the user.
- **No built-in; user creates the first persona**: rejected — first-run UX that forces persona creation before chatting is friction we do not need.

### 4. Macro substitution: `{{user}}`, `{{char}}`, and single-brace variants

The backend substitutes every occurrence of `{{user}}`, `{user}`, and `<user>` with the active persona's `displayName` in the active `AppLanguage`, and every occurrence of `{{char}}`, `{char}`, and `<char>` with the active companion card's `displayName` (or `name` field, whichever is canonical for the card) in the active `AppLanguage`. Substitution happens right before the prompt is sent to the provider — the stored turn text is never rewritten retroactively.

Accepted macro forms — canonical six, shared by backend prompt assembler and Android `MacroSubstitution` helper:

| Role | Double-brace | Single-brace | Angle-bracket | Resolves to                                                                     |
| ---- | ------------ | ------------ | ------------- | ------------------------------------------------------------------------------- |
| user | `{{user}}`   | `{user}`     | `<user>`      | Active `UserPersona.displayName` in the active `AppLanguage`                    |
| char | `{{char}}`   | `{char}`     | `<char>`      | Active `CompanionCharacterCard.displayName` in the active `AppLanguage`         |

Both sides MUST match this list exactly. Substitution is case-insensitive (`{{User}}`, `{{CHAR}}` etc. resolve identically), so ST cards authored with capitalised macros still substitute. Unknown macro-like tokens (`{{random}}`, `{foo}`, `<bar>`, whitespaced `{{ user }}`) pass through as literal text. The canonical form list is mirrored in code at `core/model/MacroSubstitution.kt` via `UserForms` and `CharForms` so the Android client reuses the same list that the backend documents here.

Rationale:
- Three forms (`{{x}}`, `{x}`, `<x>`) because ST users rely on the double-brace form, some Lobe-chat exports use single-brace, and XML-style is common in Anthropic-flavored system prompts. Accepting all three avoids surprise.
- Substituting right before provider call keeps the stored message raw, which is important for regeneration, edit, and future prompt diffing tools.
- Persona-aware substitution upgrades the `llm-text-companion-chat` display-name-only behavior; the contract is backwards compatible because the substituted value is still a string.
- Shared form table prevents drift between the backend assembler and the client-side preview helper — a new form MUST land in both places together.

### 5. Persona description injection in the token-budget allocator

The allocator introduced by `companion-memory-and-preset` gains a new section: `userPersonaDescription`. Its priority + drop position:

Priority (higher = preserved longer):
- Above the rolling summary (user persona is load-bearing identity; summary is auto-regenerated).
- Below pinned facts (pins are explicit user commitments; persona description is durable but less specific).
- Above persona `exampleDialogue` (persona identity beats tone examples).

Drop order (when over budget, lowest priority first):
- Persona `exampleDialogue` (already dropped first by `companion-memory-and-preset`).
- Older half of recent-N turns.
- Rolling summary.
- **`userPersonaDescription`** ← added drop step between rolling summary and non-critical preset sections.
- Non-critical preset sections (`formatInstructions`, then `systemSuffix`).
- Never drop: pinned facts, persona `systemPrompt`, preset `systemPrefix`, `userPersonaDescription`'s displayName-only fallback, the user turn.

When `userPersonaDescription` is dropped, the `{{user}}` macro still substitutes to the display name (that substitution lives outside the allocator entirely).

Rationale:
- Keeps the user's identity visible even under heavy budget pressure (via `{{user}}` substitution), while allowing the longer description to drop before preset polish sections and the example dialogue.
- Pinned facts remain the ultimate user-controlled memory knob.

### 6. Chat chrome surfaces the active persona alongside the active preset

The companion chat chrome displays the active persona's display name in a small pill, mirrored next to the active-preset pill introduced by `companion-memory-and-preset`. Tapping the persona pill routes to the persona library in Settings. This keeps the "who am I talking as / which style" pair visible at all times.

Rationale:
- Symmetry with active-preset pill is easier to learn.
- One tap to Settings is short enough for rapid experimentation.

### 7. Settings: dedicated persona library screen

A Personas section in Settings lists every persona (built-in + user-owned) with display name, an active-badge, description preview, and actions for Create / Edit / Duplicate / Activate / Delete. Delete is disabled for built-ins and for the currently active persona. The editor screen edits bilingual display name + bilingual description, with validation that neither field may be blank.

Rationale:
- Mirrors the preset library layout, so users who learned one learn the other.
- Empty-field validation prevents degenerate `{{user}}` substitutions.

### 8. `{{user}}` / `{{char}}` resolution outside the server prompt pipeline

The substitution contract lives on the backend. However, certain client-side previews (e.g., rendering `firstMes` in the greeting picker before submit) currently contain macros. This slice adds a lightweight client-side substitution helper that expands `{{user}}` / `{user}` / `<user>` → active persona's display name and `{{char}}` / `{char}` / `<char>` → active companion's display name for UI preview only. This helper is presentation-only; it does not touch stored data.

Rationale:
- Without client-side substitution the greeting picker shows raw `{{user}}` to the user, which breaks immersion at first entry.
- A separate helper keeps the contract — backend is canonical, client-side is cosmetic.

## Risks / Trade-offs

- **Active persona change mid-conversation confusing users** → Mitigation: active-persona pill in the chat chrome makes the current choice obvious; activation is a single-tap but visible. No retroactive rewrite.
- **Built-in persona editability causing "who am I?" drift** → Mitigation: the built-in's default description is a neutral placeholder; users are invited to edit it the first time they open persona settings. Reverting to the seed text is offered in the editor.
- **Persona description token budget** → Mitigation: documented drop position in the allocator ensures the description drops before the preset polish sections; the `{{user}}` substitution remains regardless.
- **Macro form ambiguity** (double-brace vs single-brace vs angle-bracket) → Mitigation: accept all three forms; documented in spec. Unknown macros stay as literal text.
- **Persona deletion while active** → Mitigation: blocked server-side with a typed error, client-side by disabling the delete affordance.
- **Cross-language mismatch** (persona description only in one language) → Mitigation: editor requires both sides to be non-empty; if a user fills only one side, the other side auto-mirrors the filled side until edited, so `{{user}}` substitution never produces an empty string.
- **Client-side preview substitution drifting from server** → Mitigation: the helper uses the same macro form list as the server; a shared const list lives in `core/model` and is referenced on both sides.
- **Privacy of persona descriptions** → Mitigation: personas are user-owned; delete removes them; export is not part of this slice. Future export slices must make this opt-in.

## Migration Plan

1. Add the `user_personas` table and `activeUserPersonaId` column on the user profile in the backend. Seed the built-in persona on first bootstrap for each account (or on next access for existing accounts).
2. Extend the macro-substitution step in the prompt assembler to resolve `{{user}}` / `{user}` / `<user>` to the active persona's display name (display name in the active `AppLanguage`) and `{{char}}` / `{char}` / `<char>` to the active card's display name in the active `AppLanguage`.
3. Extend the allocator's priority ordering with a `userPersonaDescription` slot between pinned facts and rolling summary for priority, and between rolling summary and non-critical preset sections for drop order.
4. Add HTTP endpoints:
   - `GET/POST/PATCH/DELETE /api/personas` + `/{personaId}`
   - `POST /api/personas/{personaId}/activate`
   - `GET /api/personas/active`
5. Android side:
   - Add `UserPersona` model under `core/model`.
   - Add `UserPersonaRepository` interface + default + live implementations.
   - Extend `ImBackendClient` / `ImBackendHttpClient` with persona endpoints + DTOs.
   - Wire the repository into `AppContainer`.
6. Android UI:
   - Settings: Personas list + editor, mirroring the Presets UX.
   - Chat chrome: active-persona pill alongside active-preset pill.
   - Add shared `MacroSubstitution` helper in `core/model` for client-side preview substitution of greetings and any other macro-carrying UI string before the backend resolves it.
7. Verification:
   - Unit: macro substitution for the six accepted forms, persona CRUD, active-singleton enforcement, built-in immutable delete, `UserPersonaRepository` reducer, editor validation.
   - Integration: activating a new persona changes the `{{user}}` substitution in the next turn's reply; the persona description is observable in the token-budget allocator logs under the debug flag.
   - Instrumentation: Settings → Personas CRUD, chat chrome persona pill labeling, greeting picker showing the substituted display name.
8. Record delivery evidence in `docs/DELIVERY_WORKFLOW.md` following the slice template.

Rollback strategy:
- If the backend migration lands without client support, the active persona is still server-resolved and macros substitute correctly; the UI degrades to no persona controls.
- If the client ships without backend support, the repository surfaces stub/failure states and the app falls back to the display-name-only substitution from `llm-text-companion-chat`.

## Deferred scope (captured now, implemented later)

These items extend this slice's data model without requiring a schema change:

- **Per-companion persona override**: a `personaId` field on the companion card that shadows the active persona while the card is active. Owned by `tavern-experience-polish`.
- **Per-conversation persona pinning**: a `conversationPersonaId` field that overrides both the active persona and the per-companion default for that conversation. Owned by `tavern-experience-polish`.
- **Persona avatar upload**: image hosting + display in the persona list. Polish; uses `extensions.avatarUrl` placeholder today.
- **Pronoun rules**: beyond display-name substitution, some users want `{{user}}`-derived pronoun substitution. Requires pronoun fields; reserved in `extensions` bag for now.
- **Persona import from ST cards**: when a card carries a `persona.description`, offer to create a matching user persona. Owned by `sillytavern-card-interop`.
- **Telemetry around persona swaps**: which persona is active, how often they swap. Requires telemetry plumbing that is out of scope here.
- **Multi-persona fusion**: running two personas simultaneously in a single conversation. Much more complex; deferred indefinitely.

## Open Questions

- Should the active persona selection be mirrored in the compose input's placeholder ("Talking as Alex...")? Default: yes, subtle footer line under the chrome pills. Cheap polish.
- Should personas carry tags? Default: no in this slice — flat list only.
- Should the built-in persona be re-seeded when the account's `displayName` changes? Default: no — the user has already had a chance to edit it; silently rewriting their persona is surprising. We surface a Settings banner offering to sync instead.
- When a card carries `{{user}}` inside its `firstMes` and the user changes the active persona mid-conversation, should the stored greeting be retroactively re-substituted? Default: no — stored greetings capture substitution at persist time. Only the next assembled prompt uses the new persona.
- Should we expose the `{{char}}` macro doc to users who write persona descriptions? Default: not in this slice — the macro helps card authors more than persona authors. Revisit if we ship a card editor surface later.
- Should the greeting picker allow live preview of all active macros before the user commits? Default: yes — the client-side substitution helper renders preview text.
