## Why

The current IM app direction and capability spec are anchored to a UniAPP cross-platform scaffold, but the product requirement has shifted to Android-only delivery with strong device adaptation and native-quality media/AIGC workflows. We need to realign the implementation contract now so the project can optimize for Android performance, local persistence, media access, and maintainability without changing the approved Aether Mono frontend design.

## What Changes

- Pivot the baseline IM app implementation contract from a UniAPP x cross-platform scaffold to an Android-only application architecture.
- Preserve the existing visual system, screen hierarchy, and interaction model defined by the Aether Mono and Stitch design references, including the three-tab shell, chat flows, workshop, and AI settings surfaces.
- Redefine the scaffold requirements around an Android-native stack that better fits responsive messaging, media selection, WebSocket-based IM, offline cache, and AIGC generation entry points.
- Keep the existing product capabilities intact at the UX level while updating the underlying spec requirements for navigation, state management, rendering, persistence, and provider integration on Android.
- Capture the implementation-ready scope for Android app bootstrap, screen shells, rich-content rendering, media ingest, and configurable AI provider settings required before /opsx:apply.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `core/im-app`: Replace the UniAPP-oriented scaffold and runtime requirements with Android-native requirements while preserving the current IM information architecture, AIGC flows, and Aether Mono visual contract.

## Impact

- Affects the mobile app foundation, build tooling, navigation, state model, storage, media pipeline, and AIGC integration strategy for the IM client.
- Updates the OpenSpec contract for openspec/specs/core/im-app/spec.md and adds a delta spec that changes implementation requirements without changing the approved frontend design behavior.
- Introduces Android-native dependencies for UI, persistence, networking, media handling, and rich content rendering in place of the previous UniAPP-specific assumptions.
