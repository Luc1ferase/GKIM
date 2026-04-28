## MODIFIED Requirements

### Requirement: Android scaffold follows the repository harness with the Tavern visual direction

The system SHALL initialize an Android application scaffold implemented with Kotlin and Jetpack Compose, and it MUST organize client responsibilities into Android-native UI, state, repository, remote, local, rendering, and model layers that preserve the repository harness intent. The shared design-system foundation MUST conform to the `tavern-visual-direction` capability — palette tokens come from the Tavern Dark / Light variants, typography binds to the bilingual humanist serif (headline) + humanist sans (body) chains, and the bottom navigation lands on Tavern by default with the `联系人` tab folded into the tavern surface. Companion-skin imagery MUST flow through the `companion-skin-gacha` capability — that is, every avatar / portrait / banner shown for a companion MUST be loaded from the Cloudflare R2 CDN domain `cdn.lastxuans.sbs` via the singleton Coil `ImageLoader` exposed at the app root, with `AvatarFallbackSilhouette` as the failure fallback. Companion-card data structures MUST carry an `activeSkinId` field whose value drives URL construction for the chat header and tavern card avatar slots.

#### Scenario: Scaffolded Android project structure is created

- **WHEN** the change is applied to initialize the project
- **THEN** the repository contains the mandated Android application configuration, package structure, and shared design system foundation for the IM app, with the design-system foundation reading palette and typography from the `tavern-visual-direction` capability and asset URL construction routed through the `companion-skin-gacha` capability's `skinAssetUrl` helper

#### Scenario: Screen logic is delegated to state and data layers

- **WHEN** a screen needs chat, contact, feed, workshop, or AIGC behavior
- **THEN** the implementation uses ViewModels, repositories, and shared services instead of direct API calls or complex business logic in Compose screen functions

#### Scenario: Authenticated startup lands on the tavern surface

- **WHEN** an authenticated session is restored on cold start
- **THEN** the start destination of the root nav graph is the `tavern` route, satisfying the bottom-navigation requirement of the `tavern-visual-direction` capability

#### Scenario: Companion avatar slots load through the CDN-backed image loader

- **WHEN** a tavern card or chat header renders a companion avatar
- **THEN** the avatar URL is constructed via `skinAssetUrl(card.id, card.activeSkinId, artVersion, variant)` against `cdn.lastxuans.sbs` and loaded through the singleton Coil `ImageLoader`, with `AvatarFallbackSilhouette` rendered on load failure

#### Scenario: Companion-card data structure carries the active skin

- **WHEN** the repository projects a `CompanionCharacterCard` for the UI
- **THEN** the projection includes an `activeSkinId: String` field whose value defaults to `"default"` when no `user_active_skin` row exists for the user-character pair, and whose value drives the URL construction in the tavern card and chat header avatar slots
