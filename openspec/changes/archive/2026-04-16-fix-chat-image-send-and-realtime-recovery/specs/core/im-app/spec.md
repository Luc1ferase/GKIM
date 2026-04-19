## ADDED Requirements

### Requirement: Chat detail keeps backend conversations synchronized after realtime interruption
The system SHALL automatically reconnect backend-backed chat sessions after realtime interruption, and it MUST refresh conversation summaries plus any already-loaded backend conversation histories so missed direct messages appear without requiring the user to exit and re-enter the app.

#### Scenario: Automatic reconnect resumes an authenticated IM session
- **WHEN** the backend WebSocket closes or fails while the Android app still holds a valid authenticated IM session
- **THEN** the app attempts reconnect automatically, keeps the validation/debug status updated while reconnect is in progress, and returns the messaging integration state to ready once `session.registered` succeeds

#### Scenario: Missed messages are recovered after reconnect
- **WHEN** one or more direct messages arrive while the Android client is disconnected from realtime and the user later reconnects
- **THEN** the app refreshes backend conversation summaries and force-reloads any previously loaded backend conversation histories so the missed messages appear in chat detail and recent-conversation previews without a manual app restart

## MODIFIED Requirements

### Requirement: Android app exercises live IM backend flows during emulator validation
The system SHALL let the Android app authenticate through the backend development session flow, hydrate conversations, message history, and image-message attachments from the live IM backend, and reconcile authenticated WebSocket events plus backend image-send responses into visible chat state during Android emulator validation runs against a locally containerized or remotely published backend. The Android app MUST reconnect and resynchronize current conversation state automatically after realtime interruption during emulator validation instead of requiring the tester to exit and re-enter the app to observe missed messages.

#### Scenario: Emulator validation bootstraps live conversation data
- **WHEN** a tester starts an IM validation session on the Android app in the emulator with a configured development user
- **THEN** the app issues the backend session/bootstrap requests and renders conversation state from live backend data instead of seed-only in-memory messaging rows

#### Scenario: Emulator validation drives realtime text and image send updates
- **WHEN** the tester sends a text message or an image message from the Android app in the emulator while the backend and counterpart session are online
- **THEN** the app uses the live backend send contracts, persists the resulting message, and updates the visible conversation from the live backend send, receive, delivered, and read flows instead of relying on local-only message append behavior

#### Scenario: Emulator validation recovers after reconnect or relaunch without manual chat restart
- **WHEN** the Android app loses its realtime connection, misses inbound messages during the gap, or is relaunched during an emulator validation session
- **THEN** it reconnects or reloads current conversation state from backend bootstrap/history APIs, resumes WebSocket synchronization, and renders the missed messages without requiring the tester to manually exit and re-enter the chat flow beyond the relaunch itself

#### Scenario: Emulator validation surfaces backend failures explicitly
- **WHEN** session issuance, bootstrap loading, history retrieval, image-message upload, attachment fetch, or realtime connection setup fails in the Android emulator
- **THEN** the app shows an explicit validation/debug failure state instead of silently falling back to placeholder success behavior

### Requirement: Chat detail exposes AIGC generation entry points
The system SHALL provide a standard chat composer with a text input field and send action as the primary control path, and it MUST expose distinct secondary actions for only the AIGC generation modes supported by the currently active provider, normal chat media attachments, and generation-source media selection from the `+` menu inside the chat experience. Image-to-image and video-to-video actions MUST only appear or enable as ready-to-run actions when both the active provider supports the mode and the required local source media for that mode has been selected explicitly for generation. Unsupported AIGC modes MUST NOT appear as ready-to-run actions for the active provider. For backend-backed user conversations, a normal chat image attachment MUST use the authenticated direct-message attachment path when the user sends it instead of stopping at a local-only placeholder row.

#### Scenario: User sends plain text from the primary composer
- **WHEN** the user is in chat detail and enters text without opening secondary actions
- **THEN** the screen provides a text input field with a send control on the right side as the primary messaging action

#### Scenario: User opens the secondary action menu
- **WHEN** the user taps the `+` affordance in the chat composer
- **THEN** the app reveals a secondary action menu that separates normal chat media attachment actions from the AIGC generation actions supported by the active provider instead of relying on one ambiguous `Pick image` path

#### Scenario: User picks an image to send as a normal chat message
- **WHEN** the user chooses the chat image attachment action from the secondary menu and selects a local photo
- **THEN** the app stages that photo as a normal outgoing chat attachment and, once the user sends it in a backend-backed conversation, submits it through the authenticated backend image-message path so counterpart sessions and history receive the same durable image

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

#### Scenario: Unsupported AIGC modes are not presented as ready actions
- **WHEN** the active provider does not support one or more AIGC modes
- **THEN** the chat `+` menu hides or otherwise clearly blocks those unsupported generation actions before any network request starts
