## 1. Messages quick-action shell

- [x] 1.1 Replace the Messages header's passive active-conversation count with a `+` quick-action trigger and anchored dropdown menu that exposes `Add friend / 加好友` and `Scan QR code / 扫描二维码`.
- [x] 1.2 Extend authenticated navigation so the Messages quick actions can open the existing user-search flow plus new QR scan/result routes while preserving expected back-stack behavior.

## 2. QR scanning flow

- [x] 2.1 Add the Android camera/barcode dependencies and implement a dedicated QR scan screen with permission-aware camera preview, decode handling, and clean cancel/back behavior.
- [x] 2.2 Implement a passive QR result surface that shows the decoded payload content without auto-navigation, account mutation, or external-link side effects.

## 3. Real add-friend entry path

- [x] 3.1 Route the Messages `Add friend / 加好友` action into the live user-search/request flow so it reuses the existing backend-backed social workflow instead of a local demo path.
- [x] 3.2 Tighten the Messages-launched add-friend UX so backend request success, pending state, and request failure remain visible and truthful when testing with different real accounts.

## 4. Verification and evidence

- [x] 4.1 Add or refresh focused Android UI/instrumentation coverage for the Messages `+` menu, add-friend routing, QR scan/result navigation, and no-side-effect result behavior.
- [x] 4.2 Run focused local verification for cross-account friend add plus QR payload display, then capture verification/review/score/upload evidence in `docs/DELIVERY_WORKFLOW.md` before checking the tasks off.
