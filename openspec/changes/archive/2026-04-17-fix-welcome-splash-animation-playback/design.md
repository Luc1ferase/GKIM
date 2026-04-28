## Context

The current Android welcome route renders the unauthenticated opening experience in `WelcomeRoute.kt` and plays the approved bundled asset `welcome_intro_1` through a custom `VideoView` container. That path already tries to preserve aspect-ratio cover layout, but the playback contract is still fragile because video preparation, attach/detach, layout updates, and resume behavior are managed through a lightweight imperative wrapper with no explicit lifecycle owner integration or playback-state contract. The user-reported symptom is that the opening animation is not playing correctly, which means the existing welcome-motion requirement in `core/im-app` is no longer being met reliably.

This change affects the app's very first visual surface, so the design should bias toward predictable playback semantics and regression coverage rather than another narrow one-off `start()` tweak. The repository already has focused welcome tests and welcome-specific OpenSpec history, so the fix should align with that existing onboarding contract rather than redefining the screen.

## Goals / Non-Goals

**Goals:**
- Restore reliable playback of the approved welcome animation when the unauthenticated welcome route first appears.
- Make welcome playback resilient to common Android view lifecycle events such as attach, resume, and layout refresh.
- Preserve the existing visual contract: muted looping video, cover-style scaling, same approved asset, same native-composed onboarding UI.
- Add focused automated coverage so a future welcome refactor cannot silently regress playback readiness again.
- Keep implementation scope centered on Android welcome/onboarding behavior, with delivery evidence captured through the existing workflow.

**Non-Goals:**
- Do not redesign the welcome screen layout, copy, or CTA structure.
- Do not replace the approved bundled video asset.
- Do not expand this change into Android 12+ system splash-screen API work.
- Do not introduce backend, auth, or navigation behavior changes unrelated to welcome animation playback.

## Decisions

### 1. Replace the welcome route's `VideoView` playback path with a lifecycle-aware Media3 player surface

The welcome backdrop should move from the current custom `VideoView` container to a small, dedicated Media3/ExoPlayer-backed playback surface configured for muted looping playback and cover-style rendering.

Why this decision:
- `VideoView` exposes only a thin imperative API and makes it easy for playback to depend on timing quirks around preparation, attachment, or focus.
- Media3 gives the app explicit player state, repeat behavior, and view lifecycle hooks that are easier to reason about and test.
- A `PlayerView` or equivalent Media3 surface can preserve the existing zoom/crop behavior through stable resize-mode configuration instead of hand-managed replay behavior on top of `VideoView`.

Alternatives considered:
- Keep `VideoView` and add more `start()` / listener retries: rejected because it is the pattern that already regressed and still leaves the welcome route dependent on brittle view timing.
- Build a custom `TextureView`/matrix pipeline: rejected because it adds unnecessary rendering complexity for a single looping welcome asset.

### 2. Give the welcome animation an explicit playback lifecycle contract

The playback wrapper should define when the player is created, prepared, resumed, paused, and released, and it should reconnect playback automatically when the welcome route re-enters the foreground or the playback view is reattached.

Why this decision:
- The reported issue is about animation not actually playing, not about the asset being missing, so readiness and lifecycle recovery need to become first-class behavior.
- An explicit lifecycle contract makes the welcome surface less sensitive to Compose recomposition and Android view attach/detach timing.
- Clear ownership boundaries will let tests target playback readiness without depending on opaque `VideoView` state.

Alternatives considered:
- Only start the player once during view creation: rejected because it does not protect against pause/resume or reattach regressions.
- Release and rebuild the player on every recomposition: rejected because it would introduce unnecessary churn and could make startup timing worse.

### 3. Keep the current onboarding visual contract intact while tightening fallback/error behavior

The implementation should preserve the current approved asset, scrim treatment, and cover presentation, but it should also define a truthful fallback when media preparation fails so the welcome surface never looks half-initialized.

Why this decision:
- This change is meant to repair a regression, not re-open the design direction for the onboarding surface.
- Users should either see active welcome motion or a deliberate static fallback, not an ambiguous frozen first frame.
- Explicit fallback/error behavior gives QA and future maintainers a stable expectation even if device-specific playback issues occur.

Alternatives considered:
- Hide all failure states and leave the screen as-is: rejected because a silent frozen first frame is exactly the experience we are trying to avoid.
- Swap to a static poster image by default: rejected because the product requirement still calls for visible runtime motion.

### 4. Add focused playback-specific regression coverage instead of relying only on visual spot checks

The test story should expand beyond current asset/scrim/layout assertions to include welcome playback readiness or lifecycle recovery checks, plus focused validation evidence for the repaired launch path.

Why this decision:
- Existing tests confirm the asset and layout contract, but they do not strongly protect against playback-start regressions.
- A launch-surface regression is too visible to leave dependent on manual demo checks alone.
- Focused tests keep the implementation honest while still staying narrower than full video-content instrumentation.

Alternatives considered:
- Rely only on manual emulator observation: rejected because it is slow, subjective, and easy to skip.
- Attempt frame-perfect video validation in instrumentation: rejected because it adds more complexity than this fix needs.

## Risks / Trade-offs

- [Media3 adds a new Android dependency surface] → Mitigation: keep usage narrowly scoped to the welcome route and avoid broader playback abstraction churn in this change.
- [Lifecycle-aware playback can still be tricky around Compose disposal] → Mitigation: isolate player ownership in a single wrapper with explicit release behavior and focused tests around re-entry/recovery.
- [Different devices may still prepare the asset at different speeds] → Mitigation: define a truthful ready/fallback contract so delayed preparation does not present as a broken frozen UI.
- [A welcome-only player refactor could accidentally disturb cover layout] → Mitigation: preserve the current viewport-cover expectations and keep/update the existing layout tests alongside the playback fix.

## Migration Plan

1. Add or wire the welcome playback dependency and wrapper needed for stable muted looping playback.
2. Replace the current `VideoView`-based welcome backdrop implementation with the lifecycle-aware player surface while preserving the approved asset and cover styling.
3. Add or refresh focused welcome playback tests around first render and re-entry/recovery behavior.
4. Run targeted Android verification for the welcome launch surface and record evidence in `docs/DELIVERY_WORKFLOW.md`.

Rollback strategy:
- Revert the welcome playback wrapper back to the current `VideoView` path if the new player integration introduces worse regressions, while keeping the rest of the welcome route intact.

## Open Questions

- Whether the final implementation should expose a subtle static fallback poster when playback preparation fails, or simply preserve the existing welcome background treatment without additional poster artwork.
