# Tavern Visual Direction — Reference

This document is the **design reference** for the redesign slice. It is consulted during R1–R4 to keep token decisions consistent. Anything contradicting this file in implementation is a finding.

## North-star vibe

A small, low-lit cocktail bar at 21:30 — brass fixtures, aged paper menu, candlelight on the wood, leather seats, a record playing low. Warm, ambient, slightly intimate. **NOT** a SaaS dashboard, **NOT** a deep-space cyber app, **NOT** a Material 3 demo.

## Anti-patterns (user-stated)

1. Don't overuse rounded-pill buttons. Pills are reserved for **primary emotional actions**.
2. Don't overuse gradients, especially heavy dark-blue → dark-purple full-screen gradients.
3. Don't keep the IM-app skeleton (clean white surface + indigo accents + 100 % sans-serif).
4. Don't fix one screen — every chrome surface should breathe the same atmosphere.

## Palette tokens

### Tavern Dark (default)

| Token | Hex | Description |
|---|---|---|
| `surface` | `#1A0F0A` | deep espresso / smoked oak — the room itself |
| `surfaceContainerLow` | `#271812` | red-mahogany table |
| `surfaceContainerHigh` | `#3A2419` | warm cocoa for cards / chips |
| `surfaceContainerHighest` | `#4D2E1F` | brass-rim accents on raised surfaces |
| `surfaceLowest` | `#0E0805` | shadow / vignette base |
| `primary` | `#E0A04D` | brass / golden tequila — the bar's main metal |
| `primaryContainer` | `#A06135` | caramel / aged whisky |
| `secondary` | `#8B5E3C` | leather seat |
| `tertiary` | `#B85450` | ember red, used sparingly |
| `onSurface` | `#F4ECDD` | warm cream paper |
| `onSurfaceVariant` | `#B8A78D` | aged linen |
| `outlineVariant` | `#4A382C` | dark wood grain edge |
| `success` | `#7BA05B` | sage / muted absinthe green |
| `danger` | `#C24644` | dried garnish red |

### Tavern Light (aged-paper)

| Token | Hex | Description |
|---|---|---|
| `surface` | `#F1E7D2` | aged paper |
| `surfaceContainerLow` | `#F8F0DD` | unbleached linen |
| `surfaceContainerHigh` | `#E5D5B6` | vellum |
| `surfaceContainerHighest` | `#D8C49B` | cream card |
| `surfaceLowest` | `#EBDFC4` | shadow into paper |
| `primary` | `#8B4513` | saddle brown / aged spice |
| `primaryContainer` | `#C28E5A` | toasted caramel |
| `secondary` | `#705033` | walnut |
| `tertiary` | `#9B3C30` | terracotta |
| `onSurface` | `#2A1810` | espresso ink |
| `onSurfaceVariant` | `#705943` | walnut ink |
| `outlineVariant` | `#B19877` | aged paper edge |
| `success` | `#5E7E3F` | bottled-green olive |
| `danger` | `#A33A35` | cherry tincture |

### Use rules

- **Never** combine `primary` and `tertiary` in the same gradient. They are accent siblings, not gradient endpoints.
- Background-to-background transitions use `surface` ↔ `surfaceContainerLow` only; the delta is intentionally subtle (~ 9 % luminance shift) to avoid the heavy-gradient anti-pattern.
- High-contrast UI (active-state pill, danger-button text) reads on `onSurface`; never on `primary` directly (low contrast).

## Typography

### Families (chosen for OFL / no-cost packaging)

| Role | Latin | CJK | Source |
|---|---|---|---|
| Display / headline | **Newsreader** | **Noto Serif CJK SC** | both Google Fonts (OFL) |
| UI / body | **Inter** | **Noto Sans CJK SC** | both Google Fonts (OFL) |

> Future swap to Anthropic-style commercial faces (Copernicus / Styrene / Tiempos) is one-file: replace the `FontFamily` instances in `AetherFonts.kt`. All token names stay.

### Token roles

```
headlineLarge   Newsreader Bold      34sp / 38sp lh
headlineMedium  Newsreader SemiBold  28sp / 32sp lh
titleLarge      Newsreader SemiBold  22sp / 26sp lh
bodyLarge       Inter Regular        15sp / 24sp lh
bodyMedium      Inter Regular        13sp / 20sp lh
labelLarge      Inter Medium         12sp / +1.4 letter-spacing
```

### Use rules

- Headlines (`headline*` and `titleLarge`) are **always** the serif. No SansSerif on headlines anywhere in the app.
- Body, button labels, secondary text, captions are Inter. No serif body — keeps reading speed high.
- Never mix Latin Newsreader with CJK SansSerif on the same line; the bilingual-paired family map (Newsreader ↔ Noto Serif CJK SC) MUST stay.

## Component patterns

### Pill discipline

- Pills (rounded ≥ 18 dp) are reserved for **primary emotional actions** (e.g. `抽卡` / "Pour a drink", `开始对话` / "Take a seat"). Maximum **one** pill on screen at a time, optionally a second only if it's the duplicate-language localization of the same action.
- Admin actions (`设置`, `导入卡`, overflow menu items) use **rectangular containers** (8–10 dp radius) with `surfaceContainerHigh` background and lower visual weight, OR plain text-with-icon when in-line in a list.
- The Tavern home's current `[设置] [抽卡] [导入卡]` triple-pill row is an explicit anti-pattern to fix in R3.

### Avatar fallback

- A card without a portrait MUST fall back to a thematic placeholder (silhouette in `surfaceContainerHighest` with `primary`-tinted strokes), **not** a letter monogram circle.
- The placeholder uses the same shape as the portrait (rounded-square per existing tavern-card layout). Avoids the IM-style "AO" monogram on the tavern home.

### FAB framing

- The new-conversation FAB on `消息` should be a small thematic icon (door-bell / candle / chat-quill) on `primary` over `surfaceContainerHigh`, NOT a Material-default FloatingActionButton.

### Bottom-nav active state

- Active state replaces the Material pill-behind-icon with a 2 dp `primary` underline anchored at the icon's bottom edge. Matches the brass-fixture metaphor; reads warm.

## Ambient layer

### Texture

- A single shared PNG (`raw/tavern_grain.png`) at 1024×1024 px containing a low-frequency noise pattern (Perlin-style, very faint).
- Applied as an `Image` painter with `BlendMode.Overlay` at **≤ 8 % opacity** on top of `surface` and `surfaceContainerLow`.
- Adds material warmth without being visible as a "pattern" — should read as "the surface has weight" rather than "this has a texture."

### Candle-light glow

- A single radial brush per atmospheric surface (tavern home top, chat header).
- `Brush.radialGradient(0f to primaryContainer.copy(alpha = .04f), 1f to Color.Transparent)`, anchored top-left or top-right of the surface, radius ≈ surface diagonal.
- **≤ 5 %** opacity at peak. Practically invisible on flat inspection; visible as warmth in motion (when the user scrolls, the brush moves with the surface).
- Never combined with an additional gradient. The room has one candle, not five.

### Anti-patterns to avoid in the ambient layer

- **No full-screen indigo → magenta gradient** (the previous Aether style).
- **No animated shimmer / aurora effects** — the bar is calm, not a club.
- **No drop shadows ≥ 4 dp** on cards. Shadows feel modern Material; we want soft warmth, not floating cards.

## Out of scope for the redesign

- Chat bubble shape redesign (current rounded rectangle is fine; only color tokens change).
- Replacing all icons with thematic SVG illustrations — only the FAB and avatar fallback are explicitly thematic in this slice; standard Material icons elsewhere stay.
- Splash / welcome screen — has its own approved video backdrop contract.
- Any per-companion theming (cards picking accent colors). The whole app reads from one palette.
