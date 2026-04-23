## Context

The AI companion branch already establishes a broader product pivot away from human-first IM, but the concrete third-tab experience is still inherited from the old `Space` feed: discovery filters, developer posts, and prompt cards. The user now wants that surface replaced with something closer to tavern-style character apps, where the user can enter a dedicated role lobby, choose from preset characters, or draw random character cards that can then become active companions.

This is not just a label change. The new surface has to replace the existing content/feed assumptions, introduce character-card mental models, and connect active character selection to the companion conversation system. It also needs to stay coherent with the branch-level AI companion pivot: characters are not cosmetic only; they define the persona the user is talking to.

## Goals / Non-Goals

**Goals:**
- Replace the current `Space` feed with a tavern-style role-selection and draw experience.
- Let users browse preset companion角色 and also obtain角色卡 through a draw flow.
- Preserve a clear “owned roster” so users can return to previously acquired characters and pick the active one explicitly.
- Ensure the selected角色 drives the companion conversation identity instead of being a disconnected collection UI.

**Non-Goals:**
- Do not introduce paid monetization, real-money gacha, or probability-driven spend systems in this slice.
- Do not build a full public marketplace, trading system, or user-generated card publishing pipeline yet.
- Do not redesign the rest of the branch’s inbox/chat architecture beyond what is needed to route into character-led companion dialogue.

## Decisions

### 1. Replace the third tab with a dedicated tavern/character lobby instead of embedding character selection inside settings

The current `Space` destination should be repurposed into a dedicated tavern-style surface, rather than leaving `Space` in place and hiding character selection behind secondary menus.

Why this decision:
- The user explicitly wants the old `Space` feature replaced, not supplemented.
- A primary-tab surface matches the importance of character choice in companion products.
- It gives role cards, draw entry, and owned roster enough space to feel like a core product loop instead of a settings detail.

Alternatives considered:
- Keep `Space` and add a small character drawer inside it: rejected because the feed metaphor would still dominate.
- Move character choice entirely into chat/settings: rejected because it hides the branch’s core product differentiator.

### 2. Use tavern-style character cards with two acquisition paths: preset and drawn

The lobby should present character cards with persona-facing metadata, and the app should support two clear paths:
- preset characters that are always available
- draw-acquired characters that enter the user’s owned roster after a draw result

Why this decision:
- It matches the requested “预设角色 + 自抽卡角色” split.
- It avoids making draw-only access the sole route into the product.
- The card mental model fits the “酒馆应用” reference more naturally than list rows or plain avatars.

Alternatives considered:
- Preset-only selection: rejected because it drops the requested draw loop.
- Draw-only access: rejected because it makes first-run onboarding weaker and overly random.

### 3. Keep draw behavior transparent and non-monetized in the first slice

The first implementation should treat draws as a product mechanic for discovery and ownership, not as a monetization system. The app should make it clear whether a draw produced a new card or a duplicate-like result, and should not imply paid rarity economics that the product does not actually support yet.

Why this decision:
- It keeps the branch focused on companion UX, not payment or economy complexity.
- It avoids misleading “gacha” expectations before the product has any spend or progression system.

Alternatives considered:
- Full rarity/economy design now: rejected because it would explode scope and blur the core companion goal.

### 4. Persist owned roster state and active character selection on the backend

The backend should own the preset catalog, draw outcome history, owned character roster, and active selected角色 per user, so the same character identity can drive durable companion memory and conversation continuity.

Why this decision:
- The selected角色 is part of the conversation contract, not a local visual preference.
- Backend ownership keeps state durable across reinstall, multi-device, and reconnect scenarios.
- It fits the branch-level decision that companion orchestration lives server-side.

Alternatives considered:
- Purely local roster selection in DataStore/Room: rejected because it breaks durable persona continuity and future multi-device behavior.

## Risks / Trade-offs

- [Replacing `Space` removes an existing surface that still has code/tests behind it] → Mitigation: explicitly rewrite the product contract and associated tests instead of leaving half-removed feed behavior.
- [Draw mechanics can feel deceptive if rewards are unclear] → Mitigation: keep first-slice draw behavior transparent, non-paid, and explicit about ownership outcomes.
- [Character selection could become visually rich but disconnected from actual chat behavior] → Mitigation: require active角色 selection to drive the companion conversation identity and startup path.
- [Backend persistence adds more product state to an already evolving companion branch] → Mitigation: scope the first backend slice to catalog, ownership, and active-selection durability rather than every future progression mechanic.

## Migration Plan

1. Replace the current `Space` product contract with tavern/角色-lobby requirements.
2. Define character-card, draw-result, owned-roster, and active-selection domain models.
3. Rebuild the Android third tab around preset cards, draw entry, owned roster, and role activation.
4. Add backend support for character catalogs, draw results, and active selection state.
5. Verify that selecting or drawing a角色 can drive the intended companion conversation path.

Rollback strategy:
- Keep the work isolated to the AI companion branch.
- If the tavern replacement proves too disruptive, the branch can temporarily fall back to the existing `Space` feed while preserving the change artifacts for a narrower retry.

## Open Questions

- In the first slice, should the third tab label become `酒馆`, `角色`, or another character-lobby name?
- Should a draw immediately activate the newly obtained角色, or only add it to the owned roster?
- How many preset characters should be guaranteed available before the user uses any draw mechanic?
