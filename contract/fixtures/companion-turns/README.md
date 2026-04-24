# companion-turns contract fixtures

Canonical JSON payloads shared between `GKIM` (Android client) and `GKIM-Backend` (Rust server) for the companion-turn submit / regenerate HTTP routes and the `companion_turn.*` WebSocket events.

Every Kotlin DTO / `ImGatewayEvent` variant relevant to companion turns has a corresponding fixture file here. The Android client's snapshot tests and the backend's serde round-trip tests both consume these files.

## Sync policy

These files are duplicated byte-for-byte between:

- `GKIM/contract/fixtures/companion-turns/`
- `GKIM-Backend/contract/fixtures/companion-turns/`

**Changing a fixture requires a paired PR in both repos.** The canonical check is:

```
git diff --no-index GKIM/contract/fixtures/companion-turns/ GKIM-Backend/contract/fixtures/companion-turns/
```

If this diff is non-empty after a PR merges, one side has drifted — the two repos must be realigned before any downstream slice builds on top.

## Files

| File | Shape | Used by |
|------|-------|---------|
| `submit-request.json` | `CompanionTurnSubmitRequestDto` — body of `POST /api/companion-turns` | Android `ImBackendHttpClient.submitCompanionTurn`; Rust submit handler |
| `submit-response.json` | `CompanionTurnRecordDto` — 200 response of `POST /api/companion-turns` | Android `LiveCompanionTurnRepository`; Rust submit handler success path |
| `regenerate-request.json` | `CompanionTurnRegenerateRequestDto` — body of `POST /api/companion-turns/:turnId/regenerate` | Android `ImBackendHttpClient.regenerateCompanionTurn`; Rust regenerate handler |
| `event-started.json` | `ImGatewayEvent.CompanionTurnStarted` — WebSocket frame | Android `ImGatewayEventParser`; Rust WebSocket broadcaster |
| `event-delta.json` | `ImGatewayEvent.CompanionTurnDelta` — WebSocket frame | Android `ImGatewayEventParser`; Rust WebSocket broadcaster |
| `event-completed.json` | `ImGatewayEvent.CompanionTurnCompleted` — WebSocket frame | Android `ImGatewayEventParser`; Rust WebSocket broadcaster |

## ID conventions used in these fixtures

All fixtures refer to the same imaginary smoke turn so a reader can follow the lifecycle across files:

- `conversationId`: `conversation-daylight-listener-smoke`
- `activeCompanionId`: `daylight-listener`
- `turnId`: `turn-scripted-smoke-01`
- `messageId` (companion message): `companion-turn-scripted-smoke-01`
- `variantGroupId`: `variant-group-scripted-smoke-01`
- `clientTurnId` (submit): `client-turn-submit-smoke-01`
- `clientTurnId` (regenerate): `client-turn-regenerate-smoke-01`
- `startedAt`: `2026-04-24T12:00:00Z`
- `completedAt`: `2026-04-24T12:00:01.500Z`

## Not yet covered (scheduled for S3)

- `event-failed.json` (`companion_turn.failed`)
- `event-blocked.json` (`companion_turn.blocked`)
- Pending-turn snapshot response (`GET /api/companion-turns/:turnId`)
- Pending-turn list response (`GET /api/companion-turns/pending`)
