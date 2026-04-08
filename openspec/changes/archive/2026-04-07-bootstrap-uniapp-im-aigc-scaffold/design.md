## Context

The repository defines a clear harness for a cross-platform UniAPP x mobile product, but it does not yet include any OpenSpec capability or code scaffold that realizes those constraints. The requested product is an IM application for iOS and Android with a cyber-minimal, early-Telegram-inspired interface, bottom-tab navigation for Messages, Contacts, and Space, and built-in AIGC workflows for text-to-image, image-to-image, and video-to-video generation.

The architecture harness requires strict layering (`pages/`, `components/`, `composables/`, `stores/`, `api/`, `utils/`) and explicitly forbids complex logic or direct API access inside page and component files. The design-system harness requires Aether Mono styling, Space Grotesk + Inter typography, no hard divider lines, glassmorphism for floating surfaces, and page-level alignment with the provided Stitch references for messages, contacts, chat, workshop, settings, and discovery feed.

This change is the first project scaffold, so it must define a stable implementation shape rather than solving every backend or production detail. The design therefore focuses on a shippable vertical skeleton: app shell, core navigation, placeholder data flows, provider abstraction, content rendering pipeline, and the state/configuration surfaces that future iterations can extend.

## Goals / Non-Goals

**Goals:**
- Establish a UniAPP x scaffold using Vue 3, TypeScript, Pinia, and UnoCSS that conforms to the repository harness.
- Define a bottom-tab mobile shell with Messages, Contacts, and Space as first-class navigation surfaces.
- Define placeholder but structurally correct pages for chat list, contacts sorting, developer Space feed, chat room AIGC entry, creative workshop, and AI settings.
- Standardize AIGC integration behind composables and provider adapters so Tencent Hunyuan, Alibaba Tongyi, and custom OpenAI-compatible APIs share one configuration model.
- Define a content-rendering path for developer posts that can parse Markdown, CSS-authored presentation blocks, and MDX-oriented content metadata without coupling rendering logic to pages.
- Keep the initial scaffold fast, mobile-first, and visually aligned with the Aether Mono references for both iOS and Android.

**Non-Goals:**
- Deliver a production-ready IM backend, authentication system, or fully operational WebSocket infrastructure in this change.
- Implement final-quality AIGC media generation, billing, moderation, or asset storage flows.
- Finalize cloud deployment topology between UniCloud and a custom backend.
- Ship a complete social graph, notifications pipeline, or KOL reputation algorithm beyond scaffold-level placeholders.

## Decisions

### 1. Use a route-driven app shell with a dedicated tab host and stacked detail pages
The scaffold will define a root shell that owns the fixed bottom navigation and route state for Messages, Contacts, and Space, while chat room, workshop, and settings pages live as stacked detail routes outside the tab host.

Why this decision:
- It matches the user requirement for three persistent bottom tabs.
- It keeps the early Telegram-like mental model of fast top-level switching with deeper conversational/detail flows.
- It avoids duplicating navigation chrome across feature pages.

Alternatives considered:
- Putting every screen inside a tab: rejected because settings, chat detail, and workshop are secondary flows, not primary destinations.
- Using a custom all-in-one page with conditional panels only: rejected because it weakens route clarity and platform navigation behavior.

### 2. Encode harness layering directly in the scaffold structure
The scaffold will mirror the required architecture with `src/pages`, `src/components`, `src/composables`, `src/stores`, `src/api`, `src/utils`, `src/styles`, and `src/types`. Page files remain thin and delegate state and business behavior to Pinia stores plus composables such as `useChat`, `useAIGC`, and `useCreativeWorkshop`.

Why this decision:
- The harness explicitly prohibits direct API calls and complex logic in pages/components.
- Encoding the separation in the initial scaffold reduces future architectural drift.
- It makes later testing straightforward because stores/composables become the primary units under Vitest.

Alternatives considered:
- Starting with feature-heavy page files and refactoring later: rejected because it violates the harness and increases cleanup cost.
- Organizing by domain folders only: rejected for the first scaffold because the harness already standardizes layer-first ownership.

### 3. Implement the visual language through UnoCSS theme tokens plus lightweight shared primitives
The scaffold will define Aether Mono design tokens in `unocss.config.ts` and centralize app-level visual primitives such as glass panels, tonal cards, typography helpers, and animated tab states in shared styles/components.

Why this decision:
- The design-system harness is token-driven and explicitly references UnoCSS.
- Shared primitives are the easiest way to keep Messages, Contacts, Space, Chat, Workshop, and Settings visually coherent.
- It supports the required no-line rule, glassmorphism, gradient CTAs, and responsive motion without introducing a heavyweight UI kit.

Alternatives considered:
- Raw page-local class strings only: rejected because token drift would happen quickly.
- A third-party component library: rejected because it would fight the bespoke Aether Mono visual direction.

### 4. Model conversations, contacts, feed posts, prompts, and provider settings as persisted Pinia domains
The scaffold will include domain stores for chat, contacts/feed, and AIGC settings/workshop. Each store will expose typed state, derived getters, and seed/mock data so the scaffold is interactive before backend integration. Persisted storage will be used for conversations cache, provider selection, and workshop drafts.

Why this decision:
- The harness requires Pinia plus persisted state, and local cache is mandatory for IM data.
- Seed data keeps empty-state and populated-state UI easy to validate during implementation.
- Separating provider settings from generation sessions makes future backend swapping safer.

Alternatives considered:
- Keeping all scaffold data as page-local refs: rejected because it would be hard to migrate and test.
- Persisting everything immediately to remote storage: rejected because the backend contract is not finalized.

### 5. Put AIGC provider switching behind adapter interfaces in `api/` and a single `useAIGC` composable
The change will define provider presets for Tencent Hunyuan and Alibaba Tongyi, plus a custom provider form for OpenAI-compatible endpoints. The API layer will normalize provider capabilities, authentication headers, model identifiers, and payload shapes behind adapter interfaces, while `useAIGC` exposes generation commands to chat and workshop surfaces.

Why this decision:
- The architecture harness requires all AIGC flows to go through `composables/useAIGC.ts` with multi-provider support.
- A normalized provider registry reduces coupling between UI and vendor-specific request formats.
- Chat, workshop, and settings can share the same capability metadata and error/loading states.

Alternatives considered:
- Calling provider APIs directly from settings or chat pages: rejected because it violates the harness and fragments behavior.
- Designing for only one vendor now: rejected because multi-provider support is a stated product requirement.

### 6. Use a unified developer-post content pipeline with Markdown-first rendering and MDX-ready abstraction
The Space feed and creative workshop content previews will use a shared renderer utility that accepts a typed post document containing Markdown body, optional CSS theme blocks, and MDX-oriented metadata/extensions. The initial scaffold will prioritize Markdown rendering through `markdown-it` and style-safe CSS sections, while keeping the content model ready for MDX parsing without requiring arbitrary runtime code execution on mobile.

Why this decision:
- The product requirement explicitly includes CSS, Markdown, and MDX parsing for developer-style posts.
- A Markdown-first renderer is lighter and safer for the initial scaffold, while an MDX-ready document model avoids repainting the feature later.
- Shared rendering logic keeps Space posts, prompt guides, and workshop templates visually and semantically aligned.

Alternatives considered:
- Full runtime MDX execution in the initial scaffold: rejected because it increases complexity and platform risk before backend/content rules are settled.
- Plain text feed posts only: rejected because it misses the core developer-community differentiator.

### 7. Treat creative workshop as a reusable prompt-template domain, not a one-off modal
The creative workshop will be modeled as a full screen/route with searchable prompt cards, category filters, saved presets, and contribution affordances. Chat pages will open it as a supporting workflow for users who cannot write prompts from scratch.

Why this decision:
- The workshop is central to user adoption and creator/KOL growth, not an incidental settings drawer.
- A reusable route supports both browsing community prompts and selecting templates inside chat flows.
- It matches the design references that emphasize a rich, visual, bento-like browsing surface.

Alternatives considered:
- A simple dropdown of preset prompts: rejected because it undercuts the community and discovery goals.
- Embedding workshop content only inside the chat input overlay: rejected because it would be cramped on mobile and hard to scale.

## Risks / Trade-offs

- [MDX support may be over-scoped for the first scaffold] -> Mitigation: define an MDX-ready content model and renderer boundary now, while prioritizing Markdown/CSS rendering and sample MDX parsing hooks during implementation.
- [Cross-platform visual fidelity may vary between iOS and Android] -> Mitigation: keep tokens centralized, rely on UniAPP-safe layout primitives, and reserve platform-specific polish for conditional compile blocks only when necessary.
- [Provider APIs differ substantially in auth and payload shape] -> Mitigation: keep adapters strongly typed and normalize capability metadata before UI consumption.
- [An empty scaffold could feel too static] -> Mitigation: seed representative mock data for conversations, contacts, developer posts, workshop prompts, and provider presets.
- [The initial change could sprawl into backend work] -> Mitigation: explicitly limit this change to scaffold, contracts, placeholder flows, and local state architecture.

## Migration Plan

- Create the initial app scaffold and configuration files in a single feature branch through `/opsx:apply`.
- Seed the app with local mock data and persisted settings so designers and reviewers can validate information architecture before backend work lands.
- Integrate real IM/AIGC backends incrementally behind the already-defined `api/` adapters and stores.
- Rollback, if needed, by removing the scaffolded frontend files and reverting the new capability spec; there is no production data migration in this change.

## Open Questions

- Which backend path should become the default long-term runtime target for IM: UniCloud, self-hosted WebSocket service, or a hybrid approach?
- What exact provider contracts are available for image-to-image and video-to-video on Tencent Hunyuan and Alibaba Tongyi in the first implementation increment?
- Should Space authoring be read-only in the first scaffold, or should a minimal post composer ship alongside the feed?
- How much of MDX should be executable versus statically transformed for mobile safety and performance?
