## MODIFIED Requirements

### Requirement: Chat detail exposes AIGC generation entry points
The system SHALL provide a standard chat composer with a text input field and send action as the primary control path, and it MUST expose distinct secondary actions for AIGC generation, normal chat media attachments, and generation-source media selection from the `+` menu inside the chat experience. Image-to-image and video-to-video actions MUST only appear or enable as ready-to-run actions when both the active provider supports the mode and the required local source media for that mode has been selected explicitly for generation.

#### Scenario: User sends plain text from the primary composer
- **WHEN** the user is in chat detail and enters text without opening secondary actions
- **THEN** the screen provides a text input field with a send control on the right side as the primary messaging action

#### Scenario: User opens the secondary action menu
- **WHEN** the user taps the `+` affordance in the chat composer
- **THEN** the app reveals a secondary action menu that separates normal chat media attachment actions from AIGC generation actions instead of relying on one ambiguous `Pick image` path

#### Scenario: User picks an image to send as a normal chat message
- **WHEN** the user chooses the chat image attachment action from the secondary menu and selects a local photo
- **THEN** the app stages that photo as a normal outgoing chat attachment rather than treating it as image-to-image input

#### Scenario: User picks an image as image-to-image source media
- **WHEN** the user chooses the generation-source image action from the secondary menu and selects a local photo
- **THEN** the app stages that photo as image-to-image input and keeps it separate from normal outgoing chat attachments

#### Scenario: User starts text-to-image generation in chat
- **WHEN** the user selects the text-to-image AIGC action from the secondary composer menu
- **THEN** the app opens the generation flow with a prompt input experience tied to the shared AIGC composable

#### Scenario: Image-to-image only becomes runnable after source image selection
- **WHEN** the active provider supports image-to-image but the user has not selected a generation-source image yet
- **THEN** the chat `+` menu does not present image-to-image as a ready-to-run action and instead guides the user to choose generation source media first

#### Scenario: User starts image-to-image generation with explicit source media
- **WHEN** the active provider supports image-to-image, the user has selected a generation-source photo explicitly for AIGC, and the user chooses image-to-image
- **THEN** the app passes that staged generation-source media and prompt data through the shared AIGC generation flow

#### Scenario: User starts video-to-video generation with local media
- **WHEN** the user selects video-to-video from the secondary composer menu and chooses a local video as generation source media
- **THEN** the app passes the chosen generation-source media and prompt data through the shared AIGC generation flow

## ADDED Requirements

### Requirement: Successful generated images expose follow-up actions
The system SHALL expose actionable follow-up controls on successful generated-image results, and it MUST let the user either save the generated image locally or send it into the current conversation without auto-sending the result by default.

#### Scenario: User saves a generated image locally
- **WHEN** a generated image result succeeds and the user selects the save action from the result surface
- **THEN** the app exports the generated image to local device storage and reports whether the save completed successfully

#### Scenario: User sends a generated image into the active conversation
- **WHEN** a generated image result succeeds and the user selects the send action from the result surface
- **THEN** the app inserts the generated image into the current conversation as a normal outgoing image message

#### Scenario: Successful generation does not auto-send without user confirmation
- **WHEN** an image generation succeeds
- **THEN** the result remains a preview/action surface until the user explicitly chooses a follow-up action such as save or send
