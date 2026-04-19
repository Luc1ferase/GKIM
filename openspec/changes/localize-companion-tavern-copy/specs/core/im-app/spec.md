## ADDED Requirements

### Requirement: Companion roster surfaces render character copy in the active app language
The system SHALL resolve companion `displayName`, `roleLabel`, `summary`, and `openingLine` from the currently selected app language on tavern preset cards, owned cards, and draw-result surfaces, and it MUST keep that copy aligned with the active English/Chinese language preference instead of leaving companion content fixed to one shipped language.

#### Scenario: Chinese app language shows Chinese companion roster copy
- **WHEN** the user sets the app language to Chinese and opens the tavern roster or latest draw result
- **THEN** the visible companion card fields render the authored Chinese character copy instead of English-only text

#### Scenario: English app language shows English companion roster copy
- **WHEN** the user sets the app language to English and opens the tavern roster or latest draw result
- **THEN** the visible companion card fields render the authored English character copy

#### Scenario: Language switch refreshes existing tavern content
- **WHEN** the user changes the app language after companion roster data has already been loaded locally
- **THEN** the tavern roster and latest draw-result content refresh to the newly selected language without requiring a new draw or account reset

### Requirement: Companion chat identity follows the same localized copy contract
The system SHALL derive companion-chat identity labels from the same bilingual companion model used by the tavern, and it MUST present the active companion's visible name and role label in the current app language when the user enters or resumes companion chat from the roster.

#### Scenario: User activates a companion and enters chat in Chinese mode
- **WHEN** the app language is Chinese and the user activates a companion card from the tavern into chat
- **THEN** the resulting companion conversation entry uses the companion's Chinese display name and Chinese role label in the visible chat identity surface

#### Scenario: User activates a companion and enters chat in English mode
- **WHEN** the app language is English and the user activates a companion card from the tavern into chat
- **THEN** the resulting companion conversation entry uses the companion's English display name and English role label in the visible chat identity surface
