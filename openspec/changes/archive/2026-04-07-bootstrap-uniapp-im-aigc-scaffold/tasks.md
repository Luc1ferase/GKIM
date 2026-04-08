## 1. Project Setup

- [ ] 1.1 Initialize the UniAPP x project scaffold with Vue 3, TypeScript, and the repository-aligned source directory structure under `src/`
- [ ] 1.2 Add and configure Pinia, persisted state, UnoCSS theme tokens, shared fonts, and baseline tooling needed by the harness
- [ ] 1.3 Create shared types, seed/mock datasets, and utility modules for conversations, contacts, Space posts, workshop prompts, and provider settings

## 2. App Shell And Navigation

- [ ] 2.1 Implement the root mobile shell with fixed bottom navigation for Messages, Contacts, and Space plus route wiring for chat, workshop, and settings pages
- [ ] 2.2 Build shared layout and visual primitives that encode the Aether Mono token system, glass surfaces, typography, and active-tab feedback
- [ ] 2.3 Implement the Messages page with empty state support and conversation rows showing nickname, latest message preview, timestamp, and unread badge count

## 3. Contacts And Space Surfaces

- [ ] 3.1 Implement the Contacts page with the required dropdown sorting options for nickname initial, oldest added time, and newest added time
- [ ] 3.2 Implement the Space feed shell with developer-post cards and a shared renderer path for Markdown, scoped CSS presentation blocks, and MDX-ready content documents
- [ ] 3.3 Implement the creative workshop page with searchable prompt templates, category filters, template selection flow, and contribution metadata placeholders

## 4. AIGC Foundation

- [ ] 4.1 Implement the chat detail page shell with AIGC entry points for text-to-image, image-to-image, and video-to-video flows plus local media pickers
- [ ] 4.2 Create the `useAIGC` composable, provider adapter interfaces, and API-layer normalization for Tencent Hunyuan, Alibaba Tongyi, and custom OpenAI-compatible endpoints
- [ ] 4.3 Implement the AI settings page with preset provider toggles, custom endpoint/model fields, and persisted provider selection/configuration

## 5. Verification

- [ ] 5.1 Add Vitest coverage for key stores, composables, and rich-post rendering utilities introduced by the scaffold
- [ ] 5.2 Run the relevant verification commands for type checking, tests, and app scaffold integrity, then fix any issues required to keep the change apply-ready
