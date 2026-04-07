## Context

The repository currently documents a UniAPP x, Vue, Pinia, and UnoCSS harness for a cross-platform IM product, while the approved UI language and screen references live in the Aether Mono and Stitch design files. The new requirement narrows delivery to Android only and explicitly allows the implementation stack to change, but it does not allow changes to the approved frontend design, information architecture, or feature set.

This means the project now needs a native Android implementation contract that preserves the Messages, Contacts, Space, Chat, Workshop, and AI Settings experiences while replacing the old cross-platform assumptions with Android-specific choices for navigation, rendering, persistence, media access, and realtime messaging.

The earlier database connection string and CA certificate are still relevant to the overall system, but they are not appropriate runtime dependencies for a mobile client. Embedding PostgreSQL credentials or CA material inside the Android app would create an unsafe architecture, so this design treats database access as a backend responsibility and keeps the Android app on HTTPS and WebSocket service boundaries only.

## Goals / Non-Goals

**Goals:**
- Preserve the current Aether Mono visual contract, tab structure, and page behaviors on Android.
- Replace the UniAPP-oriented implementation contract with an Android-native stack that is better suited to responsive messaging, media ingestion, and device adaptation.
- Define an Android app foundation for realtime IM, offline cache, developer-oriented feed rendering, prompt workshop flows, and AI provider settings.
- Support text-to-image, image-to-image, and video-to-video entry points with native Android photo and video selection flows.
- Keep implementation boundaries clear so IM data, feed content, and provider configuration can evolve without rewriting the UI shell.

**Non-Goals:**
- Deliver a production-ready backend for messaging, feed publishing, or AIGC execution in this change.
- Allow the Android client to connect directly to PostgreSQL or carry raw database credentials.
- Change the approved visual direction, navigation hierarchy, or core product scope defined by the existing design references.
- Finalize advanced community ranking, moderation, billing, or creator monetization logic.

## Decisions

### 1. Use Kotlin plus Jetpack Compose as the Android implementation foundation
The Android client will use Kotlin, Jetpack Compose, Navigation Compose, and a custom Aether Mono theme layer that ports the existing visual tokens into Compose primitives instead of relying on default Material visuals. This keeps the UI native and high performance while preserving the approved frontend appearance.

Why this decision:
- Compose gives the best Android-native control over motion, layout, typography, and adaptive surfaces without fighting a cross-platform abstraction.
- The app depends on rich interactions such as media picking, realtime chat, dynamic feed rendering, and polished transitions that benefit from native APIs.
- The user explicitly removed the cross-platform constraint, so the stack can optimize for Android quality first.

Alternatives considered:
- Keeping UniAPP: rejected because the product no longer needs iOS parity and Android-only delivery can benefit from native media, storage, and rendering behavior.
- Flutter or React Native: rejected because they still introduce an abstraction layer that is less aligned with deep Android integration and Kotlin-first maintenance.

### 2. Translate the harness layers into Android-native feature and data boundaries
The old pages, components, composables, stores, api, utils, and types responsibilities will be preserved conceptually, but expressed as Android packages such as `feature/*`, `core/designsystem`, `core/model`, `core/rendering`, `data/local`, `data/remote`, and `data/repository`. Screens stay thin, ViewModels own state, repositories coordinate data sources, and shared rendering or provider logic lives outside the UI layer.

Why this decision:
- It preserves the architecture intent of the repository harness without forcing Vue-specific directory names onto a Kotlin codebase.
- Feature-first packaging keeps Messages, Contacts, Space, Chat, Workshop, and Settings isolated and easier to test.
- It reduces the risk of business logic leaking into composables or screen files.

Alternatives considered:
- A flat package structure: rejected because it would become hard to navigate once IM, feed, workshop, and AIGC features grow.
- A screen-heavy architecture with direct network calls: rejected because it would recreate the layering problems the harness was trying to prevent.

### 3. Use ViewModel plus StateFlow for screen state, Room for local domain cache, DataStore for non-secret settings, and encrypted storage for API keys
Each top-level feature will expose UI state from an Android `ViewModel` through `StateFlow`. Conversations, contacts, feed entries, prompt templates, and pending AIGC jobs will use Room-backed local persistence. Non-secret preferences such as sort mode and selected provider preset will use DataStore, while user-supplied API keys are stored through Android secure storage instead of plain text persistence.

Why this decision:
- IM and feed surfaces benefit from resilient local cache and predictable restore behavior.
- StateFlow integrates cleanly with Compose and keeps screen logic testable.
- Provider credentials are sensitive and should not share the same persistence path as ordinary UI preferences.

Alternatives considered:
- In-memory state only: rejected because the app needs offline-friendly behavior and durable configuration.
- SharedPreferences for all settings: rejected because it is weaker for typed state and secret handling.

### 4. Use Retrofit and OkHttp for HTTPS APIs, OkHttp WebSocket for realtime IM, and repositories as the only integration boundary
The Android app will speak to backend services over HTTPS and WebSocket only. IM events, feed content, workshop templates, and provider presets load through repository interfaces backed by remote data sources. Realtime chat uses OkHttp WebSocket, and HTTP APIs use Retrofit with Kotlin serialization.

Why this decision:
- The app needs a straightforward transport model for chat, feed, and AIGC requests.
- OkHttp covers both HTTP and WebSocket needs with consistent interceptors, logging, and TLS behavior.
- A repository boundary allows scaffold-level mock data now and backend integration later without rewriting screens.

Alternatives considered:
- Direct PostgreSQL from Android: rejected because it is insecure, hard to evolve, and not acceptable for shipped mobile clients.
- Separate networking stacks for HTTP and WebSocket: rejected because it adds complexity without clear benefit for the first implementation.

### 5. Keep database credentials and the CA certificate on the backend side, not inside the Android app
The provided PostgreSQL DSN and `ca.pem` are treated as backend infrastructure inputs. If a supporting backend module is created later, the certificate should live in a server-side infrastructure path such as `infra/certs/` or a backend configuration directory, and the mobile app should consume only the backend API and WebSocket endpoints exposed above that layer.

Why this decision:
- Shipping raw database credentials in an APK is a severe security risk.
- TLS trust for PostgreSQL belongs to the runtime that owns the database connection, not to the mobile presentation client.
- This keeps the mobile app replaceable and allows backend hardening, auditing, and rate limiting.

Alternatives considered:
- Bundling the CA and database DSN into the app: rejected for security and operational reasons.
- Using direct database access only during development: rejected because it encourages an unsafe integration path that later has to be unwound.

### 6. Render developer posts through a native content pipeline that is Markdown-first, CSS-aware, and MDX-ready without arbitrary runtime execution
Space posts and prompt workshop guides will use a shared parser and renderer pipeline that turns Markdown and approved presentation metadata into a typed content model rendered by Compose. The model remains MDX-ready by supporting extension nodes and structured metadata, but the Android client will not execute arbitrary embedded code.

Why this decision:
- The product differentiator depends on developer-oriented rich posts rather than plain text cards.
- A native renderer preserves visual consistency with the Aether Mono design and avoids WebView-heavy layouts.
- Restricting execution keeps the app safer and more predictable on Android.

Alternatives considered:
- Full runtime MDX execution: rejected because it is heavy, difficult to secure, and unnecessary for the scaffold milestone.
- Rendering everything in WebView: rejected because it weakens app feel, complicates theming, and hurts performance.

### 7. Use native Android media pickers and a staged AIGC input pipeline
Image-to-image and video-to-video generation will use Android Photo Picker or Storage Access Framework selection flows, optional camera or capture integrations where needed, and a staged local input model that records media URI, prompt text, provider choice, and desired output mode before a request is submitted. Previews use Coil and long-running generation tasks can later be reconciled with WorkManager if background retry becomes necessary.

Why this decision:
- Native media selection is a core part of the AIGC experience and needs reliable Android behavior.
- A staged input model keeps chat actions and workshop templates interoperable.
- It avoids embedding provider-specific upload logic directly into chat screens.

Alternatives considered:
- Browser-style file selection wrappers: rejected because they would feel less native and may behave inconsistently across Android devices.
- Separate generation flows for each provider: rejected because it would fragment the user experience and multiply maintenance cost.

## Risks / Trade-offs

- [Dropping cross-platform delivery reduces code reuse] -> Mitigation: accept the trade in favor of better Android quality and document the Android-native contract clearly for future reuse or backend sharing.
- [Porting the visual system from UnoCSS references to Compose may introduce fidelity gaps] -> Mitigation: treat the Stitch screens and Aether Mono tokens as non-negotiable references and build a dedicated design-system layer before feature screens.
- [Preset provider execution paths may differ from custom OpenAI-compatible endpoints] -> Mitigation: normalize provider capability metadata and keep request execution behind a gateway interface.
- [Rich developer content can grow beyond safe mobile rendering scope] -> Mitigation: support Markdown and approved presentation extensions first, while keeping arbitrary MDX execution out of scope.
- [Native media flows increase Android API-surface complexity] -> Mitigation: centralize picker and generation input handling in dedicated feature modules instead of scattering it across screens.

## Migration Plan

- Create a new Android application scaffold during /opsx:apply with Kotlin, Compose, Gradle configuration, and the package structure that reflects the decisions above.
- Port the Aether Mono token system and shared surface primitives first, then implement the three-tab shell and the secondary Chat, Workshop, and AI Settings screens with mock-backed state.
- Add local persistence, repository interfaces, provider configuration storage, and the rich content renderer so the app remains interactive before backend contracts are finalized.
- Integrate real backend endpoints later behind the same repositories and transport clients without changing the approved screen structure.
- Roll back, if needed, by removing the Android scaffold and reverting this capability delta; no production data migration is required at proposal stage.

## Open Questions

- Will Tencent Hunyuan and Alibaba Tongyi requests be executed through a backend relay in the first implementation, or should the Android client call vendor endpoints directly when configured?
- What backend contract will serve IM conversations, Space feed content, and workshop templates in the first connected milestone?
- Does the first Android milestone need explicit tablet or foldable adaptations beyond responsive phone layouts?
- How much authoring capability, if any, should the first Space iteration include beyond feed browsing and rich post rendering?
