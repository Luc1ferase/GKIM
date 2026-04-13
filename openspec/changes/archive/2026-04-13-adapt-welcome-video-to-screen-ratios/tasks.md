## 1. Welcome Video Cover Coverage

- [x] 1.1 Add or update focused Android coverage so the welcome/onboarding backdrop contract verifies aspect-ratio-preserving fullscreen cover behavior for representative phone viewport ratios and confirms the lower CTA area no longer renders the standalone translucent block above the `注册` / `登录` actions.

## 2. Responsive Welcome Playback

- [x] 2.1 Replace the current welcome-video playback path with a runtime surface/configuration that preserves the approved `welcome_intro_1` asset, keeps playback muted and looping, applies centered zoom-and-crop cover behavior across supported Android phone screen sizes, and removes the standalone translucent block above the auth CTA row.
- [x] 2.2 Clean up any supporting welcome-video playback helpers or dependency wiring needed for the new cover policy, then run the focused Android verification required by the updated welcome coverage.
