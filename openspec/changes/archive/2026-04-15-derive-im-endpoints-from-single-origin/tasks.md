## 1. Single-origin IM endpoint model

- [x] 1.1 Replace the separate IM HTTP and WebSocket preference contract with a single backend-origin source of truth, including migration or compatibility handling for already-stored endpoint values.
- [x] 1.2 Extend the shared IM endpoint resolver so it normalizes the backend origin, derives the WebSocket endpoint centrally, and enforces stricter release-versus-debug safety rules for allowed schemes and hosts.
- [x] 1.3 Update auth, bootstrap, contacts, search, and realtime connection flows to consume the shared resolved origin contract instead of reading separate transport-specific endpoint inputs.

## 2. User-facing settings and guarded validation override

- [x] 2.1 Remove separate raw IM HTTP/WebSocket entry from the normal production-facing settings path and replace it with a status or environment summary built around the resolved backend origin.
- [x] 2.2 Add or adapt a guarded developer-validation flow so testers can still override one backend origin plus validation identity without restoring the old two-field endpoint model for ordinary users.

## 3. Verification and operator guidance

- [x] 3.1 Add or refresh focused unit coverage for preference migration, single-origin derivation, and release-versus-debug safety validation.
- [x] 3.2 Add or refresh instrumentation or integration coverage for the new settings behavior, guarded override path, and auth or IM flows that depend on the resolved origin.
- [x] 3.3 Update Android or operator documentation and record delivery evidence for the single-origin endpoint contract, including how guarded validation override works after the UI change.
