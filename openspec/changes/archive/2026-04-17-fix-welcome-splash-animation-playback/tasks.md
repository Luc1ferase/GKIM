## 1. Playback contract coverage

- [ ] 1.1 Add or refresh focused Android coverage that proves the welcome animation starts on unauthenticated launch and does not silently regress into a frozen first frame.
- [ ] 1.2 Add focused playback-state or lifecycle-recovery coverage around the welcome media surface so attach/resume refreshes have an explicit automated contract.

## 2. Welcome playback implementation

- [ ] 2.1 Introduce the lifecycle-aware welcome playback surface and any narrowly scoped Android media dependency changes needed to replace the current fragile `VideoView` path.
- [ ] 2.2 Rewire `WelcomeRoute` to the new playback surface while preserving the approved `welcome_intro_1` asset, muted looping playback, and cover-style presentation.
- [ ] 2.3 Add truthful fallback or recovery handling for welcome-video prepare/resume failures so the onboarding surface never pretends healthy motion when playback has stalled.

## 3. Verification and evidence

- [ ] 3.1 Run focused Android verification for the repaired welcome animation launch path and playback recovery behavior.
- [ ] 3.2 Record verification, review, score, and upload evidence in `docs/DELIVERY_WORKFLOW.md`, and update any affected welcome/onboarding guidance before closing the change.
