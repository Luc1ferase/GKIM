## ADDED Requirements

### Requirement: Android scaffold follows the repository harness
The system SHALL initialize an Android application scaffold implemented with Kotlin and Jetpack Compose, and it MUST organize client responsibilities into Android-native UI, state, repository, remote, local, rendering, and model layers that preserve the repository harness intent.

#### Scenario: Scaffolded Android project structure is created
- **WHEN** the change is applied to initialize the project
- **THEN** the repository contains the mandated Android application configuration, package structure, and shared design system foundation for the IM app

#### Scenario: Screen logic is delegated to state and data layers
- **WHEN** a screen needs chat, contact, feed, workshop, or AIGC behavior
- **THEN** the implementation uses ViewModels, repositories, and shared services instead of direct API calls or complex business logic in Compose screen functions

### Requirement: Android client accesses protected infrastructure through service boundaries
The system SHALL access IM, feed, and AIGC backend capabilities through HTTPS and WebSocket service endpoints, and it MUST NOT embed direct PostgreSQL credentials or CA certificate material in the Android client runtime.

#### Scenario: Android app initializes remote connectivity
- **WHEN** the app configures its connected services
- **THEN** it uses API base URLs and WebSocket endpoints instead of a direct PostgreSQL DSN

#### Scenario: Protected infrastructure material stays outside the APK
- **WHEN** database credentials or database CA trust material are required
- **THEN** they are held by backend infrastructure and are not packaged into the Android app

## REMOVED Requirements

### Requirement: UniAPP scaffold follows the repository harness
**Reason**: The implementation contract is moving from a UniAPP cross-platform scaffold to an Android-native scaffold.
**Migration**: Replace this requirement with the new Android scaffold requirement defined in this change before /opsx:apply.
