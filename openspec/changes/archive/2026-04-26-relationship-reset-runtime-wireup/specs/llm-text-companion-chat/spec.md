## ADDED Requirements

### Requirement: MessagingRepository exposes a relationship-reset method that delegates to the backend and reconciles local conversations

The `MessagingRepository` SHALL expose `resetRelationship(characterId): Result<Unit>`. The `LiveMessagingRepository` impl SHALL delegate to `ImBackendClient.resetRelationship(characterId)` (which calls `POST /api/relationships/{characterId}/reset`), and on success SHALL remove every `Conversation` whose `companionCardId == characterId` from the local `conversations` StateFlow before returning. Wire failures SHALL be surfaced as `Result.failure(throwable)` whose message is a stable error code so the UI can render localized copy without parsing exception types.

#### Scenario: Successful reset removes the matching conversations from the local cache

- **WHEN** the local `conversations` StateFlow contains entries with `companionCardId` values `["c1", "c2", "c3"]` and `LiveMessagingRepository.resetRelationship("c2")` returns `Result.success`
- **THEN** the StateFlow's next emission contains exactly the conversations with `companionCardId` in `["c1", "c3"]`; the conversation with `companionCardId == "c2"` is removed

#### Scenario: HTTP 403 character_not_available maps to error code `character_not_available`

- **WHEN** `ImBackendClient.resetRelationship` throws an HTTP exception with status 403 carrying the backend's `character_not_available` error body
- **THEN** the method returns `Result.failure` whose throwable message is `character_not_available` and the local conversations cache is NOT mutated

#### Scenario: Network failure maps to `network_failure` and leaves the cache intact

- **WHEN** `ImBackendClient.resetRelationship` throws an `IOException`
- **THEN** the method returns `Result.failure` whose throwable message is `network_failure` and the local conversations cache is NOT mutated

#### Scenario: Missing base URL or token short-circuits without calling backend

- **WHEN** `baseUrlProvider()` or `tokenProvider()` returns null
- **THEN** the method returns `Result.failure` immediately and `ImBackendClient.resetRelationship` is NOT invoked
