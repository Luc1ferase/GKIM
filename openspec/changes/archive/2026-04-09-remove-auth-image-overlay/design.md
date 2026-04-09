## Context

`refresh-shell-space-and-auth-onboarding` recently introduced a root auth gate plus a welcome screen in Android. The visual direction came from `docs/stitch-design/welcome_screen`, but the current implementation imported the static screenshot mockup into the runtime surface and layered live controls on top of it. That breaks the user’s intent for the design handoff: the image was only meant to show what to build, not to become part of the actual shipped screen.

This correction needs to stay tightly scoped. The underlying auth-state seam from the previous change should remain intact, and the backend work has not started yet. The goal here is to fix the visual contract of the unauthenticated shell without reopening the broader auth roadmap.

## Goals / Non-Goals

**Goals:**
- Render the welcome/auth surface as native Compose UI instead of a screenshot-backed composition.
- Keep the existing `注册` / `登录` entry actions and unauthenticated-to-authenticated preview seam working.
- Preserve the high-key, architectural-light mood from the provided reference materials without embedding the static mockup itself.
- Add regression coverage so future onboarding changes do not reintroduce reference-image overlays.

**Non-Goals:**
- Changing the backend auth model, account persistence contract, or session APIs.
- Replacing the onboarding video direction unless it becomes technically necessary during implementation.
- Completing the real login/register forms; that remains in the follow-up task for Android-side auth flows.

## Decisions

### 1. Remove the static mockup image from the runtime welcome composition
The shipped Android welcome surface will no longer render the imported screenshot/mockup image as a full-screen layer. The layout should be rebuilt from native gradients, spacing, typography, and optional ambient media instead.

Why this decision:
- It directly addresses the reported overlap problem.
- It keeps the UI readable across device sizes instead of baking in one captured composition.
- It treats the design handoff correctly: reference, not runtime content.

Alternatives considered:
- Keep the screenshot but fade or blur it more aggressively: rejected because the underlying problem remains; runtime UI would still depend on a mockup capture.
- Crop the screenshot into smaller decorative fragments: rejected because it still ships reference artwork as product UI.

### 2. Keep the current auth-state seam, but only change the unauthenticated presentation layer
The root auth gate added in the existing change should remain the same. This follow-up only adjusts how the unauthenticated welcome state is rendered and tested.

Why this decision:
- It keeps the fix small and reviewable.
- It avoids colliding with the next task, which will introduce real local session persistence and login/register forms.

Alternatives considered:
- Fold this fix into the larger auth-form task: rejected because the visual regression is already user-visible and should be corrected independently.

### 3. Treat the video asset as optional ambient media, not as layout structure
If the implementation keeps the provided MP4, it should behave as a subordinate atmosphere layer with native overlays defining the real structure. The static screenshot image should not be used at runtime.

Why this decision:
- The earlier design direction explicitly referenced a video-led first impression.
- It preserves flexibility: the video can stay if it helps, but the layout itself must no longer depend on imported UI art.

Alternatives considered:
- Remove all bundled onboarding media: rejected for now because the user only objected to the screenshot overlay, not the motion direction itself.

## Risks / Trade-offs

- [Removing the screenshot may make the screen feel less dramatic at first] → Rebuild the drama through native gradient, spacing, and type hierarchy instead of imported artwork.
- [The current video layer may feel too visually busy once the screenshot is gone] → Keep video subordinate and allow a gradient-first fallback if readability suffers.
- [This follow-up touches code that just landed in another change] → Keep the diff tightly scoped to the unauthenticated route and its tests.

## Migration Plan

1. Remove the static mockup image from the Android welcome/auth runtime composition.
2. Replace it with native layout layers that recreate the intended composition without embedding the screenshot.
3. Update welcome/auth UI tests to verify the unauthenticated surface still appears and that auth controls render without the reference-image dependency.
4. If the screenshot asset is no longer used anywhere in Android runtime code, stop packaging it with the app.

Rollback strategy:
- Revert the welcome-route presentation change while leaving the root auth-state seam intact if the new native composition introduces regressions.

## Open Questions

- Should the follow-up implementation remove the static screenshot asset from Android resources entirely, or keep it only in `docs/stitch-design/welcome_screen` as an out-of-app reference?
- Is the onboarding MP4 still desirable as ambient motion once the screen is rebuilt natively, or should the fix simplify the first-run presentation to gradients plus typography only?
