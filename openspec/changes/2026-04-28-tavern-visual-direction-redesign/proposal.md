## Why

The Android client carries the visual DNA of the IM tool it pivoted from: a cold "Aether" palette (deep navy `#091328` → indigo `#4F46E5` → lavender `#C3C0FF`), 100 % sans-serif typography, and a Material-default bottom nav that treats `酒馆` as a third tab next to `消息` / `联系人`. Pure IM apps are functional; the tavern needs to deliver **emotional value** — the chill, ambient feeling of a small cocktail bar — and the current chrome actively undercuts that. The user's own audit pointed out three load-bearing problems: rounded-pill overuse, heavy dark-blue-to-purple gradients, and a missing "cocktail bar chill" atmosphere.

This proposal is a directed visual + IA redesign that swaps the foundation (palette + typography) without rewriting layouts, fixes the IM-residue information architecture (nav + empty-state copy), prunes component-pattern noise (pill overuse, letter avatars, generic FAB), and finally adds a restrained ambient layer (texture / light hints). It is delivered as four ordered sub-slices (R1 → R4) so each stage produces a shippable UI delta on its own and the next builds on it.

## What Changes

### R1 — Foundation (palette + typography swap, no layout edits)

- Replace `AetherTheme.kt`'s `DarkAetherPalette` and `LightAetherPalette` with the warm "Tavern" palette (deep espresso / oxblood + brass / amber / caramel; chill-bar dark and aged-paper light variants).
- Add packaged font assets: **Newsreader** (humanist serif, OFL — Anthropic's Copernicus look-alike), **Inter** (UI sans, OFL — Styrene look-alike), **Noto Serif CJK SC** + **Noto Sans CJK SC** (CJK headings + body).
- Update `AetherTypography` so `headline*` / `titleLarge` use Newsreader (Latin) / Noto Serif CJK SC (Chinese), and `body*` / `labelLarge` use Inter / Noto Sans CJK SC.
- Document the palette + typography token contract in spec form so future changes do not silently re-introduce SaaS-cold colors or all-sans-serif headlines.
- No layout / component / route change in R1.

### R2 — Information architecture (mental model fix)

- Make Tavern (`酒馆`) the default landing tab on authenticated startup instead of `消息`.
- Fold `联系人` into Tavern; the app's contact list **is** the companion roster, so a separate top-level tab is redundant. Bottom nav reduces from three tabs (`消息 / 联系人 / 酒馆`) to two (`酒馆 / 消息`), with `消息` framed as "active conversations" rather than a generic IM inbox.
- Rewrite IM-style empty-state and onboarding copy across the conversation list, contact-search-result fallbacks, and post-relationship-reset states to be in-character ("the bar is empty, pull up a stool …" rather than "no active conversations").

### R3 — Component-pattern pruning

- Audit pill-button overuse on the tavern home (`设置 / 抽卡 / 导入卡`) and chat top-bar; reserve pill shape for **primary emotional actions** (e.g. `抽卡` / `开始对话`); demote admin actions (`设置`, `导入卡`) to text-with-icon or rectangular containers with smaller weight.
- Replace the letter-fallback avatar (`AO`-style monogram circle) with a thematic placeholder (silhouette / wax-seal motif) for cards without a high-resolution portrait.
- Reframe the new-conversation FAB (`+` filled circle on top right of `消息`) into a thematic affordance ("invite a guest" / candle-flame / door-bell icon, retaining the same tap-target).
- Replace the bottom nav's Material-default pill-behind-icon active state with a warm under-line indicator that matches the new palette.

### R4 — Ambient layer (restrained, not gradient-heavy)

- Add a low-opacity (≤ 8 %) paper / wood grain texture overlay on `surface` and `surfaceContainerLow` so flat color blocks gain subtle material warmth.
- Add a sparse ambient "candle-light" highlight (single radial brush, ≤ 5 % opacity, fixed position per surface) on the tavern home and the chat header — the **anti-pattern** is the dark-blue-to-purple full-screen gradient the user explicitly asked to avoid; this slice is the calibrated counter-example showing how ambient depth can be added with restraint.
- Tighten gacha-result animation to use the new palette's brass / candle accents instead of the current high-saturation lavender / pink flashes.

## Capabilities

### New Capability

- `tavern-visual-direction` — owns the design-token contract (palette + typography), the bottom-nav IA rule, and the component-pattern guardrails (pill discipline, avatar fallback, FAB framing, ambient-layer restraint). Future visual changes write deltas against this capability rather than free-styling the chrome.

### Modified Capability

- `core/im-app` — gets a delta narrowing the scaffold's design-system requirement to the new token contract and adjusting the nav/landing requirement to point at Tavern.

## Impact

- **Affected Android code (R1)**: `core/designsystem/AetherTheme.kt` (palette + typography), new `core/designsystem/AetherFonts.kt` exposing the four font families, `app/src/main/res/font/*.ttf` packaged assets, build manifest entries that bundle the OFL license texts. No call-site changes — palette tokens (`AetherColors.Surface`, etc.) keep their public names.
- **Affected Android code (R2)**: `feature/navigation/GkimRootApp.kt` (default route + nav reduction), `feature/messages/MessagesRoute.kt` and `feature/social/UserSearchRoute.kt` (empty-state copy), `feature/tavern/TavernRoute.kt` (acts as the contact-list integration target). The "联系人" tab's existing screen merges into Tavern with a folded section rather than being deleted, so muscle memory survives.
- **Affected Android code (R3)**: `feature/tavern/TavernRoute.kt` (pill audit), `feature/chat/ChatRoute.kt` top-bar (pill audit), shared avatar fallback composable, the new-conversation FAB site, the bottom-nav composable.
- **Affected Android code (R4)**: a new `core/designsystem/AmbientLayer.kt` exposing texture + glow modifiers, applied opt-in to the tavern home and chat header.
- **Affected specs**: new `tavern-visual-direction` capability; `core/im-app` delta.
- **Tests**: each task names a Kotlin presentation test or instrumentation test that verifies the new contract (palette tokens are read from the right source, typography families resolve to the packaged assets, nav routes land on Tavern, empty-state copy is bilingual, avatar fallback renders the silhouette, ambient overlays apply only to the named surfaces).
- **Backend**: zero changes. This is a 100 % client-side initiative.
- **Non-goals (scoped out)**:
  - KLIM Copernicus / Styrene / Tiempos commercial licensing — left as a future swap; the token contract is font-family-agnostic so the swap is one-file.
  - Per-companion theming (cards picking their own accent colors).
  - Animated portraits / sprites / parallax depth in chat bubbles.
  - Tablet / foldable layout density rework.
  - Dark/light auto-switch by ambient sensor.
  - Re-skinning of authenticated-onboarding (welcome-screen) since that capability has its own approved video backdrop and motion contract.
  - Any change to login / register / settings inner panels' layout. Visual tokens flow through automatically; no per-screen layout pass.
