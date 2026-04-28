## MODIFIED Requirements

### Requirement: Application shell provides three primary mobile tabs
The system SHALL provide a fixed bottom navigation bar with Messages, Contacts, and a tavern-style character destination as the three primary destinations, and it MUST visually indicate the active tab using the design-system token set.

#### Scenario: User switches between primary tabs
- **WHEN** the user taps Messages, Contacts, or the tavern-style character destination in the bottom navigation
- **THEN** the application displays the corresponding top-level page and highlights the selected tab state

#### Scenario: Secondary pages do not replace the primary tab model
- **WHEN** the user opens chat detail, creative workshop, or settings
- **THEN** the application routes to a secondary page flow without redefining the primary tab set

### Requirement: Space feed renders developer-oriented rich posts
**Reason**: The AI companion branch no longer uses `Space` as a developer-content feed; the third tab is being repurposed into a tavern-style character lobby.
**Migration**: Replace the existing `Space` feed and discovery-filter implementation with a tavern/角色-lobby surface that supports preset角色浏览、抽卡和 owned roster selection.

## ADDED Requirements

### Requirement: Third tab presents a tavern-style character lobby instead of a feed
The system SHALL replace the current `Space` surface with a tavern-style character lobby that centers preset角色, draw entry, and the user’s owned roster. The top-level page MUST not render the old developer-post feed or prompt-discovery filter rail as the primary content of this tab.

#### Scenario: User opens the third tab
- **WHEN** the user enters the third primary tab on the AI companion branch
- **THEN** the app presents a character-lobby surface for role selection and draw actions instead of the old feed of posts and prompts

#### Scenario: Character lobby exposes draw and roster entry points
- **WHEN** the character-lobby surface renders
- **THEN** the page includes clear entry points for browsing preset角色, triggering a draw, and reviewing already owned角色 cards

### Requirement: Android app routes selected角色 into the companion chat flow
The system SHALL treat the selected角色 card as the active companion identity for the ensuing AI conversation flow, and it MUST let the user enter or resume the corresponding companion chat from the tavern-style character surface.

#### Scenario: User chooses a preset or owned角色 card
- **WHEN** the user activates a character card from the tavern roster
- **THEN** the app records that角色 as the current companion selection and can route the user into the corresponding companion conversation

#### Scenario: Draw result leads into usable companion selection
- **WHEN** the user finishes a draw and receives a usable角色 card
- **THEN** the app lets the user review or activate that角色 instead of dropping the result as a disconnected animation-only outcome
