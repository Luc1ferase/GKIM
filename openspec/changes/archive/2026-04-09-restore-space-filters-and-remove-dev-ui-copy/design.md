## Context

`refresh-shell-space-and-auth-onboarding` merged Workshop discovery into the `Space` experience and simplified the filter row down to `为你推荐` and `提示工程`. That merge achieved the structural goal, but the resulting page chrome drifted in two ways: first, `AI 工具` and `动态` disappeared even though they still matter as discovery entry points; second, the page now contains supporting copy such as `创作者动态`, a long developer-oriented descriptive sentence, and the `未读信号` summary card that reads more like internal scaffolding than product UI.

This follow-up should stay narrowly focused on presentation. It does not reopen the merged-feed architecture, prompt application flow, or unread-count data model. The point is to refine what the user sees, not to unwind the previous information-architecture work.

## Goals / Non-Goals

**Goals:**
- Restore the four-item `Space` discovery filter row.
- Remove the top-level unread summary card from the `Space` surface.
- Simplify the `Space` header to production-facing product chrome by removing eyebrow and developer-explanatory copy.
- Encode a clear product rule that shipped UI should avoid development-stage commentary in visible app surfaces.

**Non-Goals:**
- Reintroducing a separate `Workshop` page or header button.
- Changing prompt application behavior or the mixed-feed composition model.
- Altering the underlying unread-count data tracked elsewhere in messaging or space state.
- Performing a whole-app copy audit in this single change.

## Decisions

### 1. Treat the `Space` filter row as a four-entry discovery rail again
The `Space` screen should show `为你推荐`, `提示工程`, `AI 工具`, and `动态` in one row. Only some filters may actively switch data at first, but all four should exist as part of the intended discovery affordance.

Why this decision:
- It restores the user-facing entry points that disappeared during the merge.
- It preserves the broader discovery vocabulary without reintroducing a separate Workshop route.

Alternatives considered:
- Keep only two filters and assume the feed is enough: rejected because it narrows the user’s visible navigation language too aggressively.

### 2. Remove aggregate unread chrome from `Space`
The unread summary card should be removed from the `Space` page entirely so discovery content starts earlier and the screen is not framed by status-heavy helper UI.

Why this decision:
- The user explicitly wants the card and its copy removed.
- The `Space` screen should foreground browsing, not messaging state.

Alternatives considered:
- Keep the summary but shrink it: rejected because the request is removal, not minimization.

### 3. Keep the `Space` header title-only and product-facing
The header should keep the `空间` title but drop the `创作者动态` eyebrow and the long description sentence. If supporting framing is needed later, it should use concise product language rather than commentary about architecture or development intent.

Why this decision:
- It removes prototype-feeling copy without harming discoverability.
- It makes the page more visually aligned with production-style mobile surfaces.

Alternatives considered:
- Replace the old copy with different long explanatory text: rejected because the desired direction is less commentary, not rewritten commentary.

### 4. Capture a production-first UI-copy rule in spec, but implement it narrowly here
This change should introduce a requirement that visible app UI avoids development-stage explanatory notes. The concrete code change in this slice will apply that rule to the `Space` surface rather than trying to audit the entire app immediately.

Why this decision:
- It codifies the direction for future work.
- It keeps the implementation slice reviewable.

Alternatives considered:
- Audit all app surfaces immediately: rejected because it expands scope far beyond the `Space` refinement request.

## Risks / Trade-offs

- [Restoring four filters without fully differentiated data behind each could feel visually ahead of behavior] → Keep the visual restoration aligned with the current feed model and add behavior-specific follow-up only if needed.
- [Removing all helper copy may make the page feel too sparse] → Retain a strong title and the feed/filter structure so the page still reads clearly without explanatory text.
- [The new UI-copy rule may be broader than this one implementation slice] → Capture it in spec, but limit the code change here to the `Space` surface and future touched surfaces.

## Migration Plan

1. Update the `Space` header/filter composition to restore the four filters and remove the unread summary plus development-only header copy.
2. Update Android UI tests to assert the restored filter entries and the absence of removed helper strings.
3. Merge as a presentation refinement on top of the ongoing shell refresh work.

Rollback strategy:
- Revert the `Space`-screen presentation changes while preserving the underlying merged-feed implementation if the refined chrome introduces regressions.

## Open Questions

- Should `AI 工具` and `动态` eventually filter distinct feed subsets, or is restoring them as top-level affordances sufficient for this slice?
- After this change lands, do we want a dedicated follow-up to remove remaining development-stage copy from other surfaces such as onboarding or settings?
