## Context

Three cleanup concerns bundle into one slice because they all ship at the same layer (Settings + chat presentation) and would each be too small to justify their own proposal:

1. **Settings reframe**: `SettingsRoute.kt` currently has sections named `Appearance`, `AiProvider`, `ImValidation`, `Account`, reflecting the peer-IM + AIGC shell era. The new tavern reality means Settings needs to host a Preset library (from `companion-memory-and-preset`), a Persona library (from `user-persona`), and shortcuts to the per-companion Memory panel — none of which fit the current hierarchy. The AIGC image generation flow still exists and needs a dedicated slot, but the section label should stop calling it "AiProvider" as if it were THE provider. The IM validation developer flow needs to stop living under a user-facing label.

2. **Failure / safety UI polish**: `llm-text-companion-chat` ships the lifecycle data model and requires bubbles to render distinct visuals for each terminal. This slice fills in the copy, per-subtype actions, and the block-reason localization table so users get actionable guidance instead of an opaque failure bubble. Without this slice, the product fails the "explicit safety and timeout outcomes" quality bar the pivot explicitly called out.

3. **Purpose stub fix**: the archived `companion-character-card-depth` spec carries a literal `Purpose: TBD` placeholder. Over time, stubs like this erode spec trust because a reader cannot tell if the TBD means "actually undefined" or "just not written yet". Bundling the fix with this slice clears the last unfinished thread from the `deepen-companion-character-card` archival.

Constraints:
- Public repo boundary: backend source stays private; this slice defines the block-reason taxonomy as a contract.
- Service boundary (from `core/im-app`): no new client-side safety logic — the backend decides blocks; the client only maps typed reasons to localized copy.
- Bilingual contract (from `localize-companion-tavern-copy`): every user-facing copy line in the new Settings sections, bubble states, and block reasons must be bilingual.
- Forward-compatible: the block-reason taxonomy is a closed enum with an `other` escape hatch, so future new reasons can be added without breaking clients.
- Additive in codebase: do not rip out existing AIGC image gen flows; only rename the section and adjust copy.

## Goals / Non-Goals

**Goals:**
- Reorganize Settings into `Companion`, `Appearance`, `Content & Safety`, `AIGC Image Provider`, `Developer & Connection`, `Account` sections with companion-oriented copy.
- Provide distinct bubble copy + action affordances for `Failed`-transient, `Failed`-permanent, `Blocked` (per typed reason), and `Timeout` terminals.
- Finalize a closed block-reason taxonomy and ship a shared localization table.
- Add a first-launch content-policy acknowledgment surface with server-persisted acknowledgment state.
- Replace the `Purpose: TBD` stub in `companion-character-card-depth` with accurate text.

**Non-Goals:**
- Legal content-policy text authoring (ship placeholder copy; legal sign-off is a separate workflow).
- Per-user safety tier selection (strict / balanced / permissive); taxonomy is stable but UI is deferred.
- Report-abuse or flag-reply UI.
- AIGC image generation refactor beyond the section rename.
- Developer diagnostics upload (crash logs, log export).
- Localization beyond `en` / `zh`.

## Decisions

### 1. Settings section reorganization

New section order + labels:

- **Companion**
  - Persona library (from `user-persona`)
  - Preset library (from `companion-memory-and-preset`)
  - Memory (link through to the per-companion memory panel from the active companion; the entry here opens a chooser if there are multiple recently used companions)
- **Appearance**
  - Theme mode
  - Language
- **Content & Safety**
  - Content-policy acknowledgment state (read-only; "Accepted on 2026-04-21" or "Not accepted — read policy")
  - Block-reason verbosity toggle ("Show reason text" / "Show neutral notice only"; default: show reason text)
- **AIGC Image Provider**
  - Existing provider list + custom provider config
  - Copy rewritten to clarify this is for image generation only, not companion chat
- **Developer & Connection**
  - IM backend origin override (dev builds only)
  - Dev user external id
  - Connection validation action
  - Gated behind `BuildConfig.DEBUG` where it already is
- **Account**
  - Current account info
  - Sign out / switch account

Rationale:
- Puts the tavern-core controls (persona/preset/memory) at the top as the primary user surface.
- Separates image provider concerns from chat provider concerns (chat provider is server-owned; image provider is client-owned).
- Hides developer tooling under a clearly developer-oriented label.
- Keeps Account at the bottom where it is conventionally expected.

Alternatives considered:
- **Flatten everything into one scrolling list** (ST-style): rejected — too many sections, users lose orientation.
- **Tabbed navigation**: rejected — existing mobile convention is the vertical list with sub-routes, and the tab complexity is not earned.

### 2. Block-reason taxonomy (finalized closed set)

```
enum class BlockReason {
    SelfHarm,          // wire key: "self_harm"
    Illegal,           // wire key: "illegal"
    NsfwDenied,        // wire key: "nsfw_denied"
    MinorSafety,       // wire key: "minor_safety"
    ProviderRefusal,   // wire key: "provider_refusal"
    Other,             // wire key: "other" (fallback)
}
```

Bilingual localization table lives in `core/designsystem/BlockReasonCopy.kt`:
- `SelfHarm`: "This conversation involves self-harm or suicide-related content. If you need real support, please reach out to a local helpline." / Chinese equivalent.
- `Illegal`: "This content isn't allowed because it touches on illegal activity." / Chinese equivalent.
- `NsfwDenied`: "The current provider or policy doesn't allow this content." / Chinese equivalent.
- `MinorSafety`: "This content is restricted to protect minors." / Chinese equivalent.
- `ProviderRefusal`: "The AI provider declined to generate this reply." / Chinese equivalent.
- `Other`: "This reply was blocked. Try rephrasing or choosing a different direction." / Chinese equivalent.

Backend emits the wire keys in `companion_turn.blocked` events. Client maps the wire key to the `BlockReason` enum (fallback to `Other` on unknown) and then to the localized copy.

Rationale:
- Closed set + `Other` escape hatch is the standard forward-compatible pattern.
- Localization lives client-side so no round-trip is needed for copy.
- The per-reason copy follows a neutral, non-graphic tone — we do not echo the blocked content back at the user.

Alternatives considered:
- **Backend sends full localized copy**: rejected — it becomes hard to keep tone consistent across reasons, and the backend does not know the user's active `AppLanguage`.
- **Free-form reason string from provider**: rejected — we do not want raw provider strings leaking into UI; the typed taxonomy is the product contract.

### 3. Per-terminal bubble copy + actions

| Terminal | Copy pattern | Actions |
|---|---|---|
| `Failed` (transient) | "Something went wrong. Please try again." | Retry (re-submit the same user turn) |
| `Failed` (permanent, e.g., `prompt_budget_exceeded`) | Per-subtype: "Your message is longer than the model can handle. Please shorten it and try again." | Edit the user turn (no retry until edited) |
| `Blocked` | Per-`BlockReason`: localized copy from the table above | No retry; "Compose a new message" CTA |
| `Timeout` | "The AI took too long to respond. Please try again." | Retry with longer-wait affordance (server side extends the idle bound by 50%); also a "Switch preset" suggestion if the current preset has very high `maxReplyTokens` |

`Failed` subtypes are conveyed by a typed reason field on the failure event. For this slice we support:
- `transient` (default; retry expected to help)
- `prompt_budget_exceeded` (from `companion-memory-and-preset`)
- `authentication_failed`
- `provider_unavailable`
- `network_error`
- `unknown` (generic fallback)

Each subtype has its own bilingual copy table and its own action set.

Rationale:
- Explicit per-subtype copy avoids the "something went wrong" trap where users retry pointlessly.
- Giving `Blocked` a "Compose a new message" affordance instead of a retry prevents retry loops on unbreakable safety filters.
- `Timeout` with a longer-wait retry is a gentle degradation; switching preset is a hint, not a forced action.

### 4. Content-policy acknowledgment

On first successful launch after an account is created (or upgraded), the app routes the user through a one-screen `ContentPolicyAcknowledgmentRoute`. The screen presents the policy as a scrollable text block (placeholder copy for now; legal will swap in real text later) with an "I accept" CTA. Tapping accept posts `POST /api/account/content-policy-acknowledgment` with the policy version. The backend persists the acknowledgment (version + timestamp) on the account. On subsequent launches, the backend returns the acknowledgment state, and the app skips the route unless the policy version has bumped.

Rationale:
- Server-persisted acknowledgment survives reinstall.
- Version-gated re-acknowledgment supports future policy updates.
- Placeholder copy now + legal sign-off later is a legitimate ship path.

Alternatives considered:
- **Click-through banner on every launch**: rejected — annoying and doesn't prove a real acknowledgment.
- **In-app dialog on the first offensive reply**: rejected — the policy should be up front.

### 5. Companion-character-card-depth Purpose rewrite

The TBD stub is replaced with:

> Captures the companion character card as a full persona authoring record — system prompt, personality, scenario framing, example dialogue, first-message greeting, alternate greetings, tags, creator attribution, character version, and a forward-compatible extensions bag — using the bilingual `LocalizedText` contract for every prose field. Defines the authoring surfaces (tavern character detail, character editor) and the ownership + mutability rules for user-authored, preset, and draw-acquired cards.

This text is applied to `openspec/specs/companion-character-card-depth/spec.md` as a task.md step during archival. It is NOT a delta file because openspec deltas cover requirements; Purpose lives outside the requirement shape.

Rationale:
- The text accurately reflects the archived capability's requirements.
- Doing it as a tasks.md manual step during archival keeps the delta files clean.

### 6. Copy and localization hygiene

All new copy (section labels, placeholder policy, block-reason strings, failure/timeout strings, settings item descriptions) goes through the existing `LocalizedText` + `AppLanguage` plumbing. A single `core/designsystem/SafetyCopy.kt` file centralizes the safety-specific copy so updates do not require hunting across the feature tree.

Rationale:
- Existing convention in the repo is centralized copy tables; this slice follows it.
- Centralization simplifies future legal-driven copy updates.

## Risks / Trade-offs

- **Placeholder policy copy going to production** → Mitigation: the acknowledgment screen renders copy from a dedicated resource so legal can swap it without a code change later. We flag the placeholder nature in the commit message + delivery record.
- **Block-reason copy feeling cold** → Mitigation: the copy passes through a product-voice review before commit; the per-reason tables are small enough to iterate on without churn.
- **Existing AIGC users confused by section rename** → Mitigation: the AIGC Image Provider section retains its content verbatim; only the header label changes. A one-line caption ("For image generation. Companion chat uses a backend provider.") clarifies scope.
- **Developer section being visible to end users** → Mitigation: the section is gated behind `BuildConfig.DEBUG` as it already is; the rename only changes the section label, not the gating.
- **Content-policy re-acknowledgment loop** → Mitigation: the acknowledgment is keyed on policy version; changing version is a deliberate action. If the version is accidentally bumped, users re-acknowledge; no data loss.
- **Block-reason enum additions breaking old clients** → Mitigation: clients map unknown wire keys to `BlockReason.Other` and surface the generic copy. Server-side introduction of a new key therefore degrades gracefully.
- **Purpose text getting stale** → Mitigation: a tasks.md line item in future archive slices encourages updating Purpose when requirements shift.

## Migration Plan

1. Update `openspec/specs/companion-character-card-depth/spec.md` Purpose section to the text in Decision #5.
2. Finalize the block-reason wire-key taxonomy and publish it in the delta spec.
3. Android side:
   - Add `core/model/BlockReason.kt` + `core/designsystem/BlockReasonCopy.kt`.
   - Add `core/designsystem/SafetyCopy.kt` with failure subtype copy.
   - Extend `feature/chat/ChatRoute.kt` bubble rendering for per-subtype Failed, per-reason Blocked, dedicated Timeout.
   - Reorganize `feature/settings/SettingsRoute.kt` sections + copy. Add section routes as needed.
   - Add `feature/settings/ContentPolicyAcknowledgmentRoute.kt` + the bootstrap gating that routes first-time users through it.
4. Backend:
   - Finalize the wire-key taxonomy in the block event emitter.
   - Add `POST/GET /api/account/content-policy-acknowledgment`.
5. Verification:
   - Unit: `BlockReason` parse-unknown-fallback, `SafetyCopy` table coverage, Settings section render, acknowledgment route navigation.
   - Integration: a blocked turn event with each typed reason renders the corresponding localized copy; a transient vs permanent failed event renders the corresponding action set.
   - Instrumentation: first-launch acknowledgment flow, Settings sections navigation, bubble rendering per terminal.
6. Record delivery evidence in `docs/DELIVERY_WORKFLOW.md`.

Rollback strategy:
- Settings reorganization is Android-only; rolling back is a UI change.
- Block-reason taxonomy additions are backward-compatible if clients fall back to `Other`.
- Content-policy acknowledgment can be bypassed server-side (skip the gating) without schema rollback if the route surfaces issues.

## Deferred scope (captured now, implemented later)

- **Per-user safety tier** (strict / balanced / permissive): the taxonomy supports it; UI and storage are deferred.
- **Report-abuse / flag-reply**: separate safety slice; needs moderation workflow.
- **Crash-report upload / log export**: separate developer-tooling slice.
- **AIGC image generation refactor**: separate concern; the section rename is all this slice covers.
- **Legal-reviewed policy copy**: dependent on legal workflow; the plumbing ships now.
- **Localization beyond en / zh**: future i18n slice.
- **Per-card safety profile**: per-card provider / safety overrides fold into `tavern-experience-polish`.

## Open Questions

- Should the Companion section link directly to the per-companion memory panel for the "active" companion, or should it open a chooser? Default: chooser with a shortcut to the active companion at the top. Simpler.
- Should the content-policy acknowledgment be skippable on dev builds? Default: yes for `BuildConfig.DEBUG`. Shipping builds must acknowledge.
- Should block-reason copy include a "Learn more" link into the content policy? Default: yes — a single link at the bottom of the bubble per-reason. Non-blocking polish.
- Should the `Blocked` bubble hide the "Compose a new message" CTA on repeat blocks (to avoid looping)? Default: no — users should always have a way to move forward; we log repeat blocks but do not remove the affordance.
- Should the `Failed`-permanent subtype list include `prompt_budget_exceeded` as user-facing? Default: yes — the copy is actionable ("shorten your message"). Users understand length limits.
