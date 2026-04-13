## Context

The Android app already exposes preset AIGC providers for Tencent Hunyuan and Alibaba Tongyi in Settings, stores custom-provider secrets in secure local storage, and lets chat users trigger AIGC actions from the composer. However, the current Android `DefaultAigcRepository` still synthesizes immediate success with stock preview URLs, so provider selection does not affect any real generation behavior. The requested work is to connect the two preset providers to real image-generation models, validate output quality locally, and do so without committing raw vendor API keys into repository files or OpenSpec artifacts.

This change is cross-cutting across settings, repository, chat UI state, provider metadata, and tests. It also introduces security-sensitive handling because provider keys are user-supplied local secrets rather than checked-in configuration.

## Goals / Non-Goals

**Goals:**
- Make the Android preset providers for Tencent Hunyuan and Alibaba Tongyi invoke real image-generation APIs instead of placeholder previews.
- Default the preset models to `hy-image-v3.0` and `wan2.7-image`.
- Keep provider API keys local-only and restore them through the existing secure on-device storage path.
- Surface truthful generation progress, success, unsupported-mode, and failure states in the chat AIGC flow.
- Add focused automated tests for provider configuration and request orchestration, plus a local manual verification story for real provider output.

**Non-Goals:**
- Do not commit the provided API keys to tracked files, OpenSpec artifacts, defaults, screenshots, or tests.
- Do not add a backend relay/proxy in this change; the initial validation path is direct Android-to-provider HTTPS with local secure key entry.
- Do not broaden the requested preset models beyond the vendor image models already supplied by the user.
- Do not promise CI-executed real vendor generation tests; real-provider validation remains local/operator-driven because it depends on secrets and billable APIs.

## Decisions

### 1. Keep provider secrets local and provider-specific

Preset providers will use secure local storage entries keyed by provider ID, rather than hardcoded defaults or one shared API-key field. This lets the app restore Hunyuan and Tongyi credentials independently while keeping the existing custom-provider flow intact.

Why this approach:
- The repository already has secure local storage and preferences plumbing, so this extends an existing pattern instead of introducing a new secret mechanism.
- It prevents the user-provided keys from being copied into tracked source, OpenSpec files, or default config.
- It supports switching between providers without retyping keys every time.

Alternatives considered:
- Hardcode the user-provided keys in seed config. Rejected because it would leak secrets into version control.
- Require manual key entry on every launch. Rejected because it makes repeated local evaluation unnecessarily brittle.

### 2. Add dedicated provider adapters for Hunyuan and Tongyi instead of routing them through the custom endpoint path

The Android AIGC layer will gain provider-specific request builders and response mappers for Tencent Hunyuan and Alibaba Tongyi. The custom OpenAI-compatible provider remains separate because its request/response contract is intentionally generic.

Why this approach:
- The vendor APIs are not guaranteed to match the custom OpenAI-compatible schema.
- Provider-specific adapters keep vendor payload details isolated from chat and settings UI code.
- This structure makes it easy to test request mapping and error handling per provider.

Alternatives considered:
- Force both vendors through the existing custom provider schema. Rejected because it would hide provider-specific auth and payload differences behind brittle string configuration.
- Add backend proxying first. Rejected for this change because the current request is to connect and validate the models quickly, and no backend AIGC gateway exists today.

### 3. Make real generation asynchronous and truthfully stateful

`AigcRepository.generate` should move from instant synthetic success to an asynchronous real request flow. The repository should create/update task state so the UI can distinguish in-flight, succeeded, and failed generations, and chat should append results only after a real success response arrives.

Why this approach:
- Real provider HTTP calls are asynchronous and failure-prone.
- The current synchronous success path would otherwise keep producing fake success even when keys are missing or vendors reject the request.
- Truthful state is necessary for demo credibility and local debugging.

Alternatives considered:
- Keep the synchronous repository API and block until completion. Rejected because it would make UI state/error handling awkward and hide progress semantics.
- Continue to generate placeholder cards on failure. Rejected because it misrepresents the provider outcome.

### 4. Restrict visible AIGC actions to the active provider’s supported capabilities

The requested models are image-generation models, so preset provider capabilities must stay aligned with what the active provider can really execute. The chat `+` menu should only offer supported AIGC modes for the selected provider, or explicitly block unsupported modes before any network request starts.

Why this approach:
- It avoids presenting video or other unsupported flows as if they are ready.
- The existing `AigcProvider.capabilities` model already supports this filtering.
- It keeps the UI honest when switching among providers with different capability sets.

Alternatives considered:
- Keep showing all AIGC modes and fail only after tap. Rejected because it still overpromises capability.
- Remove provider presets entirely and require custom-only setup. Rejected because the change is specifically about the two named presets.

## Risks / Trade-offs

- [Vendor API contracts may differ from assumptions] → Mitigation: isolate request/response handling in provider adapters and verify against official docs during implementation.
- [Direct mobile-to-vendor requests expose secrets on-device] → Mitigation: scope this to local/operator validation, use secure local storage, and keep all repo defaults secret-free.
- [Real generation latency may slow the chat UX] → Mitigation: add explicit in-flight state and delay message insertion until a confirmed success response arrives.
- [Image-only models may shrink the visible preset capability set] → Mitigation: filter actions by provider capability instead of pretending unsupported modes work.
- [Automated tests cannot safely call billable vendor APIs] → Mitigation: cover adapter/repository logic with mocks and record separate manual/local validation evidence.

## Migration Plan

1. Extend preset provider metadata to the requested model identifiers and accurate capability sets.
2. Add provider-specific secret storage and settings UI inputs for preset credentials/models.
3. Introduce provider adapters plus repository/task-state changes for real asynchronous generation.
4. Update chat UI to reflect loading/success/failure and to show only supported AIGC actions per provider.
5. Add unit/UI coverage with mocked provider responses.
6. Run local real-provider validation with the user-supplied keys entered locally, then record evidence in `docs/DELIVERY_WORKFLOW.md`.

Rollback strategy:
- Revert to the current placeholder-generation repository path and prior preset metadata.
- Clear local preset-provider keys if needed; no backend or database migration is involved.

## Open Questions

- Should preset-provider model fields remain editable for advanced local experimentation, or should they be fixed to the requested defaults and only be changed in code?
- Do we want the chat timeline to show a failed AIGC task card, or should failure remain an inline composer/message-level notice without inserting a timeline item?
