## Why

The current AIGC provider presets for Tencent Hunyuan and Alibaba Tongyi are only selection scaffolds: generation still returns placeholder previews instead of real provider output, so we cannot truthfully validate image quality or demo the product's AIGC value. We need to wire the two requested image models now so local testing can verify real generation behavior while keeping vendor API secrets out of versioned repository artifacts.

## What Changes

- Replace the Android app's placeholder preset-provider generation path with real Tencent Hunyuan and Alibaba Tongyi image-generation adapters.
- Update the preset provider metadata to use the requested models `hy-image-v3.0` and `wan2.7-image`.
- Add secure local configuration and persistence for provider API keys so the app can test real generation without committing raw keys to tracked files.
- Surface truthful generation states, errors, and returned image results in the existing AIGC workflow instead of always synthesizing success with stock preview URLs.
- Add focused verification for provider selection, provider request wiring, secure key handling, and local AIGC generation validation evidence.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: AI provider settings and AIGC generation behavior will change so preset Hunyuan and Tongyi selections can drive real image generation with secure local secret handling and truthful runtime result states.

## Impact

- Affected code: Android AIGC repository, provider metadata, settings surfaces, secure storage integration, network client layer, and AIGC-related tests under `android/app/src/main`.
- Affected systems: Android app runtime AIGC flow, local provider credential configuration, and local/manual provider validation.
- Affected dependencies: provider-specific HTTP request handling for Tencent Hunyuan and Alibaba Tongyi image APIs; existing secure storage support will likely be reused for local API-key persistence.
- Constraints: the provided vendor API keys must remain local-only inputs and must not be written verbatim into versioned OpenSpec artifacts, source files, or default config files.
