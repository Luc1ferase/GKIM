## ADDED Requirements

### Requirement: The Android client carries a single Tavern design-token palette with brass-on-espresso dark and aged-paper light variants

The system SHALL define exactly one shared palette object exposed via `core/designsystem/AetherTheme.kt`. The Tavern Dark variant MUST anchor on `surface = #1A0F0A` (deep espresso) with `primary = #E0A04D` (brass / golden-tequila) and warm-cocoa surface containers stepping `#271812 → #3A2419 → #4D2E1F`. The Tavern Light variant MUST anchor on `surface = #F1E7D2` (aged paper) with `primary = #8B4513` (saddle brown) and linen-based surface containers stepping `#F8F0DD → #E5D5B6 → #D8C49B`. The deep-navy `#091328` / electric-indigo `#4F46E5` / lavender `#C3C0FF` triplet from the prior "Aether" palette MUST NOT appear in either variant.

#### Scenario: Tavern Dark surface and primary tokens resolve to the brass-on-espresso pair

- **WHEN** a composable in dark theme reads `AetherColors.Surface` and `AetherColors.Primary`
- **THEN** the values are `#1A0F0A` and `#E0A04D` respectively

#### Scenario: Tavern Light surface and primary tokens resolve to the saddle-brown-on-paper pair

- **WHEN** a composable in light theme reads `AetherColors.Surface` and `AetherColors.Primary`
- **THEN** the values are `#F1E7D2` and `#8B4513` respectively

#### Scenario: The prior cold palette is forbidden

- **WHEN** any source file under `app/src/main/` is searched for the literal hex values `#091328`, `#4F46E5`, or `#C3C0FF`
- **THEN** zero matches are found outside of archived openspec changes and ADR notes

### Requirement: Headlines use a humanist serif and body uses a humanist sans, both bilingual

The system SHALL render `Typography.headlineLarge`, `Typography.headlineMedium`, and `Typography.titleLarge` using a humanist-serif `FontFamily` chain (Newsreader → Noto Serif CJK SC fallback) exposed as `AetherFonts.DisplaySerif`, and SHALL render `Typography.bodyLarge`, `Typography.bodyMedium`, and `Typography.labelLarge` using a humanist-sans `FontFamily` chain (Inter → Noto Sans CJK SC fallback) exposed as `AetherFonts.UiSans`. Default-platform `FontFamily.SansSerif` and `FontFamily.Default` MUST NOT appear in `AetherTypography`.

#### Scenario: Headline roles bind to the serif chain

- **WHEN** a composable resolves `MaterialTheme.typography.headlineLarge.fontFamily`
- **THEN** the resolved family equals `AetherFonts.DisplaySerif`

#### Scenario: Body roles bind to the sans chain

- **WHEN** a composable resolves `MaterialTheme.typography.bodyMedium.fontFamily`
- **THEN** the resolved family equals `AetherFonts.UiSans`

#### Scenario: Bilingual rendering uses paired family endpoints

- **WHEN** a Chinese-language headline is rendered
- **THEN** the glyphs fall back to `Noto Serif CJK SC` rather than to the platform default Han face

### Requirement: Bottom navigation reduces to two tabs and lands on Tavern by default

The system SHALL render exactly two bottom-navigation tabs, in order: `酒馆` / `Tavern` first, `消息` / `Messages` second. The previously-existing `联系人` / `Contacts` top-level tab MUST NOT appear in the bottom navigation; its companion-list content MUST be reachable from a folded section inside the Tavern home. On authenticated startup, the start destination of the root nav graph MUST be the `tavern` route, not the `messages` route.

#### Scenario: Two-tab nav with tavern first

- **WHEN** an authenticated user opens the app
- **THEN** the bottom navigation displays exactly two tabs whose label keys are `tavern` and `messages` in that order

#### Scenario: Authenticated startup lands on Tavern

- **WHEN** an authenticated session is restored on cold start
- **THEN** the start destination of the root nav graph is the `tavern` route

#### Scenario: Companion list survives the contacts-tab removal

- **WHEN** a user opens the tavern home
- **THEN** an "All companions" section is reachable from the tavern home and contains every companion the user has acquired

### Requirement: Pill-shaped containers are reserved for one primary emotional action per surface

The system SHALL apply rounded-pill geometry (corner radius ≥ 18 dp + chromatic emphasis using palette `primary`) to **at most one** call-to-action per visible surface, and that single pill MUST be the surface's primary emotional action (e.g. `抽卡` on the tavern home, `开始对话` / `Take a seat` on a card-detail surface). Admin and secondary actions (`设置` / `Settings`, `导入卡` / `Import card`, overflow triggers, export-chat menu items) MUST use rectangular containers (corner radius ≤ 12 dp) with `surfaceContainerHigh` backgrounds, OR plain text-with-icon when in-line within a list.

#### Scenario: Tavern home holds exactly one pill

- **WHEN** the tavern home is rendered
- **THEN** exactly one container has corner radius ≥ 18 dp + `primary` chromatic emphasis, and its action is `抽卡` / `Pour a drink`

#### Scenario: Tavern home admin actions are rectangular

- **WHEN** the tavern home is rendered
- **THEN** the `设置` and `导入卡` triggers each render as rectangular containers with corner radius ≤ 12 dp and `surfaceContainerHigh` background

### Requirement: Avatar fallback renders a thematic silhouette rather than a letter monogram

The system SHALL render the avatar fallback (when no portrait is available) as a generic upper-body silhouette in `surfaceContainerHighest` with a 1 dp `primary`-tinted stroke, preserving the surrounding shape (rounded-square for tavern cards, circle for chat avatars). The system MUST NOT render letter or initial monograms as avatar fallbacks.

#### Scenario: Tavern card without portrait shows silhouette

- **WHEN** a tavern card with no uploaded portrait is rendered
- **THEN** the avatar slot shows the silhouette placeholder, not a letter circle

#### Scenario: Chat avatar without portrait shows silhouette

- **WHEN** a chat header or chat bubble for a companion with no portrait is rendered
- **THEN** the avatar slot shows the silhouette placeholder in a circle, not a letter circle

### Requirement: Ambient layer is bounded and applied opt-in to chrome surfaces only

The system SHALL provide two ambient modifiers (`Modifier.tavernGrain()` for a paper / wood texture overlay capped at 8 % opacity, `Modifier.candleGlow(anchor: Alignment)` for a single radial highlight capped at 5 % opacity using `primaryContainer`). The ambient layer MUST be applied **only** to the tavern home outer column and the chat header surface in this slice; no other surface MAY apply it without an explicit follow-up requirement. The full-screen indigo → magenta gradient style MUST NOT appear on any surface.

#### Scenario: Tavern home applies grain + glow once

- **WHEN** the tavern home is composed
- **THEN** `Modifier.tavernGrain()` and `Modifier.candleGlow(Alignment.TopEnd)` are each applied exactly once to its outer column

#### Scenario: Chat header applies glow only

- **WHEN** the chat header is composed
- **THEN** `Modifier.candleGlow(Alignment.TopStart)` is applied exactly once and `Modifier.tavernGrain()` is not applied

#### Scenario: Other chrome surfaces opt out

- **WHEN** any non-tavern-home, non-chat-header chrome surface is composed
- **THEN** neither `Modifier.tavernGrain()` nor `Modifier.candleGlow(...)` is applied
