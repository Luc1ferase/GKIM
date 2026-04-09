## 1. Regression Coverage

- [ ] 1.1 Update Android UI coverage for the shared shell-heading rhythm across `Messages`, `Contacts`, and `Space`, including the Contacts top-band sort placement and the tighter first-row position.
- [ ] 1.2 Update Android UI coverage so `Messages` no longer renders the live IM status card and `Settings > IM Validation` shows the live IM status near the backend endpoint inputs.

## 2. Shell Layout and Status Relocation

- [ ] 2.1 Implement the aligned top-band heading treatment across `Messages`, `Contacts`, and `Space`, and move the Contacts sort dropdown into the same row as the `Contacts / 联系人` title without changing sorting behavior.
- [ ] 2.2 Move the visible live IM validation status from `Messages` into `Settings > IM Validation`, reusing the existing messaging integration state instead of introducing a duplicate status source.
