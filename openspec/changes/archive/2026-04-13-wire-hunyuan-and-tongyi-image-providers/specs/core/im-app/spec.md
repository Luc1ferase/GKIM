## MODIFIED Requirements

### Requirement: Chat detail exposes AIGC generation entry points
The system SHALL provide a standard chat composer with a text input field and send action as the primary control path, and it MUST expose only the AIGC actions supported by the currently active provider plus local media pickers from a secondary `+` action menu inside the chat experience. Unsupported AIGC modes MUST NOT appear as ready-to-run actions for the active provider.

#### Scenario: User sends plain text from the primary composer
- **WHEN** the user is in chat detail and enters text without opening secondary actions
- **THEN** the screen provides a text input field with a send control on the right side as the primary messaging action

#### Scenario: User opens the secondary action menu
- **WHEN** the user taps the `+` affordance in the chat composer
- **THEN** the app reveals a secondary action menu that contains the AIGC actions supported by the active provider plus local media pickers instead of showing those controls inline by default

#### Scenario: User starts text-to-image generation in chat
- **WHEN** the user selects the text-to-image AIGC action from the secondary composer menu and the active provider supports that mode
- **THEN** the app opens the generation flow with a prompt input experience tied to the shared AIGC composable

#### Scenario: User starts image-to-image generation with local media
- **WHEN** the user selects image-to-image from the secondary composer menu, the active provider supports that mode, and the user chooses a local photo
- **THEN** the app passes the chosen media and prompt data through the shared AIGC generation flow

#### Scenario: User starts video-to-video generation with local media
- **WHEN** the user selects video-to-video from the secondary composer menu, the active provider supports that mode, and the user chooses a local video
- **THEN** the app passes the chosen media and prompt data through the shared AIGC generation flow

#### Scenario: Unsupported AIGC modes are not presented as ready actions
- **WHEN** the active provider does not support one or more AIGC modes
- **THEN** the chat `+` menu hides or otherwise clearly blocks those unsupported generation actions before any network request starts

### Requirement: AI settings support preset and custom infrastructure providers
The system SHALL provide a Settings page that includes preset providers for Tencent Hunyuan and Alibaba Tongyi, and it MUST allow users to configure preset-provider API keys locally through secure device storage while also supporting a custom OpenAI-compatible endpoint, API key, and model identifier plus app-level language and appearance preferences. The Tencent Hunyuan preset MUST default to model `hy-image-v3.0`, and the Alibaba Tongyi preset MUST default to model `wan2.7-image`. The page MUST support Chinese and English selection plus explicit light and dark theme modes, and those preferences MUST persist across app restarts. Preset and custom provider secrets MUST NOT be sourced from tracked repository defaults.

#### Scenario: User activates Tencent Hunyuan with the requested preset model
- **WHEN** the user selects Tencent Hunyuan in Settings
- **THEN** the provider configuration store marks `hunyuan` as active for subsequent AIGC requests and uses `hy-image-v3.0` as the preset model unless the user explicitly overrides it locally

#### Scenario: User activates Alibaba Tongyi with the requested preset model
- **WHEN** the user selects Alibaba Tongyi in Settings
- **THEN** the provider configuration store marks `tongyi` as active for subsequent AIGC requests and uses `wan2.7-image` as the preset model unless the user explicitly overrides it locally

#### Scenario: User stores preset-provider API keys locally
- **WHEN** the user enters a Tencent Hunyuan or Alibaba Tongyi API key in Settings
- **THEN** the app persists that key through secure local storage and restores it for later local generation sessions without reading it from tracked source defaults

#### Scenario: User configures a custom provider
- **WHEN** the user enters a custom API base URL, API key, and model in Settings
- **THEN** the provider configuration store persists the custom configuration for the shared AIGC adapter layer

#### Scenario: User switches app language
- **WHEN** the user selects Chinese or English in Settings
- **THEN** the app persists that language preference and updates affected UI copy to the selected language on the supported surfaces

#### Scenario: User switches app theme
- **WHEN** the user selects light mode or dark mode in Settings
- **THEN** the app persists that theme preference and re-renders the supported surfaces using the selected appearance mode

## ADDED Requirements

### Requirement: Preset image providers execute real generation requests with truthful task state
The system SHALL execute image-generation requests against the active preset provider’s real HTTP API when the user has configured a local API key, and it MUST render truthful task status, errors, and returned image output instead of synthesizing stock preview success. Successful generation MUST record the selected provider and resolved model in the resulting task metadata.

#### Scenario: Tencent Hunyuan generation returns a real image result
- **WHEN** the active provider is Tencent Hunyuan, a local API key is configured, and the user runs a supported image-generation request
- **THEN** the app sends a real provider request using model `hy-image-v3.0` (or the locally overridden preset model), receives the provider response, and renders the returned image output in the existing AIGC result surfaces

#### Scenario: Alibaba Tongyi generation returns a real image result
- **WHEN** the active provider is Alibaba Tongyi, a local API key is configured, and the user runs a supported image-generation request
- **THEN** the app sends a real provider request using model `wan2.7-image` (or the locally overridden preset model), receives the provider response, and renders the returned image output in the existing AIGC result surfaces

#### Scenario: Missing preset-provider key blocks fake success
- **WHEN** the user attempts generation from a preset provider without configuring its required local API key
- **THEN** the app reports a configuration error and does not create a fake succeeded task with a stock preview URL

#### Scenario: Provider failure remains visible and truthful
- **WHEN** the active provider returns an HTTP, auth, quota, or payload-validation failure during generation
- **THEN** the app surfaces a failed generation state or error message that reflects the provider failure and does not insert a placeholder success image

#### Scenario: Task metadata reflects the provider actually used
- **WHEN** a generation request succeeds
- **THEN** the resulting AIGC task records the active provider identity and resolved model so later UI and validation evidence can identify which provider produced the image
