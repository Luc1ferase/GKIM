## 1. Remove settings from Messages

- [x] 1.1 Remove the `onOpenSettings` callback and `PillAction("Settings")` from `MessagesScreen` header row, and remove the `onOpenSettings` parameter from `MessagesRoute`

## 2. Strip Contacts header and remove settings

- [x] 2.1 Remove `eyebrow`, `description`, and `actionLabel`/`onAction` params from the `PageHeader` call in `ContactsScreen`; remove the `onOpenSettings` parameter from `ContactsRoute`

## 3. Remove sort explanation text and compact sort control

- [x] 3.1 Replace the sort `GlassCard` (containing "SORT ORDER" label, explanation text, and dropdown) with a compact inline `Row` containing a single pill-shaped dropdown showing the active sort label + `▾` indicator, preserving the existing `DropdownMenu` behavior

## 4. Add settings to Space page

- [x] 4.1 Add `actionLabel`/`onAction` settings pill to `PageHeader` in `SpaceRoute`, wiring navigation to the settings page
