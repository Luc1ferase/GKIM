## Why

The Contacts screen has accumulated redundant chrome — an "Address Mesh / 联系人网络" eyebrow, a helper description, a sort explanation sentence, and a settings pill that also appears on Messages. The settings entry point belongs on the Space page (the app's discovery hub), and the sort control takes too much vertical real estate. Cleaning these up reduces visual noise, frees screen space for the contact list, and gives the Space page its natural home for app-level actions.

## What Changes

- **Move settings button to Space page**: The `PageHeader` in `SpaceRoute` gains the `actionLabel`/`onAction` settings pill currently present on Contacts and Messages.
- **Remove settings button from Messages**: The `PillAction` "Settings / 设置" pill in `MessagesScreen` header row is removed, along with the `onOpenSettings` callback.
- **Remove settings button from Contacts**: The `actionLabel`/`onAction` props on `PageHeader` in `ContactsScreen` are removed, along with the `onOpenSettings` callback.
- **Remove eyebrow from Contacts header**: The `eyebrow = "Address Mesh" / "联系人网络"` prop on `PageHeader` in `ContactsScreen` is removed.
- **Remove description from Contacts header**: The `description` prop ("按昵称或加入时间排序联系人…") on `PageHeader` in `ContactsScreen` is removed.
- **Remove sort explanation text**: The "为联系人列表选择一种排序方式。" / "Choose one ordering for the contact lane." `Text` inside the sort `GlassCard` is removed, along with the "SORT ORDER / 排序方式" label above it.
- **Compact sort control**: The sort card is redesigned from a two-column GlassCard (label+explanation on left, dropdown on right) to a single inline pill-style dropdown that occupies one row, reducing vertical space.

## Capabilities

### New Capabilities

_(none — all changes modify existing screens)_

### Modified Capabilities

- `core/im-app`: Contacts page header loses eyebrow, description, and settings action; sort control becomes a compact inline pill; Messages page loses settings pill; Space page gains settings pill.
