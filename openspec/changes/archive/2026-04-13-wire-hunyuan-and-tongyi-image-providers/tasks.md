## 1. Preset provider configuration

- [x] 1.1 Update the Android preset AIGC provider metadata so Tencent Hunyuan defaults to `hy-image-v3.0`, Alibaba Tongyi defaults to `wan2.7-image`, and each preset advertises only the capabilities the selected image model can really support.
- [x] 1.2 Extend the Android AIGC configuration model and secure-storage plumbing so Hunyuan and Tongyi preset API keys (and any local preset-model overrides, if supported) can be entered and restored locally without tracked repo defaults.

## 2. Real provider request path

- [x] 2.1 Add Android remote AIGC adapters/clients for Tencent Hunyuan and Alibaba Tongyi that build the correct provider-specific image-generation requests and map real success/error responses into shared app models.
- [x] 2.2 Refactor the Android AIGC repository/generation flow from placeholder synchronous success into an asynchronous real-request path with truthful task status, provider/model metadata, and no stock fallback previews on failure.

## 3. Chat and settings UX

- [x] 3.1 Update the Android AI Settings surface and ViewModel so preset provider credentials can be managed locally and the active preset/custom provider state remains clear to the user.
- [x] 3.2 Update the Android chat AIGC entry flow so the `+` menu reflects active-provider capabilities and the user sees truthful loading, success, missing-key, unsupported-mode, and provider-failure feedback.

## 4. Verification and acceptance

- [x] 4.1 Add or refresh Android unit/UI coverage for preset provider persistence, capability filtering, and mocked Hunyuan/Tongyi generation success/failure behavior.
- [ ] 4.2 Run local validation for real Hunyuan `hy-image-v3.0` and Tongyi `wan2.7-image` generation results using locally entered secrets, then record verification/review/score/upload evidence in `docs/DELIVERY_WORKFLOW.md`.
