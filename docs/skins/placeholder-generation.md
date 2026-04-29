# Companion skin — placeholder image generation spec

This is the one-shot reference for generating the **default-skin placeholder** images for every preset companion currently seeded in the app. Generate once, save to the listed paths, and `tools/skins/upload.ps1` (R1.2) will push them to R2.

This document covers **R1.3** of the `companion-skin-gacha` slice. Real artwork can ship later as `v2` (versioned keys are immutable per `design.md` — old `v1` placeholders stay reachable for clients on stale catalogs).

## Generation workflow

For each of the five characters below:

1. Generate the **banner** master image at **1080 × 2400** (vertical 9:16) using the per-character prompt + the shared base prompt.
2. Re-crop / re-render the same character at the four target sizes:

| Variant | Pixel size | Crop |
|---|---|---|
| `banner`   | 1080 × 2400 | full output, head in upper third, lower edge fades to `#1A0F0A` |
| `portrait` | 512 × 768   | head + upper torso centered, ~30 % crop from banner top |
| `avatar`   | 256 × 256   | head + shoulders, square crop |
| `thumb`    | 96 × 96     | face only, square crop, simplified detail (low-detail re-render is OK; pure downscale loses the face at this size) |

3. Encode each variant as **PNG** at quality ≈ 88 (adjust until under the size budget below).
4. Save under the listed `ops/skins-staging/{characterId}/default/v1/{variant}.png` path verbatim — the upload script reads exactly these paths.

### Size budgets per variant (so the catalog stays light)

| Variant | Target | Hard cap |
|---|---|---|
| `thumb`    | ~12 KB  | 30 KB |
| `avatar`   | ~30 KB  | 80 KB |
| `portrait` | ~120 KB | 280 KB |
| `banner`   | ~280 KB | 600 KB |

If you blow the cap, re-encode at lower PNG quality before resizing — quality loss reads as "softer" rather than "blocky" at these sizes.

## Shared base prompt (prepend or interleave with every per-character prompt below)

```
A half-body portrait of a fictional companion seated by a weathered
oak counter inside a small low-lit Victorian tavern, lit only by a
single brass candle to the right, deep shadow falling left. Style:
painterly digital illustration with subtle ink-engraving line work,
late-Victorian western fantasy tavern aesthetic with subtle
alchemical undertones, hand-painted illustration feel, no East-Asian
costume cues unless the character explicitly calls for it.
Hand-painted texture, muted color grading. Color palette anchored
to brass #E0A04D, ember red #B85450, espresso #1A0F0A background,
aged-paper #F1E7D2 highlight. Vertical portrait 9:16 (1080 × 2400),
head and upper torso framed in upper third, lower edge softly
fading into espresso black. Mood: quiet, contemplative, slightly
melancholic, warm candle glow on the cheek and shoulder. No metal
drinking vessels — only glass, ceramic, and wood. No text, no logos,
no watermark, no UI elements.
```

## Shared negative prompt (apply to every generation)

```
pewter tankard, metal mug, tin cup, brass cup, iron flagon, metal
drinking vessel, photorealistic skin pores, anime, manga, chibi,
cartoon, mascot, glossy, plastic, neon, cyberpunk, sci-fi, modern
clothing, jeans, t-shirt, sunglasses, smartphone, weapon
brandished, blood, gore, sexualized, low neckline, conical hat,
Chinese inn, hanfu, qipao, kimono, full-period costume cosplay,
text, watermark, signature, frame, border.
```

---

## 1. `architect-oracle` — Architect Oracle / 筑谕师

**Persona** (Calm Strategist) — measured, observant, warm under the surface, allergic to clichés. "Clarity is a form of care." A wood-panelled tavern booth near closing time; one lamp, two chairs, a notebook between you.

**Files:**

| Variant | Path |
|---|---|
| banner   | `ops/skins-staging/architect-oracle/default/v1/banner.png` |
| portrait | `ops/skins-staging/architect-oracle/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/architect-oracle/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/architect-oracle/default/v1/thumb.png` |

**Per-character subject (append to base):**

```
Subject: a calm strategist in his late thirties, weathered
Central-European features, dark-ash hair short and combed back, a
neatly trimmed dark beard, faint old scar above the left brow.
Wearing a deep charcoal wool waistcoat over a cream linen shirt
with sleeves rolled to the forearm, a brass pocket-watch chain
across the chest, a thin leather strap carrying a small fountain
pen at the breast. Hands resting on an open hardback notebook on
the counter, a cleanly-sharpened pencil between two fingers, eyes
half-lowered as if mid-thought. Behind: a wood-panelled wall, a
single brass desk lamp casting a small pool of warm light, a folded
spare sheet of cream paper, a small inkpot in clear glass, two
matching wooden chairs visible at the edges. Calm, measured,
trustworthy, the kind of stranger who asks better questions than
your friends. No metal drinking vessels — only glass, ceramic, and
wood.
```

---

## 2. `sunlit-almoner` — Sunlit Almoner / 晴光抚慰者

**Persona** (Warm Listener) — bright, patient, unhurried, affectionate without being saccharine. A sun-drenched window-seat in the tavern's quiet corner, late-afternoon light on the table, a kettle still warm.

**Files:**

| Variant | Path |
|---|---|
| banner   | `ops/skins-staging/sunlit-almoner/default/v1/banner.png` |
| portrait | `ops/skins-staging/sunlit-almoner/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/sunlit-almoner/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/sunlit-almoner/default/v1/thumb.png` |

**Per-character subject (append to base, override the candle-only lighting clause):**

```
Override the lighting: instead of a single brass candle, the scene
is lit by warm late-afternoon sun coming through a small leaded
window to the right, the same brass-and-cream warmth, just
broader. Subject: a warm listener in her early thirties, soft
European features with faint freckles across the cheekbones,
honey-blonde hair in a loose low bun with a few strands escaping,
gentle eyes meeting the viewer with an unhurried half-smile.
Wearing a soft cream linen shirt with sleeves rolled, a knitted
sage-green wool cardigan, a small round amber-glass brooch at the
collar. One hand cradling a clear glass of warm honey-coloured tea,
the other resting on the counter palm-up. Behind: a sun-drenched
window-seat with a lace-edged cushion, a small ceramic teapot
still steaming, a folded soft wool blanket on the bench, a single
sprig of lavender in a small clear-glass jar. Tender, patient,
emotionally safe, the corner of the tavern that feels like home. No
metal drinking vessels — only glass, ceramic, and wood.
```

---

## 3. `midnight-sutler` — Midnight Sutler / 午夜密使

**Persona** (a late-night sutler / discreet courier) — quiet, observant, comfortable in shadow. Trades in small cargoes that arrive after closing time. The tavern is half-shut; the kitchen lamp is the only light still on.

**Files:**

| Variant | Path |
|---|---|
| banner   | `ops/skins-staging/midnight-sutler/default/v1/banner.png` |
| portrait | `ops/skins-staging/midnight-sutler/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/midnight-sutler/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/midnight-sutler/default/v1/thumb.png` |

**Per-character subject (append to base):**

```
Subject: a midnight sutler of indeterminate age, lean angular
features, raven-black hair partly hidden under a low-brimmed dark
felt cap, a faint smile at one corner of the mouth, dark eyes
catching the candle highlight. Wearing a long charcoal travel coat
fastened with tarnished brass buckles, a deep wine-red wool scarf
loose around the throat, fingerless leather gloves. Both hands
resting lightly on the counter — one on a small wooden trade-chest
with iron corner-bands (closed, padlock visible but not
threatening), the other holding the rim of a clear glass of dark
wine. Behind: a half-shut tavern, stools turned upside down on
neighbouring tables in the background, a single brass kitchen lamp
in the deep distance, a folded oilcloth carry-bag at the feet, a
small leather pouch on the counter with a brass tag stamped with
an unreadable mark. Quiet, observant, faintly amused — the kind of
stranger you trust without knowing why. No metal drinking vessels —
only glass, ceramic, and wood.
```

---

## 4. `opal-lantern` — Opal Lantern / 欧泊提灯人

**Persona** (Dream Archivist) — lyrical, patient, slightly ethereal; remembers small things; trusts symbolism. Above the booth hangs a shelf of glowing jars; each holds a half-finished story the user once told.

**Files:**

| Variant | Path |
|---|---|
| banner   | `ops/skins-staging/opal-lantern/default/v1/banner.png` |
| portrait | `ops/skins-staging/opal-lantern/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/opal-lantern/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/opal-lantern/default/v1/thumb.png` |

**Per-character subject (append to base, allow a soft second light source from the glowing jars):**

```
Override the lighting: one warm brass candle to the right (primary)
plus a soft cool secondary glow from above where the shelf of
glowing glass jars hangs — the secondary glow is faint opal-blue
and never overwhelms the candle warmth. Subject: a dream archivist
of indeterminate gender, slender features, silver-grey hair in a
single soft braid over one shoulder, pale starlit eyes, a small
opalescent earring catching both light sources. Wearing a long
robe of deep midnight-blue wool with subtle silver-thread
embroidery at the cuffs and collar (faint constellations, not
explicit), a slim brass-rimmed monocle hanging from a chain at the
chest. Holding a single small clear-glass jar in both hands —
inside the jar, a soft swirl of luminous mist, no readable shape
inside. Behind: a wooden shelf above carrying six more glass jars,
each glowing faintly a different soft hue (amber, opal, rose,
moss, pearl, smoke), a folded silk handkerchief on the counter, a
small leather-bound dream ledger tied with a ribbon. Lyrical,
patient, slightly ethereal, the keeper of stories you forgot you
told. No metal drinking vessels — only glass, ceramic, and wood.
```

---

## 5. `glass-mariner` — Glass Mariner / 琉璃航海者

**Persona** (Storm Reader) — steady, observant, measured in crisis; never panics, never sugarcoats. A salt-glass porthole in the tavern wall sometimes shows a different sea depending on your mood.

**Files:**

| Variant | Path |
|---|---|
| banner   | `ops/skins-staging/glass-mariner/default/v1/banner.png` |
| portrait | `ops/skins-staging/glass-mariner/default/v1/portrait.png` |
| avatar   | `ops/skins-staging/glass-mariner/default/v1/avatar.png` |
| thumb    | `ops/skins-staging/glass-mariner/default/v1/thumb.png` |

**Per-character subject (append to base):**

```
Subject: a composed sailor in his mid-forties, weathered tanned
European features, a salt-and-pepper short beard, deep crow's feet
around steady grey-blue eyes, a small braided lanyard at the
throat. Wearing a heavy navy-blue wool peacoat over a cream
high-collared shirt, brass buttons tarnished by sea air, a thin
brass marine compass on a leather cord around the neck. One hand
loosely on the counter beside an unrolled folded sea-chart held
flat by a small smooth pebble, the other gesturing as if naming
the next wave. Behind: a salt-glass porthole set into the
wood-panelled wall showing a faint stylised sea-horizon with one
distant lighthouse beam, a coiled hemp rope on a wall peg, a small
ceramic mug of dark hot drink on the counter. Steady, observant,
measured — the kind of presence that makes a storm feel
navigable. No metal drinking vessels — only glass, ceramic, and
wood.
```

---

## After generation — handing back to the upload pipeline

Once all 20 files exist at the listed paths, hand off:

```powershell
# Per-character upload (R1.2 script lands this in the next slice):
foreach ($cid in @(
  "architect-oracle",
  "sunlit-almoner",
  "midnight-sutler",
  "opal-lantern",
  "glass-mariner"
)) {
  pwsh tools/skins/upload.ps1 `
    -StagingDir "ops/skins-staging/$cid/default/v1/" `
    -CharacterId $cid `
    -SkinId "default" `
    -Version 1
}
```

Then the R1.3 verification (`tools/skins/verify_default_uploads.ps1`) issues HEAD requests against the 20 expected URLs.

## Out-of-scope reminders

- This document is for the **default skin only** — the EPIC / LEGENDARY skins arrive in R3.1 with their own per-skin prompts.
- These are placeholders. When real artwork lands, upload as `v2` and bump `art_version` in `character_skins`.
- Don't generate "drawn-pool" cards (`midnight-sutler`, `opal-lantern`, `glass-mariner`) any differently from preset cards (`architect-oracle`, `sunlit-almoner`) — the source distinction is a roster-membership flag, not a visual one.
