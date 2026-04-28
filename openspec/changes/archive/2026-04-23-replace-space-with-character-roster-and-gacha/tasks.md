## 1. Product contract shift

- [x] 1.1 Inventory the current `Space` feed/prompt surfaces that must be removed or repurposed, then update the branch-local product docs and specs so the third tab is defined as a tavern-style role lobby.
- [x] 1.2 Define shared domain models for character cards, preset roster entries, draw outcomes, owned roster state, and active角色 selection across Android and backend layers.

## 2. Android tavern experience

- [x] 2.1 Replace the current `feature/space` feed UI with a tavern-style role-selection surface that supports preset角色 browsing, draw entry, and owned-roster review.
- [x] 2.2 Update navigation, labels, and chat-entry flows so activating a角色 card routes the user into the corresponding companion conversation path.
- [x] 2.3 Add clear draw-result presentation and roster-state handling so a newly obtained角色 can be reviewed and activated instead of appearing as a disconnected one-off reward.

## 3. Backend roster and draw state

- [x] 3.1 Add backend support for preset character catalogs, per-user owned roster persistence, and active角色 selection.
- [x] 3.2 Implement a character draw operation that returns explicit draw results and updates the user’s owned roster truthfully.

## 4. Verification and delivery evidence

- [x] 4.1 Add focused Android and backend coverage for tavern rendering,角色 activation, draw outcomes, and conversation handoff behavior.
- [x] 4.2 Record verification, review, score, and upload evidence in `docs/DELIVERY_WORKFLOW.md` for the Space-to-tavern replacement slice.
