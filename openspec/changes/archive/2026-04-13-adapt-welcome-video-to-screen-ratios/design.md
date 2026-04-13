## Context

The current Android welcome route already renders a native-composed onboarding surface and plays the approved bundled asset `welcome_intro_1.mp4` behind it. However, the implementation uses a plain `VideoView` inside `AndroidView` with `fillMaxSize()`, which does not give the app an explicit, testable contract for how the video should behave when device aspect ratios diverge from the source asset. The lower welcome composition also includes a standalone translucent decorative block above the auth CTA row, which is no longer part of the intended production-facing welcome treatment.

This change is intentionally narrow. The welcome copy, login/register actions, auth-state seam, and approved motion asset should remain intact. The only product change is how that existing runtime video scales so the screen behaves like a proper full-bleed cover surface on different Android phone resolutions.

## Goals / Non-Goals

**Goals:**
- Preserve the current approved welcome motion asset and the existing looping muted playback behavior.
- Make the welcome video backdrop fill the available viewport across supported Android phone aspect ratios without stretching the source video.
- Prefer centered zoom-and-crop behavior over letterboxing or pillarboxing when the asset ratio and screen ratio differ.
- Remove the translucent decorative block above the `注册` / `登录` action row so the CTA area reads as one clean band.
- Keep the change local to the welcome playback layer and add regression coverage for the responsive scaling contract.

**Non-Goals:**
- Redesigning the welcome page layout, typography, or CTA placement.
- Replacing the approved welcome video asset with a different animation.
- Changing account/login/register flow behavior or the broader onboarding navigation model.
- Optimizing for tablet- or desktop-specific layout modes beyond the current phone-first Android surface.

## Decisions

### 1. Move the welcome backdrop to a playback surface that supports explicit cover scaling
The implementation should stop relying on a plain `VideoView` sizing path and instead use a playback surface/configuration that can explicitly enforce fullscreen cover behavior, such as a player view with zoom/cover resize support.

Why this decision:
- The current `VideoView` path does not expose a robust cover-mode contract for mixed screen ratios.
- A playback surface with explicit cover semantics is easier to reason about and less likely to regress into fit-center or stretched behavior later.
- This keeps the responsive behavior in the media layer instead of scattering manual size hacks through the Compose layout.

Alternatives considered:
- Keep `VideoView` and accept its default fitting behavior: rejected because it does not guarantee the zoom-and-crop presentation the user asked for.
- Build a custom `TextureView` transform/matrix solution from scratch: rejected for now because it adds more bespoke media math and lifecycle work than this slice needs.

### 2. Standardize on aspect-ratio-preserving center crop
The welcome backdrop should preserve the source video aspect ratio and scale until the viewport is fully covered, allowing centered crop on the outer edges when ratios do not match.

Why this decision:
- It matches the requested “放大裁切” direction.
- It keeps the welcome experience cinematic and edge-to-edge on tall mobile screens.
- It avoids visual distortion that would make the architectural motion asset feel cheap or broken.

Alternatives considered:
- Fit-center with top/bottom or side bars: rejected because the backdrop would no longer feel like a full-screen welcome surface.
- Stretch-to-fill: rejected because it distorts the approved motion asset.

### 3. Remove the standalone translucent CTA-adjacent decoration
The dedicated translucent rounded block currently rendered above the auth action row should be removed rather than resized or faded.

Why this decision:
- The user explicitly wants that decoration gone.
- It competes with the CTA row instead of supporting it.
- A cleaner lower band better matches the production-facing welcome direction than an extra floating accent shape in the immediate action area.

Alternatives considered:
- Keep the block but make it smaller or lighter: rejected because the extra chrome would still sit directly above the primary actions.
- Move the block elsewhere in the layout: rejected because the current scope is cleanup, not replacing one decorative flourish with another.

### 4. Verify the responsive contract through focused playback-policy coverage plus existing welcome smoke coverage
The change should add targeted verification that the welcome backdrop uses the cover policy while preserving the existing welcome screen smoke checks for the unauthenticated surface and CTA actions.

Why this decision:
- The risky part of this change is not the existence of the welcome screen, but whether future refactors silently revert the cover behavior.
- A focused playback-policy seam is more stable than trying to compare rendered video frames pixel-by-pixel in instrumentation tests.
- It keeps regression coverage aligned with the existing `GkimRootAppTest` strategy.

Alternatives considered:
- Rely only on manual emulator inspection: rejected because the change is specifically about device-size behavior and would be easy to regress later.
- Add screenshot testing for live video frames: rejected because it is heavier than needed for this contract and brittle for looping motion content.

## Risks / Trade-offs

- [Adding a richer playback surface may introduce a new Android dependency and slightly more lifecycle complexity] -> Keep the media change isolated to the welcome route and prefer a small, standard dependency rather than a bespoke player stack.
- [Center crop can trim important edge detail from the source video on very tall screens] -> Use centered cropping and keep the requirement scoped to background atmosphere, not edge-critical narrative content.
- [Removing the translucent block may make the lower welcome area feel emptier at first] -> Let the CTA row, spacing, and video/gradient atmosphere carry the composition instead of restoring extra chrome above the buttons.
- [Playback-policy tests may prove configuration more directly than final pixels] -> Pair policy-level verification with the existing welcome-screen smoke tests so both configuration and end-to-end surface presence are covered.

## Migration Plan

1. Add or update focused welcome-video coverage so the responsive cover policy is part of the automated contract.
2. Introduce the playback surface/configuration needed to support aspect-ratio-preserving cover scaling.
3. Wire the existing welcome backdrop to that playback surface while keeping `welcome_intro_1` as the approved bundled asset.
4. Remove the standalone translucent decoration above the auth CTA row.
5. Run focused Android verification for welcome onboarding and record the evidence in the implementation tasks.

Rollback strategy:
- Restore the current `VideoView`-based backdrop path if the new playback surface causes regressions, while leaving the welcome route structure and approved asset unchanged.

## Open Questions

- If the Android app later adds a landscape welcome experience, should that route keep the same center-crop policy or fall back to a different presentation tuned for wider viewports?
