## Context

The `llm-text-companion-chat` slice lands the companion turn lifecycle, variant tree, deep-persona prompt assembly, and macro substitution, but it explicitly defers two concerns that the tavern product requires to feel coherent:

- **Bounded memory**: companion replies currently see only a recent-N-turns window. Past roughly ten turns the companion forgets the user's name, earlier context, and the conversational through-line. `ai-companion-experience` and the `pivot-to-ai-companion-im` parent change both commit to a memory layer scoped per user-companion pair.
- **Prompt presets**: SillyTavern and Lobe-chat ecosystems expect a named bundle of prompt-template and provider-parameter fields that can be swapped as a unit. Without that primitive, users must rewrite every card to try a different system-prompt style, and the tavern import/export path has no place to park imported preset JSON.

This slice lands both. It is scoped tightly: single-summary bounded memory (no tiered / episodic), pinned-fact model for user-curated long-term recall, global preset library with exactly one active preset, deterministic token budgeting. It is additive to `llm-text-companion-chat` — the prompt-assembly entry point in that slice is extended, not redefined.

Constraints:
- Public repo boundary: backend source stays private; this slice defines the backend contract.
- Service boundary (from `core/im-app`): Android client hits HTTPS only; summarization runs server-side; no provider secrets in the APK.
- Variant-tree contract (from `llm-text-companion-chat`): pinning operates on any `ChatMessage` identifier within a companion conversation, including non-active-path variants.
- Deep persona fields (from `companion-character-card-depth`): persona prose is part of the token budget, not replaced by memory — the budget allocator weighs both.
- Bilingual contract (from `localize-companion-tavern-copy`): summary prose is produced server-side in the user's active `AppLanguage`; preset prose fields are bilingual via `LocalizedText` where surfaced to users.

## Goals / Non-Goals

**Goals:**
- Give each user-companion pair a server-persisted bounded memory consisting of a rolling summary (natural-language prose) plus a list of pinned facts (short structured snippets).
- Let the user view that memory, pin any past message as a long-lived fact, unpin a fact, and reset memory with three granularities (pinned only / summaries only / all).
- Introduce a Preset bundle: a named set of prompt-template sections (system prefix / system suffix / format instructions / post-history instructions) plus core provider parameters (temperature, top-p, max reply tokens) that users can create, edit, activate, duplicate, and delete.
- Ship three default presets so the preset library is non-empty on first launch.
- Extend the token-budget assembler so every companion turn prompt includes: active preset's prefix/suffix + persona fields + memory summary + pinned facts + recent-N turns + user turn, all dropped in a deterministic priority order when the budget fills.
- Trigger rolling-summary regeneration on a deterministic schedule (every N completed turns OR when the running budget would otherwise exceed a soft cap), without blocking the in-flight turn.

**Non-Goals:**
- Tiered memory (short-term / mid-term / long-term separate stores) — out of scope; this slice ships a single summary field that is forward-compatible to tiered storage behind the same API shape.
- Episodic memory with embedding retrieval — deferred (depends on a vector store that this slice does not stand up).
- Per-character preset override — lives in `tavern-experience-polish`.
- User persona depth (self-description, multi-persona, persona editor) — lives in `user-persona`. This slice still uses the display-name substitution from `llm-text-companion-chat`.
- World Info / lorebook — lives in `world-info-binding`.
- SillyTavern preset JSON interop — lives in `sillytavern-card-interop` (this slice defines the model; interop converters attach later).
- Automatic "entity" or "relationship" extraction from conversation — future; pinning is manual in this slice.
- UI surfacing provider-level token accounting / cost / quota.
- Cross-companion memory sharing, or a public preset marketplace.

## Decisions

### 1. Memory shape: single rolling summary + pinned facts

Per `(userId, companionCardId)` pair the backend persists:

```
companion_memories
  userId              String
  companionCardId     String
  summaryLocalized    LocalizedText       // current rolling summary, bilingual
  summaryUpdatedAt    Instant
  summaryTurnCursor   Int                 // index of the last turn folded into summary
  tokenBudgetHint     Int?                // optional per-pair override of default budget

companion_memory_pins
  id                  String
  userId              String
  companionCardId     String
  sourceMessageId     String?             // null if user created the pin by hand
  text                LocalizedText       // short bilingual snippet
  createdAt           Instant
  pinnedByUser        Boolean             // true when user explicitly pinned
```

Rationale:
- Single-summary is the minimum viable bounded memory and matches the ST "Authors Note + summary extension" baseline expectation.
- Pinned facts give the user a handle on the companion's memory — they are the dial that fixes "the companion forgot X."
- Splitting summary from pins keeps summary regen idempotent (pins survive), and lets reset granularities map cleanly.

Alternatives considered:
- **Raw conversation log as memory**: rejected — unbounded context, forces client to trim.
- **Tiered (STM/MTM/LTM) with automatic promotion**: rejected for this slice — the promotion heuristics are not load-bearing until a conversation crosses ~50 turns, and landing them early would lock in choices we do not yet have signal for.
- **Embedding retrieval from conversation history**: rejected for this slice — adds a vector store dependency and query-time retrieval logic that the product does not yet need; the single-summary + pinned-facts layer is forward-compatible (retrieval would feed the same `memorySection` slot in the budget).

### 2. Preset shape: prompt sections + provider parameters, bundled

A Preset is:

```
data class Preset(
    val id: String,
    val displayName: LocalizedText,
    val description: LocalizedText?,
    val template: PresetTemplate,
    val params: PresetParams,
    val isBuiltIn: Boolean,        // true for the three seeded defaults
    val createdAt: Instant,
    val updatedAt: Instant,
    val extensions: JsonObject,    // forward-compat bag for ST preset fields not yet modeled
)

data class PresetTemplate(
    val systemPrefix: LocalizedText,         // inserted before persona.systemPrompt
    val systemSuffix: LocalizedText,         // inserted after persona.systemPrompt
    val formatInstructions: LocalizedText,   // tone + formatting expectations
    val postHistoryInstructions: LocalizedText, // inserted just before the user turn
)

data class PresetParams(
    val temperature: Double?,     // null means "use provider default"
    val topP: Double?,
    val maxReplyTokens: Int?,
)
```

Rationale:
- The four template sections map directly to how ST / Lobe-chat arrange system context around persona and history. Users migrating from those tools can recognize where their text goes.
- Naming the post-history slot `postHistoryInstructions` (neutral) instead of `jailbreak` or `NSFW unlock` avoids baking a community slur into our product copy while preserving the functional slot.
- Temperature / top-p / max-reply-tokens are the minimum controls users expect to flip from a preset; deeper controls (presence/frequency penalty, stop sequences) belong in a later refinement if we see demand.
- `extensions: JsonObject` parks imported ST preset fields we do not yet model (reserved for `sillytavern-card-interop`).

Alternatives considered:
- **Freeform single string as preset**: rejected — no way to decide where persona goes relative to user instructions; loses ordering.
- **Full ST-preset schema with every slot**: rejected for this slice — overfits to one tool's shape before we understand our own ordering rules.

### 3. Exactly one active preset, global scope

Per user, exactly one preset is active at any moment. There is no per-conversation or per-character override in this slice (the `tavern-experience-polish` slice adds per-character override later). The active preset id is stored as a user-profile column and returned on every bootstrap. Activating a different preset does not rewrite prior conversation history — it only affects future turn assemblies.

Rationale:
- Keeps the UX clear: one active preset is a single mental model.
- Per-character override is a product polish concern, not a foundation concern.

Alternatives considered:
- **Implicit active = most recently edited**: rejected — surprising and lossy.
- **Per-conversation active**: rejected for this slice — duplicates with per-character plans, and per-conversation granularity has no strong demand.

### 4. Default seed: three built-in presets

The backend seeds (and re-seeds idempotently on boot) three built-in presets. The following canonical template content is the authoritative source for the private backend's seeder — any change to these strings requires a corresponding spec/design edit here first.

**Preset `default` (id: `builtin-default`, `isBuiltIn=true`, temperature 0.7, topP 0.9, maxReplyTokens null):**

- `systemPrefix.english`: `"You are {{char}}, an AI companion. Respond in {{char}}'s voice and stay true to the persona described above. Keep continuity with prior pinned facts and the rolling summary, and address {{user}} naturally."`
- `systemPrefix.chinese`: `"你是 {{char}}，一个 AI 伙伴。请以 {{char}} 的口吻回应，并忠于上方的人物设定。保持与既有固定事实和滚动摘要的连续性，自然地称呼 {{user}}。"`
- `systemSuffix.english`: `""` (empty)
- `systemSuffix.chinese`: `""` (empty)
- `formatInstructions.english`: `"Reply in the user's active language. Use paragraphs for longer thoughts and keep the tone conversational."`
- `formatInstructions.chinese`: `"使用用户的当前语言回复。较长的想法请分段，保持自然的对话语气。"`
- `postHistoryInstructions.english`: `""` (empty — the default preset relies on persona + summary for continuity)
- `postHistoryInstructions.chinese`: `""` (empty)

**Preset `roleplay-immersive` (id: `builtin-roleplay-immersive`, `isBuiltIn=true`, temperature 0.9, topP 0.95, maxReplyTokens null):**

- `systemPrefix.english`: `"You are {{char}}. Remain fully in character at all times. Write as if living the scene with {{user}} — describe sensations, reactions, and internal thoughts using first-person perspective when natural. Honor the persona's voice, background, and current mood."`
- `systemPrefix.chinese`: `"你是 {{char}}。请始终保持角色一致。像真的身处场景中一样与 {{user}} 互动——在自然的时刻用第一人称描述感受、反应与内心活动。尊重角色的语气、背景和当前情绪。"`
- `systemSuffix.english`: `"Never narrate for {{user}} or decide their actions. Let the scene breathe — a pause or gesture can speak louder than exposition."`
- `systemSuffix.chinese`: `"绝不替 {{user}} 叙述或决定其行动。留白很重要——一次停顿或一个小动作有时胜过长篇独白。"`
- `formatInstructions.english`: `"Write in immersive prose. Avoid meta-commentary, disclaimers, and out-of-character asides. Mix action, dialogue, and inner thought; the narrative should feel lived, not reported."`
- `formatInstructions.chinese`: `"以沉浸式的散文风格书写。避免元评论、免责声明或脱离角色的旁白。混合动作、对白与内心独白；叙事应像亲历而非旁观复述。"`
- `postHistoryInstructions.english`: `"Reminder: you are {{char}}. Every response must continue the scene in {{char}}'s voice, preserving continuity with recent turns and any pinned facts."`
- `postHistoryInstructions.chinese`: `"提醒：你是 {{char}}。每一次回复都要以 {{char}} 的口吻延续场景，保持与近期对话和已固定事实的连贯。"`

**Preset `concise-companion` (id: `builtin-concise-companion`, `isBuiltIn=true`, temperature 0.6, topP 0.9, maxReplyTokens 320):**

- `systemPrefix.english`: `"You are {{char}}, a concise companion. Answer {{user}} warmly but briefly — lead with the point, trim filler."`
- `systemPrefix.chinese`: `"你是 {{char}}，一个简明扼要的伙伴。对 {{user}} 温暖但简短地回应——先点出要点，去掉多余的铺陈。"`
- `systemSuffix.english`: `""` (empty)
- `systemSuffix.chinese`: `""` (empty)
- `formatInstructions.english`: `"Cap replies at around two paragraphs or six sentences. Prefer short, direct prose. Avoid lists unless {{user}} explicitly asks for one."`
- `formatInstructions.chinese`: `"回复控制在约两段或六句以内。使用简短、直接的表达。除非 {{user}} 明确要求，否则不要使用列表形式。"`
- `postHistoryInstructions.english`: `""` (empty)
- `postHistoryInstructions.chinese`: `""` (empty)

These are marked `isBuiltIn=true` and cannot be deleted (the UI offers Duplicate instead). They can be cloned to a user-owned preset that is freely editable. The allocator (see §5) consumes these four template sections at priority slots 1 / 5 / 7 / 9 respectively, merged with the active companion card's persona fields.

Rationale:
- A non-empty library on first launch means the user has something to try immediately; "roleplay-immersive" covers the tavern-core use case and "concise-companion" covers the quick-chat use case.
- Built-in immutability keeps the bootstrap story deterministic.
- Publishing the template strings in design.md (rather than only in the backend seeder) keeps the source of truth shareable across the open-source client, the private backend, and the spec delta — if the backend ever regenerates the seed, reviewers can diff against this section.

### 5. Deterministic token-budget allocator (extends #7's assembler)

Each provider call has a soft target budget (defaults to 6000 tokens; configurable per-provider). The allocator composes sections in a fixed priority ordering. When the soft target would be exceeded, it drops sections in a fixed drop order, counting backwards from lowest priority, until the prompt fits:

Priority (highest first — these are preserved):
1. Active preset's `systemPrefix` + persona `systemPrompt` merged
2. Persona `personality` + `scenario`
3. Pinned facts (up to N, server-configurable cap)
4. Rolling summary
5. Active preset's `systemSuffix`
6. Recent-N turns, oldest dropped first
7. Active preset's `formatInstructions`
8. Persona `exampleDialogue`
9. Active preset's `postHistoryInstructions`
10. Current user turn (always preserved; never dropped — if the budget cannot fit the user turn alone, return `Failed` with reason `prompt_budget_exceeded`)

Drop order when over budget (lowest priority first):
- Persona `exampleDialogue` (verbose; model can infer tone from persona + summary)
- Older half of recent-N turns
- Rolling summary (keep pinned facts)
- Non-critical preset sections (`formatInstructions`, then `systemSuffix`)
- Never drop: pinned facts, persona `systemPrompt`, active preset's `systemPrefix`, the user turn

Rationale:
- Deterministic allocation is debuggable. Users can see why a reply mentioned or missed a detail.
- Pinned facts are load-bearing for the user's promise of "remember X" — they beat summary, because summary is auto-regenerated and lossy by nature.
- Dropping `exampleDialogue` first is the cheapest degradation: persona tone is still encoded in `systemPrompt` + `personality`.

Alternatives considered:
- **Weighted-retrieval instead of priority ordering**: rejected — non-deterministic, hard to debug.
- **Hard cap per section**: rejected — loses flexibility on small prompts.

### 6. Summarization trigger: deterministic + async

The backend regenerates the rolling summary when EITHER:
- `summaryTurnCursor` is at least N turns behind the latest completed turn (default N=8, per-provider configurable), OR
- The allocator projected budget exceeds a soft cap (default 75% of provider target).

Summarization runs asynchronously — the in-flight turn never waits for it. The next turn after a regen picks up the fresh summary. The summarizer is a dedicated provider call with a fixed prompt ("Given the current summary plus new turns N..M, produce an updated summary in ≤300 tokens in {active language}"). Failed summarization is a warning-level log; the prior summary remains.

Rationale:
- Async keeps turn latency predictable.
- Deterministic trigger (turns-since-last OR budget-exceeds) means users can predict when memory updates.
- Carrying the old summary forward on summarizer failure prevents regressions — the user never loses memory because of a transient provider hiccup.

Alternatives considered:
- **Per-turn summarization**: rejected — expensive and thrashes the summary prose.
- **Client-driven summarization**: rejected — violates service-boundary rule (provider key on client).

### 7. Three reset granularities

Memory reset controls expose three buttons:
- **Clear pinned facts** — removes all pins; summary remains.
- **Clear summary** — wipes summary; pins remain; summary cursor resets so the next turn retriggers summarization.
- **Clear all memory** — combined.

All three preserve the conversation transcript (unaffected). All three return the memory state to a "clean" baseline but do not reset the preset or the persona.

Rationale:
- The user's three likely intents — "forget the details but keep what I told it to remember" / "forget everything I told it" / "forget everything" — map directly to these buttons.
- Not wiping the transcript is important: the user's chat history is their record of the relationship.

### 8. Pin semantics: per-message, bilingual, editable

A pin originates either from:
- Tapping a "Pin" action on any past message bubble (user or companion) — the system copies the message text into both languages (using the content's source language as primary and a stub for the other language until summarizer/translator fills it), and stores `sourceMessageId`.
- Creating a pin manually from the memory panel — `sourceMessageId` is null, text is freely entered.

Pins are editable (inline) and deletable. Pin order in the allocator is "most recently pinned first"; older pins beyond the cap (default 20) drop quietly until the user deletes some.

Rationale:
- Pinning from a bubble is the ST "lorebook from chat" flow.
- Manual creation lets users park facts the companion has never said ("I'm allergic to peanuts").

### 9. UI surfaces: Settings for presets, chat for memory

Preset library management lives in the app's Settings screen (create, edit, duplicate, activate, delete). The active preset is also reachable one tap from the companion chat chrome so experimentation is cheap.

Memory review + reset live on a "Memory" panel scoped per companion (reachable from the companion chat chrome — e.g., the companion-detail drawer). Pin / unpin actions live on the message bubble context menu in the chat timeline.

Rationale:
- Presets are global — they belong with app-level settings, not inside a single conversation.
- Memory is per-companion — it belongs inside the companion's surface.
- Pinning from a bubble requires the action to live on the bubble.

### 10. Additive to `llm-text-companion-chat`

This slice does not redefine the submit or regenerate contracts. The only change to the existing assembler entry point is additive input:

- Backend reads the active preset and the companion's memory record in the assembly step, and feeds them into the same priority allocator.
- New WS events are not required; memory + preset state is served over HTTP only (bootstrap + change endpoints).
- Memory pin actions are sync HTTP calls, not streamed events — they are low-frequency user actions.

Rationale:
- Keeps `llm-text-companion-chat` stable and minimizes surface churn.
- Allows this slice to ship even if the companion turn runtime lands earlier.

## Risks / Trade-offs

- **Single-summary memory is lossy for long relationships** → Mitigation: pinned facts give the user a deterministic escape hatch. Follow-up slice can add tiered/episodic memory behind the same API slot without breaking clients.
- **Summarizer prompt drift or vendor differences** → Mitigation: summarizer prompt is server-owned and versioned alongside the allocator. Failed summarization preserves the prior summary. Users can manually edit the summary.
- **Token budget overruns** → Mitigation: deterministic drop order, `prompt_budget_exceeded` terminal with a typed reason when user turn alone cannot fit.
- **Preset "jailbreak" cultural baggage** → Mitigation: neutral field names (`postHistoryInstructions`) and neutral copy in UI. Imports can populate the slot; our UI does not name it.
- **Active-preset change mid-conversation confusing users** → Mitigation: UI surfaces the active preset in the chat chrome; activation is a single-tap but clearly visible. No retroactive rewrite of history.
- **Pinned-facts cap (20) is arbitrary** → Mitigation: configurable per-provider; surface a warning in UI when cap is hit. Out-of-cap pins are stored but not dropped into prompts.
- **Bilingual pin text** → Mitigation: the secondary-language pin text starts as a stub that the summarizer opportunistically fills; the user can inline-edit either side. Falling back to primary-language is acceptable.
- **Async summarization lag** → Mitigation: the next turn may see a stale summary. This is acceptable because pinned facts + recent-N turns still carry continuity.
- **Preset deletion while active** → Mitigation: delete of the currently active preset is blocked in UI; user must switch active first. Built-ins cannot be deleted.
- **Privacy of pinned facts** → Mitigation: pins are user-owned; reset clears them. They are never exported by default. Future export/import slice must make this opt-in.

## Migration Plan

1. Add `companion_memories`, `companion_memory_pins`, `companion_presets` tables in the backend. Seed the three built-in presets. Add `activePresetId` on the user profile.
2. Extend the allocator in `llm-text-companion-chat`'s assembly step to read memory + active preset and apply the priority allocator defined here. No client change required for turn assembly.
3. Add HTTP endpoints:
   - `GET /api/companions/{cardId}/memory`
   - `DELETE /api/companions/{cardId}/memory` with `?scope=pins|summary|all`
   - `GET/POST/PATCH/DELETE /api/companions/{cardId}/memory/pins` + `/{pinId}`
   - `GET/POST/PATCH/DELETE /api/presets` + `/{presetId}`
   - `POST /api/presets/{presetId}/activate`
4. Start the async summarizer worker with deterministic trigger.
5. Android side:
   - Add `CompanionMemory`, `CompanionMemoryPin`, `Preset` model types under `core/model`.
   - Add `CompanionMemoryRepository`, `CompanionPresetRepository` under `data/repository`.
   - Extend `ImBackendClient` + `ImBackendHttpClient` with the new endpoints and DTOs.
   - Wire repositories into `AppContainer`.
6. Android UI:
   - Settings screen: "Presets" section with library list, create/edit dialog, activation.
   - Companion chat chrome: active-preset pill (single-tap jump to Settings), "Memory" entry point to the Memory panel.
   - Memory panel: summary view (read-only prose), pinned-facts list with create/edit/delete, three reset buttons with confirm dialogs.
   - Chat bubble context menu: "Pin as memory" on any bubble.
7. Verification:
   - Unit: repositories, parsers, allocator priority under various over-budget scenarios, pin editing roundtrip, reset granularities.
   - Integration: a test conversation that crosses the summarization threshold and observes a fresh summary; preset swap across turns.
   - Instrumentation: memory panel render, pin from bubble, reset flow, preset library CRUD on `codex_api34`.
8. Record delivery evidence in `docs/DELIVERY_WORKFLOW.md` following the existing slice template.

Rollback strategy:
- If backend DB migrations land but allocator integration hits issues, the Android UI can render memory read-only (list existing pins, show summary) while pin/reset endpoints continue to work — the allocator degrades to the current "recent-N-turns only" behavior from `llm-text-companion-chat`.
- If the client-side UI ships partially, the backend fields remain hidden; nothing on the user-visible path regresses.
- Built-in presets are idempotent seeds; re-running the seed is safe.

## Deferred scope (captured now, implemented later)

These items reuse the memory + preset data model established here and do not require a schema change:

- **Tiered memory (STM / MTM / LTM)**: promote pinned facts or summary segments into a longer-lived tier after N turns of relevance; demote stale mid-term entries. Same API slot in the allocator; different backing store.
- **Episodic memory with embedding retrieval**: vector index over conversation chunks, queried at turn time; feeds the same `memorySection` in the allocator.
- **Automatic entity / relationship extraction**: after each turn, heuristic or LLM-driven extraction proposes pins for user approval. Extends the pin CRUD.
- **Per-character preset override**: delivered by `tavern-experience-polish`; a `characterPresetId` field on the card that shadows the active preset while the card is active.
- **Preset import / export (ST JSON)**: delivered by `sillytavern-card-interop`; maps the ST preset shape into our `Preset.extensions` bag + known fields.
- **Cross-device preset sync UI**: presets are server-persisted, but an explicit "sync" indicator + conflict resolution is a polish concern.
- **Usage analytics**: which preset is active, how often presets are swapped, how many pins per companion. Requires telemetry plumbing that is out of scope.
- **Public preset marketplace**: sharing and rating presets across users. Requires ToS/moderation work; not in this slice.

## Open Questions

- Default `recent-N` window when memory is enabled: currently the runtime uses N=10 (from `llm-text-companion-chat`). Should we reduce to N=6 once summary + pins are active? Default: keep N=10 for now; tune after observation.
- Pin cap: default 20. Should the cap be per-companion or per-user? Default: per-companion (what this slice ships). Revisit if users hit it.
- Summary language: should the summary be stored bilingually (both `en` and `zh`) always, or only in the active language at summarization time? Default: store in the active language at summarization time; the inactive side is backfilled lazily by the summarizer on the next trigger. Rationale: two summarizer calls per trigger doubles cost for little MVP gain.
- Preset editability of built-ins: should users be able to edit a built-in in place, or must they duplicate? Default: duplicate-only. Built-ins are documentation anchors.
- Should the memory panel show a chronological "turns folded since last regen" count, to help users understand freshness? Default: yes, a small subtitle ("Updated 12 turns ago"). Optional polish.
- Should pins carry tags or categories? Default: no in this slice — flat list only. Future polish can add tags.
- Should the allocator log which sections were dropped per turn (developer-visible)? Default: yes under a debug flag; not user-visible.
- Should deleting the conversation delete the memory? Default: no — the memory is scoped per user-companion pair, not per conversation. Deleting the card deletes the memory.
