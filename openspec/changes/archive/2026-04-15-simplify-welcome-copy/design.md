## Context

The Android welcome screen in `WelcomeRoute.kt` already has the right structural pieces in place: the approved motion backdrop, a compact GKIM hero, and the `注册 / 登录` actions that lead into the real auth flow. The remaining problem is tone. Two visible text areas still read like implementation guidance rather than product copy: the long helper sentence above the CTA row explains internal shell behavior, and the footer line includes `加密连接 / Encrypted connection`, which makes the screen feel more like a technical demo than a clean first-run introduction.

This change is intentionally narrow. The user asked for copy cleanup only, so the design should preserve the existing welcome layout, auth routing, motion backdrop, and lightweight footer placement while updating the content contract around those areas.

## Goals / Non-Goals

**Goals:**
- Remove the helper sentence above the CTA row from the welcome screen.
- Remove the `加密连接 / Encrypted connection` wording from the footer treatment.
- Replace the current main welcome description with more natural product-facing bilingual copy.
- Keep the change small enough that implementation can be verified with focused Android welcome tests.

**Non-Goals:**
- Do not redesign the welcome layout, spacing, typography, or button structure.
- Do not change the approved welcome video asset or its playback behavior.
- Do not modify auth navigation, session logic, or onboarding flow order.
- Do not expand this into a broader app-wide copy rewrite outside the welcome screen.

## Decisions

### 1. Keep the welcome copy inline in `WelcomeRoute` and update only the affected text nodes

The copy change should stay inside the existing `appLanguage.pick(...)` strings already used by the welcome route instead of introducing a new copy abstraction just for one screen.

Why:
- The current welcome copy is already localized inline in the composable, so the smallest truthful change is to update those existing strings.
- This keeps the implementation aligned with the current Android project pattern and avoids broad string-resource churn for a narrowly scoped wording pass.

Alternatives considered:
- Move all welcome text into new string resources: rejected for this slice because it adds translation/plumbing churn without changing the actual product requirement being targeted.
- Introduce a welcome-copy model object: rejected because it would be structural work without meaningful product benefit for three lines of content.

### 2. Remove the helper sentence entirely instead of replacing it with another explanatory line

The lower helper sentence should be deleted rather than rewritten into a softer version.

Why:
- The current request explicitly asks to remove that sentence, and the surrounding screen already has enough context through the title, intro, and auth buttons.
- Removing it helps the lower action area feel lighter and less like a narrated prototype.

Alternatives considered:
- Replace it with a shorter onboarding hint: rejected because it would preserve the same visual job the user wants removed.

### 3. Preserve the footer slot but simplify its wording

The footer can keep a small version marker, but it should no longer lead with `加密连接 / Encrypted connection`.

Why:
- The current footer placement still helps anchor the composition, so we do not need to remove the slot entirely.
- Removing the trust-signaling label keeps the tone cleaner while preserving the lightweight product/version cue.

Alternatives considered:
- Remove the footer line completely: rejected because the user asked to remove the wording, not the entire footer treatment.
- Keep the exact current copy: rejected because it is one of the explicit requested cleanup items.

### 4. Protect the new wording with focused welcome-screen assertions

Implementation should add or refresh Android tests that verify the removed technical phrases are absent and the updated welcome description is present.

Why:
- This is a copy-only change, so regression protection should directly assert the user-visible text contract instead of relying on manual visual checks.
- The project already has welcome-screen instrumentation coverage, making this a cheap and truthful extension.

Alternatives considered:
- Rely only on manual review: rejected because welcome copy can regress quietly during future UI iterations.

## Risks / Trade-offs

- [Inline localized copy can drift again during later welcome edits] → Mitigation: add focused welcome-screen assertions for the removed and replacement text.
- [A rewritten intro line may still feel too brand-heavy or abstract] → Mitigation: keep the new copy short, direct, and centered on what GKIM helps the user do.
- [Changing the footer wording could unintentionally affect visual balance] → Mitigation: preserve the existing footer placement and typography while only simplifying the text content.
