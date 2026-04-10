## MODIFIED Requirements

### Requirement: Application shell provides three primary mobile tabs
The system SHALL provide a fixed bottom navigation bar with Messages, Contacts, and Space as the three primary destinations, and it MUST visually indicate the active tab using the design-system token set. The Contacts tab MUST display a badge or indicator when there are pending incoming friend requests.

#### Scenario: User switches between primary tabs
- **WHEN** the user taps Messages, Contacts, or Space in the bottom navigation
- **THEN** the application displays the corresponding top-level page and highlights the selected tab state

#### Scenario: Secondary pages do not replace the primary tab model
- **WHEN** the user opens chat detail, creative workshop, settings, user search, login, or register
- **THEN** the application routes to a secondary page flow without redefining the primary tab set

#### Scenario: Contacts tab shows pending request badge
- **WHEN** the user has pending incoming friend requests
- **THEN** the Contacts tab in the bottom navigation displays a count badge

### Requirement: Contacts tab supports deterministic sorting controls
The system SHALL provide a compact inline dropdown pill on the Contacts page that allows sorting by nickname initial, added time ascending, and added time descending. The sort control MUST render as a single pill-shaped element displaying the active sort label with a dropdown indicator, and it MUST NOT occupy a full-width card or include explanatory text. The Contacts page header MUST display only the page title without an eyebrow label, description paragraph, or settings action. The Contacts page MUST provide entry points for user search and display pending friend requests above the contact list.

#### Scenario: User sorts contacts alphabetically
- **WHEN** the user selects the nickname-initial sorting option
- **THEN** contacts are grouped or ordered by nickname initial in ascending order

#### Scenario: User sorts contacts by oldest added time
- **WHEN** the user selects the earliest-added sorting option
- **THEN** contacts are ordered from the earliest added record to the most recent

#### Scenario: User sorts contacts by newest added time
- **WHEN** the user selects the latest-added sorting option
- **THEN** contacts are ordered from the most recent added record to the earliest

#### Scenario: Sort control renders as a compact inline pill
- **WHEN** the Contacts page renders its sort controls
- **THEN** the screen shows a single pill-shaped dropdown displaying the current sort label and a dropdown indicator, without a surrounding card, heading label, or explanatory text

#### Scenario: Contacts header shows title only
- **WHEN** the Contacts page renders its page header
- **THEN** the header displays only the "Contacts / 联系人" title without an eyebrow, description, or settings action

#### Scenario: Contacts page provides search entry point
- **WHEN** the user views the Contacts page
- **THEN** a search action or button is visible that navigates to the user search screen
