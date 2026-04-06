# Design System Specification: Cyber-Minimalist Mono

## 1. Overview & Creative North Star
**The Creative North Star: "The Architectural Glitch"**

This design system is a study in precision and atmosphere. It rejects the cluttered "dashboard" aesthetic in favor of high-end editorial clarity. By marrying the cold, geometric rigor of "Cyber" aesthetics with the breathable, quiet luxury of "Minimalism," we create a space that feels both advanced and human.

The system breaks the "template" look through **intentional asymmetry** and **tonal depth**. We do not use lines to separate ideas; we use light and shadow. The interface should feel less like a software UI and more like a high-tech terminal projected onto architectural glass—structured, translucent, and undeniably premium.

---

## 2. Colors & Surface Philosophy

The palette is anchored by deep space slates and pristine whites, unified by a singular, high-energy Indigo.

### The "No-Line" Rule
Traditional 1px solid borders are strictly prohibited for sectioning. Boundaries must be defined solely through background color shifts. A `surface-container-low` section sitting on a `surface` background provides all the separation a sophisticated eye needs.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. We use Material-aligned tokens to define "elevation" through color rather than height:
- **Surface (Base):** `#091328` (Dark) / `#F8FAFC` (Light). The foundation.
- **Surface-Container-Low:** The first level of nesting for secondary content.
- **Surface-Container-High:** Used for interactive elements or primary cards to create a "lift" effect.

### The "Glass & Gradient" Rule
To escape the "flat" look, floating elements (modals, dropdowns, floating nav) must utilize **Glassmorphism**:
- **Dark Mode:** `surface-container` at 60% opacity with a `20px` backdrop-blur.
- **Light Mode:** `white` at 70% opacity with a `12px` backdrop-blur.
- **Signature Textures:** Use a subtle linear gradient for primary CTAs: `primary` (#C3C0FF) to `primary-container` (#4F46E5) at a 135-degree angle.

---

## 3. Typography: The Geometric Voice

We utilize **Space Grotesk** as our primary voice. Its geometric construction provides a "tech-forward" spirit, while **Inter** is reserved for high-density body copy to ensure maximum legibility.

| Level | Token | Font | Size | Character |
| :--- | :--- | :--- | :--- | :--- |
| **Display** | `display-lg` | Space Grotesk | 3.5rem | Tight tracking (-2%), Bold |
| **Headline** | `headline-md` | Space Grotesk | 1.75rem | Medium, Editorial spacing |
| **Title** | `title-lg` | Inter | 1.375rem | Semi-bold, Functional |
| **Body** | `body-md` | Inter | 0.875rem | Regular, 1.6 line-height |
| **Label** | `label-md` | Space Grotesk | 0.75rem | All-caps, +5% letter spacing |

*Director’s Note: Use `display-lg` sparingly to anchor sections. The contrast between a massive geometric headline and small, precise body text creates the "Signature Editorial" look.*

---

## 4. Elevation & Depth

### The Layering Principle
Depth is achieved by stacking surface tiers. Instead of a shadow, place a `surface-container-lowest` card on a `surface-container-low` background. This creates a "recessed" or "inset" feel that is much cleaner than traditional shadows.

### Ambient Shadows
When a floating effect is non-negotiable (e.g., a primary action button or a modal):
- **Blur:** 32px to 64px (Extremely diffused).
- **Opacity:** 4% to 8%.
- **Color:** Use a tinted version of `on-surface`. Never use pure black `#000000` for shadows; it "muddies" the indigo accents.

### The "Ghost Border" Fallback
If a boundary is required for accessibility, use a **Ghost Border**:
- **Token:** `outline-variant` (#464555).
- **Opacity:** 15%.
- **Weight:** 1px.

---

## 5. Components

### Buttons: The Kinetic Strike
- **Primary:** Gradient fill (`primary` to `primary-container`). `0.25rem` (sm) corner radius. No border.
- **Secondary:** Transparent background with a `Ghost Border`. Text in `primary`.
- **States:** On hover, the primary button should "glow" by increasing the shadow spread of the indigo accent.

### Foldable Logic (Accordions)
- **Container:** No outer border. Use a `surface-container-low` background.
- **Indicator:** A custom-weighted chevron. When expanded, the chevron rotates 180 degrees and the stroke weight increases from `1px` to `2px` to signal the "active" state.
- **Spacing:** Use generous vertical padding (`2rem`) between expanded sections to maintain the minimalist breathability.

### Input Fields
- **Style:** Underline-only or subtle tonal shifts. 
- **Active State:** The bottom border transforms into a 2px indigo (`primary-container`) line. 
- **Typography:** Placeholder text uses `label-md` for a technical, data-entry feel.

### Cards & Lists
- **Rule:** Forbid divider lines. 
- **Separation:** Use `8px` to `16px` of white space or a subtle shift from `surface-container` to `surface-bright`.

---

## 6. Do’s and Don’ts

### Do
- **Use Asymmetry:** Align text to the left but place supporting imagery or data points on a custom offset grid.
- **Embrace Negative Space:** If a screen feels "empty," you are likely doing it right. Let the typography breathe.
- **Subtle Motion:** Use 300ms "ease-out" transitions for all hover and accordion states to mimic a high-end physical interface.

### Don’t
- **Don’t use 100% opaque borders:** They break the "Cyber" fluidity and feel like legacy web design.
- **Don’t crowd components:** Never place more than three primary actions on a single view.
- **Don’t use "Pure Black":** Always stick to the `#091328` Slate for Dark Mode. Pure black kills the depth of the indigo accents.
- **Don’t use standard corner radii:** Avoid the "pill" shape for everything. Use the `0.25rem` (sm) or `0.375rem` (md) scales to keep the geometric, architectural edge.