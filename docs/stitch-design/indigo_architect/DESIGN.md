# Design System Strategy: The Architectural Light

## 1. Overview & Creative North Star
**Creative North Star: "The Digital Atrium"**

This design system rejects the cluttered "chat bubble" aesthetic of legacy IM apps in favor of an architectural, editorial layout. We are building a space that feels like a modern gallery—open, filled with "daylight," and structured through volume rather than lines. 

To break the "template" look, we employ **Intentional Asymmetry**. In our layout, the sidebar might feel heavy and grounded (Surface Container High), while the main chat thread feels ethereal and expansive (Surface). By utilizing Space Grotesk’s geometric quirks against Inter’s Swiss-precision readability, we create a "Tech-Luxury" tension that signals high-end engineering and sophisticated taste.

---

## 2. Color & Tonal Depth
The palette is rooted in a "High-Key" light mode. We avoid pure blacks and heavy borders, opting for a spectrum of light that mimics the way light hits architectural surfaces.

### Surface Hierarchy & Nesting
We achieve depth through **Tonal Layering**. Instead of drawing boxes, we stack "materials."
- **The Base:** `surface` (#f7f9fb) is our floor. 
- **The Navigation:** Use `surface_container_high` (#e6e8ea) for sidebars to give them a structural, "anchored" feel.
- **The Content:** Use `surface_container_lowest` (#ffffff) for the primary message feed. This creates a "lifted" effect where the most important information feels closest to the user.

### The "No-Line" Rule
**Explicit Instruction:** Do not use 1px solid borders to separate the sidebar from the chat or the header from the feed. Boundaries must be defined by the transition from `surface_container_low` to `surface`. If a visual break is needed, use a 16px or 24px vertical gutter (Negative Space) rather than a line.

### The Glass & Gradient Rule
To provide "visual soul," use the **Signature Signature Texture**: 
- **Floating Headers:** Use `surface` at 80% opacity with a `backdrop-filter: blur(20px)`. This allows the message bubbles to softly bleed through as they scroll, maintaining a sense of spatial awareness.
- **Action Gradients:** For primary CTAs, use a subtle linear gradient from `primary` (#3525cd) to `primary_container` (#4f46e5). This adds a "weighted" feel to buttons that flat colors cannot replicate.

---

## 3. Typography: The Editorial Tech Tension
Our typography is a conversation between the "Geek" and the "Professional."

- **Space Grotesk (The Architect):** Reserved for `display`, `headline`, and `title` scales. Its monospaced-adjacent proportions give the app a technical, bespoke edge. Use it for Usernames in the chat header and Section Titles.
- **Inter (The Reader):** Used for all `body` and `label` scales. Inter is optimized for the rapid-fire reading of instant messaging. 

### Typographic Hierarchy
- **Headline-SM (Space Grotesk):** Use for "Active Chat" names. It should feel authoritative.
- **Body-MD (Inter):** The workhorse for chat messages. Set with a generous line-height (1.5) to ensure clarity during long-form reading.
- **Label-SM (Inter):** Use for timestamps. Set in `on_surface_variant` (#464555) to recede into the background until needed.

---

## 4. Elevation & Depth: The Layering Principle
We move away from the "drop shadow" era. Depth is now atmospheric.

- **Ambient Shadows:** Only use shadows for "Floating" elements (e.g., Modals or Pop-overs). Use a large spread: `box-shadow: 0 12px 40px rgba(25, 28, 30, 0.06)`. The tint is derived from `on_surface`, making it feel like a natural obstruction of light.
- **The "Ghost Border" Fallback:** If a UI element (like a search input) risks disappearing, use a "Ghost Border": `outline_variant` (#c7c4d8) at **15% opacity**. It should be felt, not seen.
- **Glassmorphism:** Navigation bars should always use a semi-transparent `surface_container_lowest` with a blur. This prevents the UI from feeling "chopped" and creates a continuous flow of light.

---

## 5. Components & Interface Elements

### Buttons
- **Primary:** Gradient fill (`primary` to `primary_container`), `on_primary` text. No border. Large `xl` (0.75rem) roundedness to feel approachable.
- **Secondary:** `surface_container_highest` fill. This should feel like a "carved" button out of the background.
- **Tertiary:** Pure text using `primary` color. No container.

### Messaging Cards
- **Forbid Dividers:** Do not use lines between messages.
- **Grouping:** Use vertical spacing. A gap of 4px between messages from the same user; 16px between different users.
- **The "Me" Bubble:** Use `primary_container` (#4f46e5) with `on_primary` text. 
- **The "Them" Bubble:** Use `surface_container_low` (#f2f4f6). This creates a soft, architectural recess.

### Input Fields
- **Search:** A "carved" look. Use `surface_container_low` with no border. When focused, transition to `surface_container_lowest` with a "Ghost Border."
- **Message Box:** Use Glassmorphism. A floating `surface_container_lowest` at 90% opacity, anchored to the bottom with a soft Ambient Shadow.

### Contextual Chips
- Use `secondary_fixed` (#d3e4fe) for status indicators (e.g., "In a Meeting"). The soft slate/blue tone signals information without the urgency of the Indigo primary.

---

## 6. Do’s and Don’ts

### Do:
- **Use "White Space" as a structural tool.** If the layout feels messy, increase the padding, don't add a border.
- **Maintain High-Key Contrast.** Ensure text on `surface_container` tiers always meets WCAG AA standards by using the `on_surface` tokens.
- **Align to a 4px Grid.** While the layout is asymmetrical, the underlying "bones" must be mathematically precise.

### Don’t:
- **Never use 100% Black.** Use `on_surface` (#191c1e) for maximum readability without the "vibration" of pure black on white.
- **Avoid Heavy Glows.** We use soft, diffused ambient shadows. A "glow" implies a light source from within the component; we want the component to feel like it is reflecting the "daylight" of the UI.
- **No Sharp Corners.** Use the `xl` (0.75rem) or `lg` (0.5rem) tokens for almost everything. Sharp corners feel aggressive; we want "Architectural Minimalism."