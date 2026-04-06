# Design System Strategy: The Kinetic Blueprint

## 1. Overview & Creative North Star
**Creative North Star: "The Digital Architect"**

This design system rejects the "standard app" aesthetic in favor of a high-end, editorial-tech experience. We are moving away from generic material containers toward a UI that feels like a precision-engineered instrument. By blending the minimalist restraint of early encryption tools with the polished depth of modern glassmorphism, we create a "geeky-chic" environment.

The system breaks the "template" look through **Intentional Asymmetry**. We utilize wide gutters, monospaced metadata accents, and high-contrast typography scales. The layout should feel "programmed" rather than "drawn," using technical precision to evoke trust, speed, and innovation.

---

## 2. Colors: Tonal Depth over Lines
Our palette is rooted in the deep void of `#060e20`, utilizing "Electric Indigo" (`primary: #9fa7ff`) as a high-energy pulse through the dark interface.

*   **The "No-Line" Rule:** 1px solid borders are strictly prohibited for sectioning. Structural boundaries must be defined solely through background shifts. For example, a chat list item sits on `surface-container-low`, while the active chat sits on `surface-container-high`. Boundaries are felt, not seen.
*   **Surface Hierarchy & Nesting:** Treat the UI as stacked sheets of obsidian and frosted glass.
    *   **Level 0 (Background):** `surface-dim` (#060e20) – The base canvas.
    *   **Level 1 (Sections):** `surface-container-low` (#091328) – For sidebars or inactive areas.
    *   **Level 2 (Cards/Active):** `surface-container-highest` (#192540) – For focused content.
*   **The "Glass & Gradient" Rule:** Floating elements (Modals, Overlays) must use Glassmorphism. Apply `surface-variant` at 60% opacity with a `24px` backdrop blur.
*   **Signature Textures:** Main CTAs should not be flat. Use a linear gradient from `primary` (#9fa7ff) to `primary-dim` (#5764f1) at a 135-degree angle to provide a "lit-from-within" tech glow.

---

## 3. Typography: Editorial Logic
We pair the human-centric **Inter** with the architectural **Space Grotesk** to create a "Technical Editorial" vibe.

*   **Display & Headlines (Space Grotesk):** Use `display-lg` to `headline-sm` for onboarding and major navigation headers. This font’s geometric quirks provide the "geeky-chic" soul.
*   **Body & Titles (Inter):** All functional communication uses Inter (`body-md` for chat bubbles). It is the workhorse that ensures the "fast and efficient" feel.
*   **Technical Accents (Monospaced):** All timestamps, file sizes, and metadata (e.g., `label-sm`) should be set in a monospaced font (system-default mono) to reinforce the high-tech, precise nature of the IM application.

---

## 4. Elevation & Depth: Tonal Layering
Traditional shadows and borders are replaced by **Atmospheric Perspective**.

*   **The Layering Principle:** To lift a component, do not add a shadow; shift the color token. A `surface-container-highest` element placed on a `surface` background creates a natural, sophisticated lift.
*   **Ambient Shadows:** Use only for floating action buttons or high-level modals. Shadows must be `0px 20px 40px` with a 6% opacity using the `primary` color shifted toward black, creating a "glow-shadow" rather than a grey smudge.
*   **The "Ghost Border" Fallback:** If a divider is essential for accessibility, use the `outline-variant` (#40485d) at **15% opacity**. It should be a suggestion of a line, a "ghost" of a boundary.
*   **Glassmorphism & Depth:** To separate the message input area from the chat thread, use a `surface-container` background with a `20px` backdrop blur. This allows the colors of shared media to bleed through subtly as they scroll under the input bar.

---

## 5. Components

### Buttons & Interaction
*   **Primary Action:** Gradient-filled (`primary` to `primary-dim`) with `0.375rem` (md) roundedness. Text is `on-primary-fixed` (Black) for maximum "pop" against the dark UI.
*   **Tertiary/Ghost:** No container. Use `primary` text with a monospaced `label-md` for a "terminal" feel.

### Messaging Elements
*   **Chat Bubbles:** 
    *   *Incoming:* `surface-container-high` with `xl` (0.75rem) corner radius. No border.
    *   *Outgoing:* `primary-container` (#8e98ff) with `on-primary-container` text.
*   **Input Fields:** Use `surface-container-lowest` (#000000) for the field background to create a "sunken" effect. The active state uses a 1px "Ghost Border" of `primary`.
*   **Chips:** Use `secondary-container` for filter chips. These should be strictly `full` (9999px) roundedness to contrast against the more angular, precise grid of the messages.

### Cards & Lists
*   **The No-Divider Rule:** Chat list items are separated by `12px` of vertical white space, not lines. A subtle background shift to `surface-bright` on press provides the necessary tactile feedback.

### Context-Specific: "The Encryption Badge"
*   A custom component for this application: A small, monospaced tag using `tertiary-container` text on `surface-dim` background, used to indicate "End-to-End Encrypted" status. It feels like a line of code embedded in the header.

---

## 6. Do’s and Don’ts

### Do
*   **Do** embrace negative space. High-tech doesn't mean cluttered; it means intentional.
*   **Do** use monospaced fonts for all numbers and data points.
*   **Do** use "Electric Indigo" (`primary`) sparingly as a wayfinding tool, not a wallpaper.

### Don’t
*   **Don’t** use pure white (`#ffffff`) for text. Use `on-surface` (#dee5ff) to reduce eye strain in dark environments.
*   **Don’t** use standard 1px borders or heavy drop shadows. They look "cheap" and "out-of-the-box."
*   **Don’t** use generic system icons. Opt for thin-stroke (1.5pt) linear icons that match the `outline` token color.
*   **Don’t** use "bouncy" animations. Transitions should be fast (`200ms`), linear, or "expo-out" to feel responsive and professional.