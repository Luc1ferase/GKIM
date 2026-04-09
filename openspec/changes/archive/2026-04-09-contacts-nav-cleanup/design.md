## Context

The GKIM Android app has three primary tabs: Messages, Contacts, and Space. Currently both Messages and Contacts carry a settings pill button, while Space has none. The Contacts screen also has an "Address Mesh / 联系人网络" eyebrow, a description paragraph, and a full-width sort card with explanatory text — all of which consume vertical space before the user sees any contacts.

**Current settings entry points:**
- `MessagesRoute.kt` — `PillAction("Settings")` in the header row (line 108)
- `ContactsRoute.kt` — `PageHeader(actionLabel = "Settings")` (lines 121–122)
- `SpaceRoute.kt` — no settings action

**Current Contacts chrome stack (top → down):**
1. Eyebrow: "ADDRESS MESH / 联系人网络"
2. Title row: "Contacts / 联系人" + settings pill
3. Description: "按昵称或加入时间排序联系人…"
4. Sort GlassCard: "SORT ORDER" label + explanation text + dropdown pill
5. Contact list

## Goals / Non-Goals

**Goals:**
- Consolidate the settings entry point to the Space page only
- Remove visual clutter from the Contacts header (eyebrow, description, sort explanation)
- Redesign the sort control into a compact inline pill that occupies a single row, not a full GlassCard

**Non-Goals:**
- Changing the Settings page itself or its contents
- Modifying the bottom navigation tab structure
- Altering contact list rendering or contact row design
- Adding new navigation routes

## Decisions

### D1: Settings pill lives exclusively on Space `PageHeader`

**Choice**: Add `actionLabel`/`onAction` to the existing `PageHeader` in `SpaceRoute` and remove settings wiring from `MessagesRoute` and `ContactsRoute`.

**Rationale**: Space is the app's discovery/feed hub and the natural home for app-level controls. Having one settings entry reduces redundancy and keeps Messages/Contacts focused on their data.

**Alternative considered**: Keep settings on Messages too — rejected because it adds no discoverability (Space is always one tap away via bottom nav).

### D2: Contacts sort control becomes an inline pill row

**Choice**: Replace the sort `GlassCard` (which includes "SORT ORDER" label, explanation text, and dropdown) with a single `Row` containing a sort pill that shows the current sort label with a `▾` indicator. Tapping it opens the same `DropdownMenu`.

**Rationale**: The current card wastes ~80dp of vertical space on static text. A pill-style control (consistent with `PillAction` design tokens) communicates the same affordance in one line.

### D3: Strip Contacts `PageHeader` to title-only

**Choice**: Remove `eyebrow`, `description`, and `actionLabel`/`onAction` params from the `PageHeader` call in `ContactsScreen`. The header becomes just the "Contacts / 联系人" title.

**Rationale**: The eyebrow and description add no functional value and push the contact list below the fold on smaller screens.

## Risks / Trade-offs

- **Discoverability of Settings** → Users who learned to access settings from Messages/Contacts need to discover the Space page entry. Mitigation: Space is a primary tab, always visible in bottom nav.
- **Sort control compactness** → The new pill may be less obvious as an interactive control. Mitigation: The `▾` suffix and `SurfaceContainerHigh` background match the existing dropdown convention.
