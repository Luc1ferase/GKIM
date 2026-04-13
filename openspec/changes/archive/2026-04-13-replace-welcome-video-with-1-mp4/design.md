## Context

The current Android welcome/onboarding surface already uses a native `VideoView` backdrop, but it is bound to the previously packaged raw asset `R.raw.welcome_atrium`. The requested replacement source exists in the design-reference folder as `docs/stitch-design/welcome_screen/1.mp4`, which cannot be referenced directly from Android runtime resources because raw resource identifiers must use Android-safe names and cannot start with a digit.

This is a tightly scoped media swap. The welcome screen layout, copy, login/register entry actions, and native-composed onboarding approach should remain intact. The only product change is which looping muted video plays behind that existing runtime composition. Because the previous change already established the rule that reference mockups should not be used as runtime overlays, this slice should preserve the same native structure while updating the motion asset only.

## Goals / Non-Goals

**Goals:**
- Replace the runtime welcome video with the approved motion source derived from `docs/stitch-design/welcome_screen/1.mp4`.
- Keep the existing welcome screen playback behavior: muted, looping, full-screen backdrop.
- Make the packaged Android resource name explicit and Android-safe so future video swaps remain understandable.
- Add verification that the new packaged runtime asset is present and the welcome route still renders the onboarding screen correctly.

**Non-Goals:**
- Redesigning welcome/onboarding layout, copy, or CTA structure.
- Introducing a different playback technology or animation framework.
- Changing authentication state handling or welcome-screen navigation behavior.
- Treating the design-reference folder itself as a runtime-loaded location.

## Decisions

### 1. Package `1.mp4` as a new Android-safe raw resource and update the binding explicitly
The source file `docs/stitch-design/welcome_screen/1.mp4` should be copied into `android/app/src/main/res/raw/` under a descriptive Android-safe name, and `WelcomeRoute` should reference that new raw resource instead of the old `welcome_atrium` id.

Why this decision:
- Android raw resource ids cannot begin with a digit, so the design file name cannot be used directly.
- An explicit new resource name makes the swap auditable in code review and easier to reason about later.
- It avoids hidden behavior changes where the file contents change but the runtime identifier still points at an old conceptual name.

Alternatives considered:
- Overwrite `welcome_atrium.mp4` in place: rejected because it hides the source swap behind an old name and makes future provenance harder to understand.
- Load `docs/stitch-design/welcome_screen/1.mp4` directly at runtime: rejected because design-reference directories are not runtime resource locations for the Android app.

### 2. Preserve the existing `VideoView` playback path
The current native `VideoView` composition should remain the playback mechanism. Only the raw resource id should change.

Why this decision:
- The requested scope is an asset replacement, not a playback refactor.
- The current implementation already supports looping muted playback and is covered by existing welcome-screen UI tests.

Alternatives considered:
- Replace `VideoView` with a more advanced player: rejected because it adds technical churn without solving the actual request.

### 3. Verify the asset swap through packaged-resource assertions plus welcome-surface coverage
Regression coverage should verify that the welcome screen still renders and that the newly packaged raw resource is present under the expected runtime name. If the old raw resource is intentionally removed, the test should also assert it is no longer packaged.

Why this decision:
- It keeps verification focused on the real change: the runtime asset identity and the preserved welcome-screen behavior.
- It avoids adding test-only production seams just to inspect which raw resource id the `VideoView` is using.

Alternatives considered:
- Only test that the welcome screen still appears: rejected because that would not prove the runtime asset changed.

## Risks / Trade-offs

- [The new resource name could be ambiguous or too version-specific] → Use a descriptive name that reflects welcome-screen motion without leaking temporary file-system quirks into app code.
- [Copying the new MP4 could increase APK size or create duplicate media if the old asset is kept] → Prefer removing the superseded raw resource if nothing else references it.
- [A resource-only change may look complete without proving the screen still plays it] → Pair the packaged-resource check with existing welcome-screen UI coverage.

## Migration Plan

1. Add or update regression coverage for the packaged welcome video resource and the existing welcome-screen runtime composition.
2. Copy `docs/stitch-design/welcome_screen/1.mp4` into Android raw resources under the chosen Android-safe name.
3. Update `WelcomeRoute` to bind the welcome `VideoView` to the new raw resource id.
4. Remove the superseded packaged raw asset if it is no longer referenced.
5. Run focused Android verification and record the evidence in the change tasks.

Rollback strategy:
- Restore the previous raw asset and revert `WelcomeRoute` to the old resource id without affecting the rest of the welcome-screen composition.

## Open Questions

- Do we want the Android-safe packaged name to preserve the `1` versioning hint, or should it use a more semantic name such as `welcome_intro_loop` for longer-term stability?
