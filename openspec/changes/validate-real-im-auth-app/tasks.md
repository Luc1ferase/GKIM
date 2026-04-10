## 1. Backend realtime validation blockers

- [x] 1.1 Reproduce the failing `backend/tests/ws_gateway.rs` auth/social scenarios against the current schema and isolate whether the timeout comes from contact-gating setup, event fan-out, or unread-state reconstruction
- [x] 1.2 Repair backend realtime delivery/read/offline bootstrap behavior so `cargo test --test http_im_api --test ws_gateway -- --nocapture` passes for both the online-send and offline-recovery scenarios
- [x] 1.3 Add or refresh backend integration coverage for register, login, user search, friend-request lifecycle, and contact-gated messaging so the auth-enabled IM path is verified beyond the websocket smoke cases

## 2. Android automated test harness repair

- [x] 2.1 Update stale unit-test doubles and fixtures to satisfy the expanded auth/social `ImBackendClient` contract used by repository and view-model tests
- [x] 2.2 Repair Android unit tests broken by auth/session model changes, including the current `LiveMessagingRepositoryTest`, `MessagesViewModelTest`, and `SettingsViewModelTest` compile failures
- [x] 2.3 Repair Android instrumentation test infrastructure so `UiTestAppContainer` and related navigation/auth tests compile and run with the current `AppContainer` contract

## 3. Android auth and contacts acceptance coverage

- [x] 3.1 Add or refresh Android coverage for login and registration submission, inline backend error handling, and persisted-session restore or fallback on app launch
- [x] 3.2 Add or refresh Android coverage for authenticated user discovery, pending friend-request actions, and the contact-gated entry path into messaging surfaces

## 4. Acceptance evidence and spec reconciliation

- [x] 4.1 Re-run backend validation commands, review the result against the OpenSpec requirements and `docs/QUALITY_SCORE.md`, and record backend evidence using the `docs/DELIVERY_WORKFLOW.md` template before marking backend tasks complete
- [x] 4.2 Re-run Android compile, unit-test, and instrumentation verification, review the result against the OpenSpec requirements and `docs/QUALITY_SCORE.md`, and record Android evidence using the `docs/DELIVERY_WORKFLOW.md` template before marking Android tasks complete
- [x] 4.3 Sync the validated auth-enabled behavior into the main `openspec/specs/im-backend/spec.md` and `openspec/specs/core/im-app/spec.md`, then reconcile whether the older `real-im-auth-and-social` change should be archived as superseded

## Task Evidence

### Task 4.1: Re-run backend validation commands, review the result against the OpenSpec requirements and `docs/QUALITY_SCORE.md`, and record backend evidence using the `docs/DELIVERY_WORKFLOW.md` template before marking backend tasks complete

- Verification:
  - `cargo fmt --check` - pass
  - `$pairs = @{}; Get-Content .env.local | ForEach-Object { if ($_ -match '^\s*#' -or $_.Trim() -eq '') { return }; $parts = $_ -split '=',2; $pairs[$parts[0].Trim()] = $parts[1].Trim().Trim('"') }; foreach ($key in 'PGHOST','PGPORT','PGUSER','PGPASSWORD','PGDATABASE') { if (-not $pairs.ContainsKey($key)) { throw "Missing $key in backend/.env.local" } }; $user = [uri]::EscapeDataString($pairs['PGUSER']); $pass = [uri]::EscapeDataString($pairs['PGPASSWORD']); $db = [uri]::EscapeDataString($pairs['PGDATABASE']); $url = "postgres://$user`:$pass@$($pairs['PGHOST']):$($pairs['PGPORT'])/$db"; if ($pairs.ContainsKey('PGSSLMODE')) { $url += "?sslmode=$([uri]::EscapeDataString($pairs['PGSSLMODE']))" }; $env:GKIM_TEST_DATABASE_URL = $url; cargo test --test migrations_schema --test http_im_api --test im_service_pg --test ws_gateway -- --nocapture` - pass, covering schema validation, auth/social HTTP flows, PostgreSQL bootstrap/history/contact gating, and websocket send/offline recovery paths against a real database-backed integration run
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `03eec84`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 4.2: Re-run Android compile, unit-test, and instrumentation verification, review the result against the OpenSpec requirements and `docs/QUALITY_SCORE.md`, and record Android evidence using the `docs/DELIVERY_WORKFLOW.md` template before marking Android tasks complete

- Verification:
  - `$env:ANDROID_HOME='D:\Android\Sdk'; $env:ANDROID_SDK_ROOT='D:\Android\Sdk'; & "$env:ANDROID_HOME\platform-tools\adb.exe" devices -l; & "$env:ANDROID_HOME\emulator\emulator.exe" -list-avds` - pass, confirming the connected `emulator-5554` device and the `codex_api34` AVD used for instrumentation coverage
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:ANDROID_HOME='D:\Android\Sdk'; $env:ANDROID_SDK_ROOT='D:\Android\Sdk'; .\gradlew.bat :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#registerRouteSubmitsCredentialsPersistsSessionAndEntersShell,com.gkim.im.android.feature.navigation.GkimRootAppTest#loginRouteShowsInlineErrorWhenBackendRejectsCredentials,com.gkim.im.android.feature.navigation.GkimRootAppTest#storedSessionRestoresAuthenticatedShellAfterBootstrapValidation,com.gkim.im.android.feature.navigation.GkimRootAppTest#invalidStoredSessionFallsBackToWelcomeAndClearsSession,com.gkim.im.android.feature.navigation.GkimRootAppTest#contactsScreenLoadsPendingRequestsAndAcceptsThem,com.gkim.im.android.feature.navigation.GkimRootAppTest#userSearchFlowShowsResultsAndMarksPendingAfterAdd'` - pass, covering Android test compilation, full debug unit tests, and the six authenticated welcome/session/contacts instrumentation scenarios on the emulator
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt android/app/src/main/java/com/gkim/im/android/feature/auth/LoginRoute.kt android/app/src/main/java/com/gkim/im/android/feature/auth/RegisterRoute.kt android/app/src/main/java/com/gkim/im/android/feature/social/UserSearchRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt openspec/specs/core/im-app/spec.md openspec/changes/validate-real-im-auth-app/tasks.md` - pass
- Review:
  - Score: `98/100`
  - Findings: `No findings`
- Upload:
  - Commit: `03eec84`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 4.3: Sync the validated auth-enabled behavior into the main `openspec/specs/im-backend/spec.md` and `openspec/specs/core/im-app/spec.md`, then reconcile whether the older `real-im-auth-and-social` change should be archived as superseded

- Verification:
  - `git diff --check -- openspec/specs/im-backend/spec.md openspec/specs/core/im-app/spec.md openspec/changes/validate-real-im-auth-app/specs/im-backend/spec.md openspec/changes/validate-real-im-auth-app/specs/core/im-app/spec.md` - pass
  - `openspec list --json` - pass, confirming the main auth-validation slice lives in `validate-real-im-auth-app` and there is no remaining active `real-im-auth-and-social` change to archive
  - `Get-ChildItem openspec/changes/archive -Directory | Select-Object -ExpandProperty FullName` - pass, confirming no archived directory named `real-im-auth-and-social` exists either, so no additional superseded-change archive step is required
- Review:
  - Score: `97/100`
  - Findings: `Main specs are synced and the older real-im-auth-and-social artifact is already absent from both active and archived change sets, so reconciliation is complete with no further archive action required.`
- Upload:
  - Commit: `03eec84`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
