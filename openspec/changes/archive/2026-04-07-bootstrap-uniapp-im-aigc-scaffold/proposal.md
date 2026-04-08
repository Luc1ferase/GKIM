## Why

The repository currently contains architectural and design-system harness documents, but no OpenSpec capabilities or implementation scaffold for the UniAPP-based IM product they describe. We need a first-class change that turns those harness constraints into an actionable contract for a modern iOS/Android IM app with built-in AIGC entry points, so implementation can begin without inventing structure ad hoc.

## What Changes

- Initialize the product direction for a UniAPP x mobile IM application using Vue 3, TypeScript, Pinia, and UnoCSS, aligned to the Aether Mono design system and the repository architecture harness.
- Define the baseline mobile shell with a fixed bottom navigation that exposes Messages, Contacts, and Space as the primary tabs.
- Define the initial information architecture for an empty but production-oriented chat list, sortable contacts view, developer-centric Space feed, chat room AIGC entry points, creative workshop flows, and AI settings.
- Define provider abstraction requirements for Tencent Hunyuan, Alibaba Tongyi, and custom OpenAI-compatible endpoints so future AIGC features share one composable and configuration model.
- Capture the expected scaffold scope for app initialization, routing, state management, design tokens, markdown rendering, and placeholder pages required for `/opsx:apply`.

## Capabilities

### New Capabilities
- `core/im-app`: Defines the baseline UniAPP IM application shell, tab navigation, chat/contact/feed surfaces, AIGC entry points, creative workshop flows, and provider settings required for the initial scaffold.

### Modified Capabilities
- None.

## Impact

- Affects the overall mobile app structure under `src/`, including pages, components, composables, stores, APIs, styles, utilities, and shared types.
- Introduces baseline frontend dependencies and configuration for UniAPP x, Vue 3, TypeScript, Pinia, Pinia persisted state, UnoCSS, and markdown rendering.
- Establishes the first OpenSpec capability for the product and creates the contract that future IM, Space, and AIGC iterations will extend.
