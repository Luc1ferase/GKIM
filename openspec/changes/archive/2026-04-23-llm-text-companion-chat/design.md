## Context

The `feature/ai-companion-im` branch already ships a tavern roster of deep persona cards and has an accepted backend contract for roster durability. What it does not yet have is the actual companion conversation: `feature/chat/ChatRoute.kt` still treats a companion the same as a human contact, relies on `MessagingRepository.sendMessage` / `RealtimeChatClient.sendMessage` (peer IM), and has no concept of LLM reply lifecycle, persona prompt injection, variants, or safety/timeout states.

The `pivot-to-ai-companion-im` parent change and the `ai-companion-experience` capability define the product contract at a high level: durable persona + relationship continuity, bounded memory, explicit failure/safety states, pending turn recovery. This slice is the concrete LLM-first realization of that contract. It is intentionally large because the conversation surface is the hard part of a tavern product; letting it bleed into the next slice would mean shipping an empty shell.

User explicitly requested (2026-04-21) that this slice land swipe variant navigation (Lobe-chat / SillyTavern style) as the core regenerate mechanic, with editable-any-bubble and arbitrary-layer regenerate written into the follow-up plan so the data model is correct on day one and Phase 2 does not force a rewrite. User also chose display-name `{{user}}` substitution now; multi-persona support defers to the `user-persona` slice.

Constraints:
- Public repo boundary: backend source stays private; this slice defines the backend contract that the private checkout implements.
- Service boundary (from `core/im-app`): Android client talks to HTTPS + WebSocket; no provider secrets in the APK for LLM text.
- Bilingual contract (from `localize-companion-tavern-copy`): the card's prose fields are bilingual; the active `AppLanguage` must steer LLM output language.
- Deep persona fields (from `companion-character-card-depth`): `systemPrompt` / `personality` / `scenario` / `exampleDialogue` / `firstMes` / `alternateGreetings` are already modeled and seeded bilingually.
- Existing realtime gateway: the WS client already reconnects with exponential backoff, parses typed events, and handles `session.registered` + `message.*` + `friend_request.*`. New companion events plug into the same pipeline.
- `ChatMessage` is the shared model used across the whole app; extending it in place is cheaper than forking a parallel companion-only type.

## Goals / Non-Goals

**Goals:**
- Ship persona-driven LLM companion chat with streaming reply lifecycle (thinking → streaming → completed) plus explicit failed / blocked / timeout terminal states.
- Land a swipe variant tree data model aligned with Lobe-chat / SillyTavern conventions, so each user turn can grow multiple companion variants without branching the whole conversation.
- Show a first-message picker (`firstMes` + `alternateGreetings`) on first entry to a companion conversation; skip it once history exists.
- Implement regenerate on the most recent companion bubble in this slice; design and document the data model + contract extensions needed for editable-any-bubble + arbitrary-layer regenerate as Phase 2 tasks.
- Keep persona prompt assembly and provider calls on the backend; the Android client carries only the turn request, user body, active language, and renders lifecycle events.
- Recover in-progress turns through a bootstrap endpoint + WS resume events so relaunch / reconnect never loses a turn silently.
- Surface blocked / failed / timeout states as first-class bubbles instead of silent drops.

**Non-Goals:**
- Do not implement memory summarization beyond a recent-N-turns window (→ `companion-memory-and-preset`).
- Do not implement user-persona branching or self-description (→ `user-persona`); only `{{user}}` → display name substitution in this slice.
- Do not implement World Info / lorebook triggers (→ `world-info-binding`).
- Do not implement per-character provider / model / temperature override (→ `tavern-experience-polish`).
- Do not implement SillyTavern PNG/JSON interop in this slice (the extensions bag reserves the data; interop comes with `sillytavern-card-interop`).
- Do not build voice / TTS / STT, multi-agent rooms, tool use / function calling, or structured output.
- Do not add cost accounting, rate-limit UI, or quota controls.
- Do not remove or refactor the current AIGC image generation chat affordances; they coexist with companion text chat. They will be reframed as companion-oriented in `companion-settings-and-safety-reframe`.

## Decisions

### 1. Split transport: HTTP submits, WebSocket streams

Turn submission (`POST /api/companion-turns`), regeneration (`POST /api/companion-turns/{turnId}/regenerate`), and pending-turn recovery (`GET /api/companion-turns/pending`) are HTTP. Reply lifecycle deltas (`companion_turn.started` / `companion_turn.delta` / `companion_turn.completed` / `companion_turn.failed` / `companion_turn.blocked`) stream over the existing WebSocket gateway.

Rationale:
- HTTP gives clean request/response for variable-sized turn bodies and explicit failure codes for configuration problems (missing provider, no active companion, etc.).
- WS matches the existing gateway pattern for durable IM events; adding a few event types is cheaper than standing up SSE.
- Reconnect plumbing in `RealtimeChatClient` already handles event replay on resume; extending the event shape is additive.

Alternatives considered:
- **Pure WS submit + stream**: rejected because retry semantics and multipart turn bodies become awkward on a command-pipe WS.
- **Pure HTTP SSE stream**: rejected because the app already opens one persistent WS for IM events and duplicating the long-lived connection path is not worth the isolation.

### 2. Variant tree data model (Lobe-chat / SillyTavern inspired)

Each conversation is a directed tree of `ChatMessage` rows. A companion turn group is a set of sibling messages sharing `variantGroupId`; the user's active path through the tree is resolved by picking one sibling per variant group. New fields on `ChatMessage`:

```
parentMessageId: String?    // null for the first message in a conversation
status: MessageStatus       // Pending / Thinking / Streaming / Completed / Failed / Blocked / Timeout
companionTurnMeta: CompanionTurnMeta?

data class CompanionTurnMeta(
    val turnId: String,
    val variantGroupId: String,
    val variantIndex: Int,        // 0-based position of this variant in its group
    val providerId: String,
    val model: String,
    val isEditable: Boolean,      // false for streaming, true after completion
    val canRegenerate: Boolean,   // true on the most recent companion variant in the active path
)
```

Key invariants:
- A user turn produces exactly one user message (`direction=Outgoing`, `kind=Text`, no `companionTurnMeta`).
- A companion reply to that user turn produces 1..N companion messages as siblings sharing `variantGroupId`; exactly one is marked active per conversation state.
- Regenerate appends a new sibling under the same `variantGroupId`; it does not delete prior variants.
- Swipe navigation mutates only the active index, not the tree.

Alternatives considered:
- **Flat list with `swipes: List<String>` per bubble** (pure ST style): rejected because mid-conversation branches lose metadata and because a phase-2 "regenerate layer 5" flow needs tree relationships that a flat swipes field cannot express.
- **Parallel `CompanionMessage` type beside `ChatMessage`**: rejected because the UI already streams one timeline; splitting doubles the rendering + persistence surface with no upside.

### 3. Backend owns persona prompt assembly

The backend resolves the active companion card, reads its deep fields (`systemPrompt`, `personality`, `scenario`, `exampleDialogue`, `firstMes`), and builds the LLM prompt server-side. The Android client only submits:

```
{
  conversationId,
  activeCompanionId,
  userTurnBody,
  activeLanguage,              // "en" or "zh"
  clientTurnId,                // idempotency key
  parentMessageId,             // for tree anchoring
}
```

Rationale:
- Matches the service-boundary rule in `core/im-app` and the backend-owns-orchestration decision in `pivot-to-ai-companion-im`.
- Keeps provider API keys and safety policy outside the APK.
- Future slices (memory, world info, user persona) can inject additional context server-side without touching the Android request shape.

Alternative considered:
- **Client assembles prompt, calls provider**: rejected because it couples the app to per-provider auth and breaks the companion continuity promise (memory/world info will not live on-device).

### 4. Streaming granularity: chunk-level, monotonic `deltaSeq`

`companion_turn.delta` events carry a `deltaSeq: Int` that increments monotonically per `turnId`, plus a `textDelta` chunk. Target chunk size is one meaningful sentence or ~40-60 characters, whichever comes first. Client applies deltas in `deltaSeq` order and ignores out-of-order or duplicate deltas after reconnect; if a gap is detected the client requests `GET /api/companion-turns/{turnId}` to snapshot-refresh.

Rationale:
- Token-level streaming over WS is expensive and scroll-jittery on mobile.
- Chunk-level preserves the "live" feel while letting the client batch rendering.
- Monotonic seq makes reconnect resumption unambiguous.

### 5. Reply lifecycle states

- `Pending`: client has sent HTTP submit, waiting for server ack.
- `Thinking`: server accepted; provider not yet responding. UI shows a shimmer bubble.
- `Streaming`: deltas arriving. UI shows growing body with a pulse.
- `Completed`: terminal success. Final text finalized, swipe + regenerate affordances active.
- `Failed`: terminal error (provider/orchestration/network). UI shows failure reason + Retry action.
- `Blocked`: terminal safety refusal. UI shows explicit block reason. Retry does not auto-fire; user can compose a new turn.
- `Timeout`: terminal expiry after 60s total or 15s no-delta. UI treats as a distinct subtype of `Failed` with explicit copy.

### 6. First-message picker on first conversation entry

When the user opens a companion conversation with an empty message history, the UI presents a picker listing the resolved `firstMes` plus every entry in `alternateGreetings` (localized to active language). User selects one; the selected greeting is persisted as a companion variant at `variantIndex=0` with `variantGroupId` scoped to "conversation opener". Once any history exists (system-inserted opener counts), the picker does not show.

Rationale:
- ST ecosystem users expect a greeting selector; hard-coding `firstMes` loses that expressiveness.
- Persisting through the same variant contract keeps the backend log honest and makes the opener swipeable.

### 7. `{{user}}` substitution: display name only, this slice

The backend replaces any of `{{user}}`, `{user}`, or `<user>` in assembled prompts with the authenticated user's `displayName`. No user-persona / self-description / multi-persona handling. The `user-persona` slice upgrades this to full persona injection.

Rationale:
- The simple substitution avoids awkward `{{user}}` leaking in companion replies while keeping this slice focused.
- Future persona injection just replaces the substituted value with a richer persona block without changing the client contract.

### 8. Pending turn recovery

On app startup / reconnect:
1. Client calls `GET /api/companion-turns/pending` with the active token.
2. Backend returns all turns in `Thinking` or `Streaming` state for this user.
3. Client rehydrates bubble state for each, subscribes to lifecycle events.
4. If a turn has been in `Streaming` >15s with no new delta, client requests snapshot.

Rationale:
- The pivot spec requires pending-turn recovery; a dedicated endpoint is cheaper than baking it into bootstrap.
- Keeps the WS event stream stateless; HTTP snapshot is authoritative for rehydration.

### 9. Safety policy: backend stub returns explicit block reason

First-slice backend safety policy is a thin guard that surfaces blocks with a typed reason (`self_harm` / `illegal` / `nsfw_denied` / `provider_refusal` / `other`). The block reason is echoed to the UI in neutral copy (no graphic detail). Actual policy tuning is owned by the private backend.

### 10. Single `ChatMessage` model extended in place

Extending the existing `ChatMessage` + introducing `MessageStatus` + `CompanionTurnMeta` keeps rendering, persistence, and history APIs unified. Callers that ignore `companionTurnMeta` (legacy peer IM code paths) continue to work unchanged.

## Risks / Trade-offs

- **Data model churn if Phase 2 reshapes things** → Mitigation: design the tree + variant shape with Phase 2 in mind from day one (editable / arbitrary-layer regenerate both reuse `parentMessageId` + append-sibling semantics). Do not defer the model change.
- **Streaming delta ordering under reconnect** → Mitigation: monotonic `deltaSeq` per turn; client discards duplicates, snapshot-refreshes on gap.
- **Provider latency blocking UI indefinitely** → Mitigation: Thinking state begins at T+500ms; Timeout at T+60s total or T+15s idle; client always has a Retry path.
- **Backend-side safety false positives** → Mitigation: block reasons are typed + overrideable; UI tells user the turn was blocked rather than pretending success.
- **Token cost for recent-N-turns context window** → Mitigation: initial N=10 turns; bounded memory comes with `companion-memory-and-preset`.
- **WS traffic explosion from token-level streaming** → Mitigation: chunk-level deltas with ~40-60 char granularity.
- **Coexistence with AIGC image gen inside the same chat** → Mitigation: do not touch the image flow; the `+` menu remains separate; image generation stays a distinct `AigcTask`, companion chat is `ChatMessage` with lifecycle.
- **ChatMessage growth pressure** → Mitigation: optional fields only; legacy call sites behave unchanged.
- **Variant tree visualization on mobile** → Mitigation: linear active-path view is default; `parentMessageId` is an implementation detail until a later slice adds a tree explorer.

## Backend migration intent (private checkout)

The public repo defines the contract; the private backend checkout owns
the implementation. For this slice the private PR will land roughly:

1. **Schema** — two tables (names indicative; MySQL first, generalized later):
   - `companion_turns (turn_id PK, conversation_id FK, active_companion_id,
     active_language, client_turn_id UNIQUE, parent_message_id, status,
     started_at, completed_at, failure_subtype, error_message, block_reason)`
     — captures one turn group; `UNIQUE(conversation_id, client_turn_id)`
     absorbs idempotent retries.
   - `companion_turn_variants (message_id PK, turn_id FK, variant_group_id,
     variant_index, provider_id, model, status, accumulated_body,
     last_delta_seq, started_at, completed_at)` — one row per sibling.
     `UNIQUE(variant_group_id, variant_index)` keeps swipe order stable.
2. **Monotonic `deltaSeq`** — column on `companion_turn_variants`, advanced
   by a serialized write per variant (row lock or per-variant mutex).
   Every gateway delta carries `(turn_id, delta_seq, text_delta)`; `deltaSeq`
   is never decreased, never reused, and never overlaps between variants of
   different turns.
3. **Pending turn index** — a covering index on
   `(owner_user_id, status)` restricted to `status IN ('thinking','streaming')`
   powers the `GET /api/companion-turns/pending` endpoint without scanning
   history. The query returns the variant row plus its `turn_id`.
4. **Authorization boundary** — every companion-turn HTTP + snapshot +
   pending endpoint filters by the authenticated user's conversations;
   gateway events only fan out on the owner's session bus.
5. **Provider abstraction** — see `Provider abstraction` below.

The private backend PR lands the migrations + orchestration + provider
adapter; this public slice lands only the contract and the Android-side
wiring. The delivery record for this change points to the private PR hash.

## Provider abstraction

The first-slice backend MUST accept at least one OpenAI-compatible text
provider (chat-completions JSON streaming). Additional text providers
(Tongyi Qwen text, Hunyuan text) are optional in this slice but the
provider layer MUST be pluggable:

- A `TextProvider` trait/interface owns
  `submit(prompt, language_hint, stream_callback) → TurnResult`.
- The dispatcher selects a provider by `provider_id`; the active card's
  configuration (or a server default) chooses which provider to call.
- No endpoint, DTO, or database column on the public contract is keyed to
  a single vendor — `provider_id` is a free-form string, `model` is a
  free-form string, and block/timeout reasons come from a shared vocabulary
  that doesn't leak vendor-specific error strings to the client.
- Adding a new provider is a backend-only change: no Android redeploy, no
  spec churn. The `tavern-experience-polish` slice will later expose
  per-character provider overrides; this slice just keeps the contract
  open.

## Migration Plan

1. Extend `ChatMessage` with `parentMessageId`, `status`, `companionTurnMeta` (nullable). Update mappers. Existing tests still compile because new fields are optional with defaults.
2. Extend `ImGatewayEvent` with `companion_turn.*` event types; extend `ImGatewayEventParser` + unit tests.
3. Extend `ImBackendClient` with `submitCompanionTurn(...)`, `regenerateCompanionTurn(turnId)`, `listPendingCompanionTurns()`; add DTOs + round-trip serializer tests.
4. Add `CompanionTurnRepository` interface + default implementation with tree invariants + variant navigation; add a `LiveCompanionTurnRepository` that wires HTTP submission + WS rehydration.
5. Wire `CompanionTurnRepository` into `AppContainer` and into a new or extended `ChatViewModel` path. Existing peer-IM path untouched.
6. Render companion lifecycle bubbles in `ChatRoute.kt`: Thinking shimmer, Streaming grow, Completed/Failed/Blocked/Timeout finals, swipe controls on active variant, regenerate action on the latest companion bubble.
7. Add greeting picker when companion conversation has no history. Route users through the same variant path.
8. Record the backend contract in `specs/im-backend/spec.md` delta; the private backend implements the schema, provider calls, safety stub, and pending-turn endpoint.
9. Run focused unit + instrumentation verification. Record delivery evidence.

Rollback strategy:
- If the backend contract cannot land in the same window, ship Android changes with a guard that falls back to the existing peer-IM send path for companion conversations; present a "Companion replies unavailable" banner instead of broken lifecycle UI.
- The data model additions are optional fields and do not require a rollback if later refined.

## Deferred scope (captured now, implemented later)

These items ride the same variant-tree data model established by this slice, so their implementation does not require a schema change:

- **Editable-any-bubble**: tapping any past companion or user message opens an inline edit; saving edits creates a new sibling `variantGroupId` with the edit as `variantIndex=0`. The conversation active-path picks the edit as the new node; descendants beneath the edited node become siblings of a new branch. Follow-up tasks will wire edit UI + backend "edit and reroute" endpoint.
- **Arbitrary-layer regenerate**: regenerate action on any past companion bubble (not only the most recent) appends a new sibling variant under the selected node's `variantGroupId` and re-runs the turn with that variant as the active path. Backend needs to accept a `regenerateFromTurnId` pointing back into history; the tree already supports this once edit-any-bubble lands.
- **Tree-view toggle**: a "branch explorer" overlay on the conversation that visualizes the full variant tree and lets users jump between branches. Optional UX polish.
- **Per-bubble provider override**: let each regenerate specify a different provider / model / temperature for A/B experimentation. Requires `tavern-experience-polish`'s per-character provider work.

These are captured in the roadmap but out of scope for this slice's tasks.

## Open Questions

- Should Thinking-state bubbles remain visible if the user navigates away and comes back (yes, via pending-turn recovery) or should we auto-collapse them after T+30s? Default: keep visible until terminal state.
- Should user-authored bubbles also support variants (for editable-user-turn)? Default in this slice: no — user turns are single-variant; edit support arrives in the follow-up phase.
- Should `companion_turn.started` carry the assembled prompt preview for debugging? Default: no in production; possibly yes under a developer flag.
- Should the greeting picker allow "random" selection? Default: no in this slice — explicit only to keep the data path clean.
- How many active concurrent companion turns per user should the backend allow? Default: one per conversation; more than one is blocked with an explicit reason.
