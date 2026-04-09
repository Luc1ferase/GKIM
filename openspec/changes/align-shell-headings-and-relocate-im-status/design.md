## Context

The current top-level Android shell uses three slightly different header compositions. `Messages` renders a custom row with a `titleLarge` heading and then inserts a full live IM status card immediately below it. `Contacts` already uses a title-only `PageHeader`, but the sort dropdown sits on a separate trailing row, which pushes the first contact card lower than necessary. `Space` uses a title-only header again, but because the other tabs use different headline scales and top-band structures, the shell feels visually inconsistent when switching between tabs.

This change is intentionally presentation-focused. It does not alter conversation data, contact sorting behavior, IM endpoint persistence, or backend contracts. It only changes where visible status and top-level controls are rendered. The IM validation state already exists in the messaging repository and the Settings flow already owns IM endpoint inputs, so the main design question is how to recompose those existing pieces without creating duplicated status logic.

## Goals / Non-Goals

**Goals:**
- Align the primary heading position and visual scale for `Recent conversations / 最近对话`, `Contacts / 联系人`, and `Space / 空间`.
- Move the Contacts sort dropdown into the same top band as the Contacts title so the list begins higher on the screen.
- Remove the live IM status card from Messages and render equivalent status feedback inside `Settings > IM Validation`.
- Keep the implementation small, UI-focused, and backed by targeted Android UI regression coverage.

**Non-Goals:**
- Changing contact sorting options or sort persistence behavior.
- Changing IM validation semantics, endpoint storage, or repository integration phases.
- Redesigning the whole Settings information architecture.
- Reopening unrelated Messages, Contacts, or Space content decisions outside top-band layout and status placement.

## Decisions

### 1. Use one shared top-band pattern for the three primary tabs
Messages, Contacts, and Space should all present their large heading with the same top padding and headline typography. The implementation can use a small shared composable or a lightweight extension of existing header primitives, but the outcome should be one visual rhythm instead of three separate ad hoc layouts.

Why this decision:
- It solves the cross-tab inconsistency at the source instead of tuning each screen independently.
- It keeps future shell polish work easier because title spacing and scale live behind one pattern.

Alternatives considered:
- Tweak each screen independently: rejected because the three tabs would drift again over time.
- Force every screen through the existing `PageHeader` API exactly as-is: rejected because Contacts needs a trailing interactive sort control that is richer than the current simple text action.

### 2. Keep the Contacts sort control inline with the title row
The Contacts dropdown should remain a compact pill, but it should move into the same top row as the `Contacts / 联系人` heading. The list should then start immediately below the title band without an extra spacer row dedicated only to sorting.

Why this decision:
- It satisfies the user request directly.
- It preserves the current compact dropdown interaction while reclaiming vertical space for contact content.

Alternatives considered:
- Replace the dropdown with a full-width segmented control: rejected because it makes the toolbar heavier and changes behavior more than necessary.
- Keep the dropdown on its own row but reduce spacing: rejected because it still leaves the sort control visually detached from the page title.

### 3. Treat live IM status as validation chrome owned by Settings, not Messages
The live IM integration status should stop rendering as a standalone card on Messages. Instead, `Settings > IM Validation` should render the current repository integration phase/message near the existing IM endpoint inputs so connection troubleshooting stays where the operator already configures those values.

Why this decision:
- The status is operational/debug information, not the primary job of the Messages tab.
- It reduces clutter on Messages while keeping the status accessible in the place users already go for IM validation setup.

Alternatives considered:
- Keep the Messages card and duplicate status in Settings: rejected because duplicated status surfaces are harder to maintain and can drift.
- Hide the status entirely unless there is an error: rejected because the current validation workflow benefits from explicit readiness/connection feedback in Settings.

## Risks / Trade-offs

- [A shared top-band abstraction could become over-generalized] → Keep the abstraction minimal and scoped to the current shell-title use case with optional trailing content only if needed.
- [Moving IM status out of Messages may reduce passive visibility for debugging] → Render the status clearly inside `Settings > IM Validation`, where endpoint configuration and troubleshooting already happen.
- [Touching three top-level tabs at once could create subtle spacing regressions] → Cover the changed layout with focused Android UI tests that check title presence, IM status relocation, and the Contacts toolbar/list relationship.

## Migration Plan

1. Add or update regression coverage for the Contacts toolbar placement, top-level title consistency expectations, and IM status relocation.
2. Refactor Messages, Contacts, and Space to use the aligned top-band pattern, including the inline Contacts sort control.
3. Move the live IM status presentation into `Settings > IM Validation`, keeping the existing validation inputs and repository state source.
4. Run focused Android verification, record the required evidence in the change tasks, and leave rollback as a straightforward revert of the affected UI composition changes.

Rollback strategy:
- Revert the shell-layout and IM-status presentation changes without touching the underlying repository state or settings persistence logic.

## Open Questions

- Should the Settings menu summary for `IM 验证` also reflect live connection state, or is surfacing the detailed status only inside the IM Validation screen sufficient for this slice?
