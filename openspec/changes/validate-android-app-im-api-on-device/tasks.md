## 1. Validation scaffolding and operator-facing configuration

- [x] 1.1 Add or update unit/integration coverage for IM session, bootstrap, history, and gateway-event mapping so backend-backed Android messaging state can be verified independently of device runs.
- [x] 1.2 Extend the Android preference and Settings/debug surfaces with IM backend validation inputs for HTTP endpoint, WebSocket endpoint, development user selection, and explicit validation failure state.

## 2. Live IM backend integration in the Android app

- [x] 2.1 Implement Android HTTP client contracts and mappers for backend development session issuance, bootstrap hydration, and paginated message history retrieval.
- [x] 2.2 Upgrade `RealtimeChatClient` into an authenticated backend event adapter that can connect, reconnect, send messages, mark reads, and surface delivery/read/error events to repository consumers.
- [x] 2.3 Replace the seed-only Android IM repository wiring with a backend-backed messaging state holder that hydrates Messages/Chat from live APIs and reconciles WebSocket events without breaking existing UI contracts.

## 3. Physical-device validation workflow

- [ ] 3.1 Add the repeatable device-validation workflow for the current deployment path, including SSH tunnel + `adb reverse` guidance, backend reachability checks, and adb/logcat evidence capture steps for a physical Android device.
- [ ] 3.2 Run the full physical-device IM API validation flow against the deployed backend for session, bootstrap, history, send, realtime receive, delivery/read, and reconnect recovery, then capture the required evidence in `docs/DELIVERY_WORKFLOW.md`.

## Task Evidence

### Task 1.1: Add or update unit/integration coverage for IM session, bootstrap, history, and gateway-event mapping so backend-backed Android messaging state can be verified independently of device runs.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest` - failed first with unresolved backend DTO/parser symbols, then passed after adding IM payload DTOs, bootstrap/history mappers, gateway-event parsing, and receipt fields on `ChatMessage`
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/core/model/ChatModels.kt android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendModels.kt android/app/src/test/java/com/gkim/im/android/data/remote/im/ImBackendPayloadsTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `3c8a034`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Extend the Android preference and Settings/debug surfaces with IM backend validation inputs for HTTP endpoint, WebSocket endpoint, development user selection, and explicit validation failure state.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.SettingsViewModelTest` - failed first with missing IM validation config state and preference methods, then passed after extending `PreferencesStore` plus `SettingsViewModel`/`SettingsRoute` with IM backend validation inputs and derived status
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsScreenExposesImBackendValidationControlsAndStatus" --rerun-tasks` - failed first on the IM validation status assertion, then passed after tightening the test around real visible status text and field-clearing behavior
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/data/local/AppPreferencesStore.kt android/app/src/main/java/com/gkim/im/android/feature/settings/SettingsRoute.kt android/app/src/test/java/com/gkim/im/android/testing/TestFakes.kt android/app/src/test/java/com/gkim/im/android/feature/settings/SettingsViewModelTest.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/UiTestFakes.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `8bad14f`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Implement Android HTTP client contracts and mappers for backend development session issuance, bootstrap hydration, and paginated message history retrieval.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest --tests com.gkim.im.android.data.remote.realtime.RealtimeChatClientTest --tests com.gkim.im.android.data.repository.LiveMessagingRepositoryTest --tests com.gkim.im.android.feature.messages.MessagesViewModelTest --rerun-tasks` - pass, reconfirming the IM backend HTTP client, repository mappers, and current app-shell consumers work together on the active branch
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendClient.kt android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendHttpClient.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `30a0e57`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.2: Upgrade `RealtimeChatClient` into an authenticated backend event adapter that can connect, reconnect, send messages, mark reads, and surface delivery/read/error events to repository consumers.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest --tests com.gkim.im.android.data.remote.realtime.RealtimeChatClientTest --tests com.gkim.im.android.data.repository.LiveMessagingRepositoryTest --tests com.gkim.im.android.feature.messages.MessagesViewModelTest --rerun-tasks` - pass, reconfirming the authenticated realtime adapter, parsed gateway events, and repository consumers remain green on the active branch
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/data/remote/realtime/RealtimeGateway.kt android/app/src/main/java/com/gkim/im/android/data/remote/realtime/RealtimeChatClient.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `30a0e57`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.3: Replace the seed-only Android IM repository wiring with a backend-backed messaging state holder that hydrates Messages/Chat from live APIs and reconciles WebSocket events without breaking existing UI contracts.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest --tests com.gkim.im.android.data.remote.realtime.RealtimeChatClientTest --tests com.gkim.im.android.data.repository.LiveMessagingRepositoryTest --tests com.gkim.im.android.feature.messages.MessagesViewModelTest --rerun-tasks` - pass, confirming the active branch hydrates IM state through the live repository path and preserves current UI-facing state contracts
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#openingConversationRequestsLiveHistoryLoad" --rerun-tasks` - pass, confirming the app shell requests live conversation history when entering a backend-backed chat
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/data/repository/Repositories.kt android/app/src/main/java/com/gkim/im/android/data/repository/AppContainer.kt android/app/src/main/java/com/gkim/im/android/feature/messages/MessagesRoute.kt android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/test/java/com/gkim/im/android/data/repository/LiveMessagingRepositoryTest.kt android/app/src/test/java/com/gkim/im/android/feature/messages/MessagesViewModelTest.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `bfacbd0`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
