## 1. Transport contracts and runtime configuration

- [x] 1.1 Add or update unit/contract coverage for IM session, bootstrap, history, and gateway-event parsing so Android transport code can be verified independently of repository wiring.
- [x] 1.2 Add or update Android settings/runtime-configuration coverage for persisted IM HTTP endpoint, WebSocket endpoint, development user selection, and visible integration-status validation.

## 2. HTTP and WebSocket transport wiring

- [x] 2.1 Implement the Android IM HTTP client layer for development session issuance, bootstrap hydration, and paginated message history retrieval using configurable backend endpoints.
- [x] 2.2 Upgrade `RealtimeChatClient` into an authenticated IM gateway adapter that parses typed backend events, exposes connection/error state, and supports send/read commands against the live WebSocket contract.

## 3. Repository and UI handoff

- [x] 3.1 Implement a backend-backed live messaging repository that authenticates, bootstraps conversation summaries, loads selected conversation history, and exposes explicit integration state without removing the seed repository fallback seam.
- [x] 3.2 Reconcile live WebSocket message, delivery, read, and failure events into the existing Messages and Chat UI models so visible conversation state is backend-driven instead of locally appended.
- [x] 3.3 Switch `AppContainer`, Messages, Chat, and any required view-model integration points onto the live messaging repository path while keeping current non-IM surfaces unchanged.

## 4. Regression and handoff

- [ ] 4.1 Add the minimal operator/developer handoff notes for configuring live IM endpoints and known scope limits before full device validation, then run the layered regression commands required by `docs/DELIVERY_WORKFLOW.md` and capture the evidence for this wiring change.

## Task Evidence

### Task 1.1: Add or update unit/contract coverage for IM session, bootstrap, history, and gateway-event parsing so Android transport code can be verified independently of repository wiring.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest` - pass on the current baseline, reconfirming backend payload DTOs, bootstrap/history mapping, gateway-event parsing, and message receipt fields remain green
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `3c8a034`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Add or update Android settings/runtime-configuration coverage for persisted IM HTTP endpoint, WebSocket endpoint, development user selection, and visible integration-status validation.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.SettingsViewModelTest` - pass on the current baseline, reconfirming IM HTTP/WS/dev-user settings and validation status remain covered
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsScreenExposesImBackendValidationControlsAndStatus" --rerun-tasks` - pass on the current baseline, confirming the settings screen persists IM runtime config and shows integration-status text for ready vs incomplete cases
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `8bad14f`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Implement the Android IM HTTP client layer for development session issuance, bootstrap hydration, and paginated message history retrieval using configurable backend endpoints.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest` - failed first with missing HTTP client symbols, then passed after adding the IM backend Retrofit wrapper and MockWebServer contract coverage for session, bootstrap, history, auth header, and query forwarding
  - `git diff --check -- android/app/build.gradle.kts android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendHttpClient.kt android/app/src/test/java/com/gkim/im/android/data/remote/im/ImBackendHttpClientTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `59e5513`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.2: Upgrade `RealtimeChatClient` into an authenticated IM gateway adapter that parses typed backend events, exposes connection/error state, and supports send/read commands against the live WebSocket contract.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.realtime.RealtimeChatClientTest` - failed first with missing typed event flow, bearer-token connect path, send/read helpers, and error state, then passed after upgrading `RealtimeChatClient` into an authenticated IM gateway adapter with typed command helpers and parsed gateway events
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/data/remote/realtime/RealtimeChatClient.kt android/app/src/test/java/com/gkim/im/android/data/remote/realtime/RealtimeChatClientTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `35d5972`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.1: Implement a backend-backed live messaging repository that authenticates, bootstraps conversation summaries, loads selected conversation history, and exposes explicit integration state without removing the seed repository fallback seam.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.LiveMessagingRepositoryTest` - failed first with missing bootstrap/history error-state handling, then passed after adding `LiveMessagingRepository`, repository-owned integration phases, backend bootstrap/history hydration, and explicit error transitions for invalid config plus transport failures
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/data/repository/Repositories.kt android/app/src/test/java/com/gkim/im/android/data/repository/LiveMessagingRepositoryTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `3117f06`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.2: Reconcile live WebSocket message, delivery, read, and failure events into the existing Messages and Chat UI models so visible conversation state is backend-driven instead of locally appended.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest --tests com.gkim.im.android.data.remote.realtime.RealtimeChatClientTest --tests com.gkim.im.android.data.repository.LiveMessagingRepositoryTest` - pass, confirming the repository now merges live HTTP bootstrap/history with realtime sent/received/delivered/read/error flows without regressing transport coverage
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/data/repository/Repositories.kt android/app/src/test/java/com/gkim/im/android/data/repository/LiveMessagingRepositoryTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `3117f06`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.3: Switch `AppContainer`, Messages, Chat, and any required view-model integration points onto the live messaging repository path while keeping current non-IM surfaces unchanged.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.LiveMessagingRepositoryTest --tests com.gkim.im.android.feature.messages.MessagesViewModelTest --rerun-tasks` - failed first on missing Messages integration-state wiring and on fallback-only rooms incorrectly attempting backend history, then passed after switching the app shell to `LiveMessagingRepository`, surfacing live IM status in Messages, triggering chat history hydration on open, and limiting history fetches to bootstrap-backed conversations
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#openingConversationRequestsLiveHistoryLoad" --rerun-tasks` - failed first because opening a chat did not request repository history, then passed after `ChatViewModel` began hydrating the selected conversation on entry
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/data/repository/Repositories.kt android/app/src/test/java/com/gkim/im/android/data/repository/LiveMessagingRepositoryTest.kt android/app/src/main/java/com/gkim/im/android/data/repository/AppContainer.kt android/app/src/main/java/com/gkim/im/android/feature/messages/MessagesRoute.kt android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/test/java/com/gkim/im/android/feature/messages/MessagesViewModelTest.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `bfacbd0`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
