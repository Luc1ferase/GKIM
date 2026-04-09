## 1. Welcome Video Coverage

- [ ] 1.1 Update Android welcome/onboarding coverage so it verifies the new packaged runtime video resource is present, the superseded welcome video asset is no longer the shipped backdrop resource, and unauthenticated startup still exposes the native-composed welcome surface with login/register entry actions.

## 2. Welcome Video Asset Swap

- [ ] 2.1 Package `docs/stitch-design/welcome_screen/1.mp4` into Android raw resources under an Android-safe name, update `WelcomeRoute` to bind the welcome `VideoView` to that resource, and remove the superseded runtime video asset if it is no longer referenced.
