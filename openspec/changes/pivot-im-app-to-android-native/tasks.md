## 1. Android Foundation

- [ ] 1.1 Initialize the Android project scaffold with Kotlin, Jetpack Compose, Gradle configuration, and the package structure for `feature`, `core`, and `data` layers
- [ ] 1.2 Add the baseline dependencies for Navigation Compose, lifecycle ViewModel, Room, DataStore, encrypted key storage, Retrofit, OkHttp, Coil, and test tooling
- [ ] 1.3 Port the Aether Mono design tokens, typography, motion timings, and shared surface primitives into a reusable Android design-system layer

## 2. Shell And Primary Screens

- [ ] 2.1 Implement the root Android navigation shell with fixed bottom navigation for Messages, Contacts, and Space plus secondary routes for Chat, Workshop, and AI Settings
- [ ] 2.2 Build the Messages screen with empty-state and populated conversation rows showing nickname, latest message preview, timestamp, and unread badge count
- [ ] 2.3 Build the Contacts screen with the required sort control for nickname initial, earliest added time, and latest added time
- [ ] 2.4 Build the Space feed shell with developer-post cards and a native content renderer for Markdown, approved presentation metadata, and MDX-ready document nodes

## 3. Chat, Workshop, And Settings Flows

- [ ] 3.1 Implement the chat detail screen with message timeline, composer shell, and AIGC action entry points for text-to-image, image-to-image, and video-to-video
- [ ] 3.2 Implement the creative workshop flow with prompt discovery, template application, category browsing, and contribution placeholders with author attribution metadata
- [ ] 3.3 Implement the AI settings screen with Tencent Hunyuan and Alibaba Tongyi presets, custom OpenAI-compatible endpoint configuration, model selection, and secure API key persistence

## 4. Data, Security, And Device Integration

- [ ] 4.1 Create Room entities, DAOs, repositories, and seed data for conversations, contacts, Space posts, prompt templates, provider presets, and pending AIGC jobs
- [ ] 4.2 Implement Retrofit and OkHttp clients plus WebSocket connectivity behind repository interfaces, using service endpoints only and excluding any direct PostgreSQL access from the Android client
- [ ] 4.3 Implement the native Android media selection pipeline for photo and video inputs, preview handling, and staged generation request assembly
- [ ] 4.4 Document backend-only handling for the provided PostgreSQL DSN and `ca.pem`, including a non-client placement path for future server infrastructure

## 5. Verification

- [ ] 5.1 Add unit tests for ViewModels, repositories, provider configuration handling, and the rich-content rendering pipeline
- [ ] 5.2 Add Compose UI tests for tab navigation, conversation list states, contact sorting behavior, and AI settings interactions
- [ ] 5.3 Run the Android verification commands required by the scaffold and fix any issues needed to keep the change apply-ready
