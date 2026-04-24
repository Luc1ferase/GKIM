## Why

The Settings screen inherited its labels and organization from the pre-pivot phase when the app was a peer-IM shell with AIGC image generation bolted on. Section titles refer to "AIGC Provider" and "IM Validation", copy talks about message delivery and AIGC endpoints, and there is no acknowledgment of the companion chat as the primary product surface. That mismatch confuses users, leaks developer-facing terminology, and makes the upcoming Settings additions (preset library from `companion-memory-and-preset`, persona library from `user-persona`) feel like they belong somewhere else.

Separately, the chat UI's failure and safety states currently collapse into a generic bubble. `llm-text-companion-chat` introduced distinct `Thinking`, `Streaming`, `Completed`, `Failed`, `Blocked`, and `Timeout` lifecycle states, and demanded explicit UI for each, but the first-slice implementation only needed them to be visually distinguishable. The tavern product is going to face safety refusals and provider timeouts regularly, and users deserve specific guidance for each terminal — why it happened, whether retrying will help, and what they can change.

Lastly, the archived `companion-character-card-depth` capability still carries a `Purpose: TBD - created by archiving change deepen-companion-character-card. Update Purpose after archive.` stub. The archive completed more than three days ago; the Purpose should now reflect the capability's actual intent.

This slice consolidates three cleanup concerns into one coherent proposal: Settings reframe, safety/failure UI polish, and the Purpose stub fix.

## What Changes

- **BREAKING** The Settings screen is reorganized around companion-oriented sections: `Companion` (persona library + preset library + memory shortcut), `Appearance`, `Content & Safety` (content-policy acknowledgment + block-reason verbosity toggle), `AIGC Image Provider` (renamed from `AiProvider`, scoped explicitly to image generation), `Developer & Connection` (renamed from `ImValidation`, developer-oriented), and `Account`. Section labels and copy move away from "AIGC / IM Validation" terminology toward "companion / image / connection".
- Companion chat failure and safety bubbles get explicit per-terminal typography, copy, and action sets: `Failed` distinguishes transient (retryable) from permanent (non-retryable) with specific guidance per subtype; `Blocked` surfaces a bilingual reason from the typed reason set, a copy block explaining why, and a path back to composing a new turn; `Timeout` surfaces a dedicated "took too long" copy with a longer-wait retry affordance distinct from the generic `Failed` retry.
- The block-reason taxonomy is finalized as a stable closed set of typed keys (`self_harm`, `illegal`, `nsfw_denied`, `minor_safety`, `provider_refusal`, `other`) with bilingual localized copy per key. Backend emits these keys; Android maps them to user-facing copy via a shared localization table.
- The archived `companion-character-card-depth` capability's `Purpose` section is rewritten from the TBD stub to accurately describe the capability's scope and intent.

## Capabilities

### New Capabilities
- `companion-settings-and-safety-reframe`: Defines the reorganized Settings navigation, the finalized block-reason taxonomy, the per-terminal failure/safety copy contract, and the content-policy acknowledgment flow.

### Modified Capabilities
- `core/im-app`: The Android app requirements change so Settings navigation is companion-oriented, so chat surfaces render per-terminal explicit states with distinct copy + actions, and so a content-policy acknowledgment is surfaced on first launch.
- `im-backend`: The backend requirements change so the block-reason taxonomy is a stable closed set of typed keys and the content-policy acknowledgment state is persisted server-side per account.

## Impact

- Affected Android code: `feature/settings/SettingsRoute.kt` (section reorganization, copy rewrites, new "Companion" + "Content & Safety" sections, renames), `feature/chat/ChatRoute.kt` + its `ChatMessageRow` bubble rendering for `Failed` / `Blocked` / `Timeout` terminals (copy + action handlers per subtype), new `core/model/BlockReason.kt` + localized copy table under `core/designsystem`, new `feature/settings/ContentPolicyAcknowledgmentRoute.kt` for first-launch acknowledgment.
- Affected backend contract: finalize the block-reason typed-reason set emitted on `companion_turn.blocked`, add content-policy acknowledgment persistence (`POST /api/account/content-policy-acknowledgment`, `GET /api/account/content-policy-acknowledgment`).
- Affected specs: new `companion-settings-and-safety-reframe`, plus deltas for `core/im-app` and `im-backend`. The `companion-character-card-depth` Purpose text is updated directly in `openspec/specs/companion-character-card-depth/spec.md` as a tasks.md step during archival of this slice (not via a capability delta, since Purpose lives outside the requirements shape).
- Affected UX: Settings feels companion-first instead of IM-first, chat failure/safety bubbles give users actionable guidance, and the archived Purpose stub no longer blocks future spec readers.
- Non-goals (scoped out of this slice):
  - A full content-policy document / legal review (→ legal; this slice ships the acknowledgment plumbing, not the policy text itself — we ship with placeholder copy until legal signs off)
  - Per-user safety tier (strict / balanced / permissive) — the taxonomy supports it but the UI is deferred
  - Report-abuse / flag-reply UI (→ future safety slice)
  - Rewriting AIGC image generation flows beyond the section rename (→ out of scope)
  - Developer diagnostics surface (crash report upload, log export) beyond the existing IM validation plumbing
  - Rewriting `pivot-to-ai-companion-im`'s safety requirements (this slice is additive polish on top)
  - Localization of the content-policy acknowledgment copy to languages beyond `en` / `zh`
