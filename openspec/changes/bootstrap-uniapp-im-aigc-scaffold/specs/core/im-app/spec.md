## ADDED Requirements

### Requirement: UniAPP scaffold follows the repository harness
The system SHALL initialize a UniAPP x application scaffold that uses Vue 3, TypeScript, Pinia, Pinia persisted state, and UnoCSS, and it MUST organize source files according to the repository harness layers for pages, components, composables, stores, api, utils, styles, and types.

#### Scenario: Scaffolded project structure is created
- **WHEN** the change is applied to initialize the project
- **THEN** the repository contains the mandated frontend configuration and source directories for the UniAPP application shell

#### Scenario: Page logic is delegated to composables and stores
- **WHEN** a page needs chat, contact, feed, or AIGC behavior
- **THEN** the implementation uses stores and composables instead of placing direct API calls or complex business logic in the page file

### Requirement: Application shell provides three primary mobile tabs
The system SHALL provide a fixed bottom navigation bar with Messages, Contacts, and Space as the three primary destinations, and it MUST visually indicate the active tab using the design-system token set.

#### Scenario: User switches between primary tabs
- **WHEN** the user taps Messages, Contacts, or Space in the bottom navigation
- **THEN** the application displays the corresponding top-level page and highlights the selected tab state

#### Scenario: Secondary pages do not replace the primary tab model
- **WHEN** the user opens chat detail, creative workshop, or settings
- **THEN** the application routes to a secondary page flow without redefining the primary tab set

### Requirement: Messages tab summarizes conversations in a single-row list
The system SHALL present conversations as one row per contact showing nickname, latest message preview, message time, and unread badge count when unread messages exist, and it MUST provide an empty-state experience when no conversations are available.

#### Scenario: Conversation row includes unread metadata
- **WHEN** a conversation has unread messages
- **THEN** its row displays the contact nickname, latest message snippet, latest timestamp, and a numeric unread badge

#### Scenario: Empty conversation state is shown
- **WHEN** the user has no conversations in local state
- **THEN** the Messages page displays an empty-state panel instead of a blank list

### Requirement: Contacts tab supports deterministic sorting controls
The system SHALL provide a dropdown control on the Contacts page that allows sorting by nickname initial, added time ascending, and added time descending.

#### Scenario: User sorts contacts alphabetically
- **WHEN** the user selects the nickname-initial sorting option
- **THEN** contacts are grouped or ordered by nickname initial in ascending order

#### Scenario: User sorts contacts by oldest added time
- **WHEN** the user selects the earliest-added sorting option
- **THEN** contacts are ordered from the earliest added record to the most recent

#### Scenario: User sorts contacts by newest added time
- **WHEN** the user selects the latest-added sorting option
- **THEN** contacts are ordered from the most recent added record to the earliest

### Requirement: Space feed renders developer-oriented rich posts
The system SHALL provide a Space feed optimized for developer posts, and it MUST render Markdown content, CSS-authored presentation blocks, and MDX-compatible post documents through a shared content-rendering pipeline.

#### Scenario: Markdown developer post is rendered in the feed
- **WHEN** a Space post contains Markdown headings, paragraphs, lists, or code blocks
- **THEN** the feed renders the content with the shared developer-post renderer and design-system styles

#### Scenario: Styled post content uses scoped presentation rules
- **WHEN** a post includes supported CSS presentation metadata or style blocks
- **THEN** the renderer applies the supported styling without breaking feed layout or app theme tokens

#### Scenario: MDX-compatible post document enters the renderer
- **WHEN** a post is authored in the MDX-compatible content format defined by the app
- **THEN** the renderer resolves the document through the shared parsing abstraction instead of bypassing the content pipeline

### Requirement: Chat detail exposes AIGC generation entry points
The system SHALL provide AIGC actions inside the chat experience for text-to-image, image-to-image, and video-to-video creation, and it MUST allow the user to choose local photo or video inputs when the selected mode requires media input.

#### Scenario: User starts text-to-image generation in chat
- **WHEN** the user selects the text-to-image AIGC action from the chat interface
- **THEN** the app opens the generation flow with a prompt input experience tied to the shared AIGC composable

#### Scenario: User starts image-to-image generation with local media
- **WHEN** the user selects image-to-image and chooses a local photo
- **THEN** the app passes the chosen media and prompt data through the shared AIGC generation flow

#### Scenario: User starts video-to-video generation with local media
- **WHEN** the user selects video-to-video and chooses a local video
- **THEN** the app passes the chosen media and prompt data through the shared AIGC generation flow

### Requirement: Creative workshop supports prompt discovery and contribution
The system SHALL provide a creative workshop that helps users discover, reuse, and contribute prompts, and it MUST expose creator attribution data so future KOL/community features can build on the same content model.

#### Scenario: User applies a workshop template to generation
- **WHEN** the user selects a prompt template from the creative workshop
- **THEN** the template content is inserted into the downstream AIGC generation flow for editing or immediate use

#### Scenario: User contributes a new prompt template
- **WHEN** the user submits a prompt template to the creative workshop
- **THEN** the app stores the contribution in the workshop content model with author attribution metadata

### Requirement: AI settings support preset and custom infrastructure providers
The system SHALL provide an AI settings page with preset providers for Tencent Hunyuan and Alibaba Tongyi, and it MUST allow users to configure a custom OpenAI-compatible endpoint, API key, and model identifier.

#### Scenario: User activates a preset provider
- **WHEN** the user selects Tencent Hunyuan or Alibaba Tongyi in AI settings
- **THEN** the provider configuration store marks the preset as active for subsequent AIGC requests

#### Scenario: User configures a custom provider
- **WHEN** the user enters a custom API base URL, API key, and model in AI settings
- **THEN** the provider configuration store persists the custom configuration for the shared AIGC adapter layer
