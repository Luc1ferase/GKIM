## MODIFIED Requirements

### Requirement: Android scaffold follows the repository harness with the Tavern visual direction
The system SHALL initialize an Android application scaffold implemented with Kotlin and Jetpack Compose, and it MUST organize client responsibilities into Android-native UI, state, repository, remote, local, rendering, and model layers that preserve the repository harness intent. The shared design-system foundation MUST conform to the `tavern-visual-direction` capability — palette tokens come from the Tavern Dark / Light variants, typography binds to the bilingual humanist serif (headline) + humanist sans (body) chains, and the bottom navigation lands on Tavern by default with the `联系人` tab folded into the tavern surface.

#### Scenario: Scaffolded Android project structure is created
- **WHEN** the change is applied to initialize the project
- **THEN** the repository contains the mandated Android application configuration, package structure, and shared design system foundation for the IM app, with the design-system foundation reading palette and typography from the `tavern-visual-direction` capability

#### Scenario: Screen logic is delegated to state and data layers
- **WHEN** a screen needs chat, contact, feed, workshop, or AIGC behavior
- **THEN** the implementation uses ViewModels, repositories, and shared services instead of direct API calls or complex business logic in Compose screen functions

#### Scenario: Authenticated startup lands on the tavern surface
- **WHEN** an authenticated session is restored on cold start
- **THEN** the start destination of the root nav graph is the `tavern` route, satisfying the bottom-navigation requirement of the `tavern-visual-direction` capability
