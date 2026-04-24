## Context

The repository already contains a shipped Android-native IM shell, a realtime Rust backend, and local/provider-driven AI settings, but those pieces are still framed around social messaging plus separate AIGC tooling. The new branch `feature/ai-companion-im` is meant to fork that existing foundation into a product whose primary value is AI companion dialogue, emotional continuity, and safe long-lived conversation instead of generic user-to-user IM.

This is a cross-cutting product pivot, not a single-screen feature. The implementation has to reuse as much of the current IM transport, navigation, persistence, and release machinery as possible while redefining the app and backend contracts around AI companion identities, durable memory, and realtime companion response lifecycle.

## Goals / Non-Goals

**Goals:**
- Reuse the current Android IM shell and backend transport as the technical base for an AI companion conversation product.
- Make AI companions first-class conversation participants with stable persona identity, relationship context, and bounded memory continuity.
- Move companion orchestration, memory updates, and reply lifecycle to the backend so the experience remains durable, consistent, and recoverable across devices/sessions.
- Keep the branch isolated from the current product line so companion-focused work can move without forcing the main branch into an immediate product rewrite.

**Non-Goals:**
- Do not create a brand-new repository or discard the existing Android/realtime foundation.
- Do not solve every future companion feature now, such as voice calls, avatar animation, multi-agent rooms, or open-ended creator platforms.
- Do not fully remove human IM/social code paths in the first design pass if they are still useful as transitional infrastructure.
- Do not commit provider secrets or collapse the current delivery/release workflow.

## Decisions

### 1. Use the existing repository as the technical fork point, but isolate the product pivot on a dedicated branch

The AI companion direction starts from the current repo and current branch state, but the work proceeds on `feature/ai-companion-im` so the product contract can shift aggressively without blocking or destabilizing the current line of work.

Why this decision:
- The existing repo already contains the hardest foundational pieces: Android runtime shell, realtime transport, backend durability, and release automation.
- A dedicated branch gives the team room to reshape product assumptions without treating every pivot decision as a mainline compatibility problem.

Alternatives considered:
- Pivot `master` directly: rejected because the product direction is too large and would mix exploratory product work with the current line.
- Start a greenfield repository: rejected because it would throw away the existing IM and release foundation that this request explicitly wants to reuse.

### 2. Model AI companions as first-class conversation participants inside the existing IM architecture

Companions should live in the same core conversation system as other chat threads, but with companion-specific metadata such as persona, mood, memory summary, response state, and safety policy.

Why this decision:
- It reuses existing conversation list, chat detail, persistence, and realtime delivery patterns instead of inventing a separate conversation stack.
- Companion turns, reconnect recovery, and unread behavior can ride on the current IM data flow with targeted extensions.

Alternatives considered:
- Build a separate “AI chat” subsystem unrelated to IM: rejected because it duplicates transport, state, and navigation concerns already solved in the repo.
- Fake companions as purely local/demo personas on-device: rejected because long-lived memory, multi-session continuity, and policy control belong on the backend.

### 3. Keep companion orchestration and memory management server-side

The backend should own persona prompts, bounded memory summaries, turn state, safety guardrails, and provider calls, while the Android app acts as the real-time client for companion dialogue.

Why this decision:
- It keeps prompts, provider choices, and safety policy out of the APK.
- It makes pending turn recovery, memory continuity, and future multi-device access possible.
- It aligns with the repo’s current service-boundary rule: clients talk to HTTP/WebSocket services, not directly to protected infrastructure.

Alternatives considered:
- Generate all companion replies on-device: rejected because secrets, policy consistency, and continuity become brittle.
- Push only final companion replies from the backend: rejected because typing/thinking/streaming lifecycle is part of the emotional UX and should be reflected explicitly.

### 4. Reframe existing AI tooling as companion controls instead of separate creator-first entry points

Current provider/model configuration and parts of the workshop/settings surface should be retained where useful, but reoriented toward companion personality, model selection, memory behavior, and relationship tuning rather than general-purpose generation-first workflows.

Why this decision:
- The repository already has AI-oriented configuration work that can be reused.
- Product coherence improves when AI settings directly support the companion experience instead of feeling like a separate experimentation lab.

Alternatives considered:
- Keep the current Space/workshop/AIGC framing unchanged: rejected because it dilutes the companion-first product story.
- Remove all AI tooling surfaces immediately: rejected because some of that machinery is the fastest path to configurable companion behavior on this branch.

## Risks / Trade-offs

- [The pivot scope is broader than a normal feature] → Mitigation: keep the first implementation centered on companion personas, conversation lifecycle, and bounded memory rather than every future companion modality.
- [Existing social IM flows may conflict with companion-first UX] → Mitigation: treat human-social flows as transitional infrastructure and explicitly decide which surfaces stay visible in the branch.
- [Backend orchestration adds latency and failure states] → Mitigation: define reply lifecycle events, pending-turn recovery, and fallback/error surfaces in both app and backend contracts.
- [The dirty worktree inherited into the new branch could blur product-pivot work with existing unpublished changes] → Mitigation: keep future commits scoped and use the OpenSpec tasks as the branch-local source of truth for companion work.

## Migration Plan

1. Use `feature/ai-companion-im` as the dedicated branch for the product pivot.
2. Add the companion capability plus Android/backend deltas that redefine the product contract.
3. Rework Android inbox/chat/settings flows so companion conversations become the primary experience.
4. Extend backend persistence and realtime orchestration for persona state, memory summaries, and companion reply lifecycle.
5. Run focused Android/backend verification and capture delivery evidence before treating the pivot as accepted.

Rollback strategy:
- Keep the pivot isolated to the dedicated branch so abandoning or reshaping the direction does not require reverting the current product line.
- If a specific companion implementation slice fails, fall back to the prior IM foundation while preserving the branch-local specs and proposal for the next attempt.

## Open Questions

- In the first branch milestone, should human-to-human conversations remain visible in the inbox, or should the UI become companion-only immediately?
- Should the initial companion product support one flagship persona or a small selectable roster?
- How much of the existing Space/workshop surface should survive as companion discovery/customization versus being collapsed into messages/settings?
