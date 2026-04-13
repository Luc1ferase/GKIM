## 1. Shell defaults and settings navigation

- [x] 1.1 Update Android preference defaults and root shell startup so first-run users start in Chinese and light theme, with regression coverage for persisted overrides.
- [x] 1.2 Replace the current long-form Settings page with a menu-style settings landing flow plus focused detail surfaces for appearance/language, AI provider, IM validation, and account actions.

## 2. Messages and Space information architecture

- [x] 2.1 Remove unread-count bubble badges from the message shell surfaces and update the affected Messages UI/state tests so incoming messages no longer render badge bubbles.
- [x] 2.2 Merge Workshop discovery into the Space filter row, normalize post and prompt/workshop cards into one waterfall-style feed model, and remove the separate Workshop navigation entry with updated UI coverage.

## 3. Welcome and Android auth shell

- [x] 3.1 Add a root auth state machine and welcome/onboarding route that uses the provided `docs/stitch-design/welcome_screen` design direction and media assets to expose `注册` and `登录` entry actions before the main shell.
- [x] 3.2 Introduce Android-side account session persistence, login/register form flows, and authenticated-shell gating tests so returning users bypass welcome while signed-out users cannot access the app shell.

## 4. Backend account and contact foundation

- [x] 4.1 Add Rust/PostgreSQL account persistence, registration/login APIs, and authenticated session/token issuance with focused backend verification.
- [x] 4.2 Add backend add-by-ID contact APIs plus account-backed IM bootstrap/session authorization so messaging data is scoped to authenticated accounts instead of development-only identities.

## 5. Account-backed app integration and acceptance

- [x] 5.1 Replace the Android development-user IM assumptions with account-backed auth/session wiring, including account-ID contact adding from the app shell and authenticated IM bootstrap usage.
- [x] 5.2 Run emulator validation for the authenticated welcome/login/add-by-ID/message flow, including the unread-badge removal and merged Space browsing surfaces, then capture the required evidence in `docs/DELIVERY_WORKFLOW.md`.

## Task Evidence

### Task 1.1: Update Android preference defaults and root shell startup so first-run users start in Chinese and light theme, with regression coverage for persisted overrides.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat cleanTestDebugUnitTest :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.SettingsViewModelTest --rerun-tasks` - pass, confirming first-run defaults now resolve to Chinese/light while persisted language/theme override behavior remains green in the view-model layer
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#rootShellDefaultsToChineseAndLightThemeOnFirstRun" --rerun-tasks` - pass, confirming the root shell boots with Chinese navigation labels and the light theme on first run
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsScreenAppliesLanguageAndThemePreferencesAcrossShell" --rerun-tasks` - pass, confirming persisted language/theme overrides still take effect after the default-value shift
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/data/local/AppPreferencesStore.kt android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt android/app/src/main/java/com/gkim/im/android/feature/settings/SettingsRoute.kt android/app/src/test/java/com/gkim/im/android/testing/TestFakes.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/UiTestFakes.kt android/app/src/test/java/com/gkim/im/android/feature/settings/SettingsViewModelTest.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `ef2bb68`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Replace the current long-form Settings page with a menu-style settings landing flow plus focused detail surfaces for appearance/language, AI provider, IM validation, and account actions.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsMenuPresentsFocusedEntriesAndAccountActionsSurface,com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsInteractionsUpdateProviderConfiguration,com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsScreenAppliesLanguageAndThemePreferencesAcrossShell,com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsScreenExposesImBackendValidationControlsAndStatus" --rerun-tasks` - pass, confirming the new menu-style Settings landing, focused detail surfaces, provider editing flow, appearance persistence flow, and IM validation detail all work on the emulator
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the refactored settings route compiles cleanly after the new internal settings state machine and detail surfaces were introduced
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.SettingsViewModelTest --rerun-tasks` - pass, confirming the underlying settings view-model state and persistence behavior remained green after the UI restructuring
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/settings/SettingsRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `7504ae7`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Remove unread-count bubble badges from the message shell surfaces and update the affected Messages UI/state tests so incoming messages no longer render badge bubbles.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesConversationRowsHideUnreadBubbleBadgesButKeepCoreMetadata,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesScreenStartsAtRecentConversationsWithoutUnreadSummaryPanel,com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenShowsUnreadSummaryAsSupportingContext" --rerun-tasks` - pass, confirming Messages rows drop unread bubble badges while keeping name/preview/time metadata intact and Space still preserves its top-level unread supporting summary
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the Messages route compiles cleanly after removing the unread bubble rendering branch
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/messages/MessagesRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `da41db0`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.2: Merge Workshop discovery into the Space filter row, normalize post and prompt/workshop cards into one waterfall-style feed model, and remove the separate Workshop navigation entry with updated UI coverage.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenShowsUnreadSummaryAsSupportingContext,com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenMergesWorkshopDiscoveryIntoUnifiedFeed,com.gkim.im.android.feature.navigation.GkimRootAppTest#spacePromptCardsApplyTemplatesIntoStudioChat,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatScreenUsesCompactHeaderAndBackNavigation" --rerun-tasks` - pass, confirming Space keeps its unread supporting summary, merges prompt discovery into the same feed/filter surface, applies prompt templates directly into `chat/studio`, and removes the independent Workshop shortcut from chat chrome
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the Space/chat/navigation refactor and Workshop route removal compile cleanly
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/space/SpaceRoute.kt android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt android/app/src/main/java/com/gkim/im/android/feature/workshop/WorkshopRoute.kt` - pass with line-ending warnings only
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `347b6e0`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.1: Add a root auth state machine and welcome/onboarding route that uses the provided `docs/stitch-design/welcome_screen` design direction and media assets to expose `注册` and `登录` entry actions before the main shell.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#launchShowsWelcomeOnboardingBeforeMainShell,com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeLoginActionEntersAuthenticatedShellPreview,com.gkim.im.android.feature.navigation.GkimRootAppTest#rootShellDefaultsToChineseAndLightThemeOnFirstRun,com.gkim.im.android.feature.navigation.GkimRootAppTest#bottomNavigationSwitchesAcrossPrimaryTabs" --rerun-tasks` - pass, confirming cold launch now shows the stitched welcome route with media-backed onboarding CTAs while the authenticated preview seam still enters the Chinese/light shell and preserves existing shell navigation tests
  - `$target = Resolve-Path 'x:\Repos\GKIM\android\app\build\kspCaches\debug\backups\java\byRounds' -ErrorAction SilentlyContinue; if ($target) { Remove-Item -LiteralPath $target -Recurse -Force }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the root auth gate, welcome route, and packaged media resources compile cleanly after clearing a stale local KSP backup cache collision
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt android/app/src/main/java/com/gkim/im/android/feature/navigation/WelcomeRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/LiveImBackendValidationTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `7a4e6a2`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.2: Introduce Android-side account session persistence, login/register form flows, and authenticated-shell gating tests so returning users bypass welcome while signed-out users cannot access the app shell.

- Reconciled completion:
  - Archived change `2026-04-10-real-im-auth-and-social` implemented Android session persistence plus real login/register routes in tasks `7.2`, `8.1`, `9.1`, `10.1`, and `10.2`.
  - Archived change `2026-04-13-validate-real-im-auth-app` revalidated the resulting flow in tasks `3.1` and `4.2`, including login/register submission, stored-session restore, and invalid-session fallback coverage on Android.
  - Archived change `2026-04-13-diagnose-onboarding-and-api-regressions` refreshed the inert auth-back-navigation regression coverage and endpoint-selection diagnostics in tasks `1.1`, `2.2`, `3.1`, and `3.2`.
- Result: `completed by later archived auth validation work`

### Task 4.1: Add Rust/PostgreSQL account persistence, registration/login APIs, and authenticated session/token issuance with focused backend verification.

- Reconciled completion:
  - Archived change `2026-04-10-real-im-auth-and-social` landed the backend account schema and auth endpoints through tasks `1.1`, `1.3`, `2.1`, and `2.2`.
  - Archived change `2026-04-13-validate-real-im-auth-app` re-ran focused backend verification in tasks `1.3` and `4.1`, covering register/login/search/friend-request/contact-gated messaging against the live PostgreSQL-backed backend.
- Result: `completed by later archived backend auth work`

### Task 4.2: Add backend add-by-ID contact APIs plus account-backed IM bootstrap/session authorization so messaging data is scoped to authenticated accounts instead of development-only identities.

- Reconciled completion:
  - The original literal `add-by-ID` wording was superseded by the later accepted authenticated user-discovery plus friend-request lifecycle, which became the durable main-spec contract.
  - Archived change `2026-04-10-real-im-auth-and-social` landed the account-backed IM bootstrap/session model plus social graph APIs through tasks `3.1`, `4.1`-`4.4`, `5.1`, and `6.1`.
  - Archived change `2026-04-13-validate-real-im-auth-app` task `4.3` synced that accepted auth/social behavior into the main `openspec/specs/im-backend/spec.md` and `openspec/specs/core/im-app/spec.md`.
- Result: `completed via the later accepted search-and-friend-request flow that replaced the earlier add-by-ID wording`

### Task 5.1: Replace the Android development-user IM assumptions with account-backed auth/session wiring, including account-ID contact adding from the app shell and authenticated IM bootstrap usage.

- Reconciled completion:
  - Archived change `2026-04-10-real-im-auth-and-social` replaced Android development-user assumptions with session-backed auth, authenticated bootstrap restore, and live social entry points across tasks `7.1`-`12.3`.
  - Archived change `2026-04-13-validate-real-im-auth-app` revalidated the resulting Android contract in tasks `3.1`, `3.2`, and `4.2`, including stored-session restore/fallback, authenticated user discovery, pending friend requests, and contact-gated messaging entry.
  - As with backend task `4.2`, the app-shell contact path evolved from exact account-ID entry to the accepted authenticated search/request flow before archive reconciliation.
- Result: `completed via the later accepted Android auth-and-social integration`

### Task 5.2: Run emulator validation for the authenticated welcome/login/add-by-ID/message flow, including the unread-badge removal and merged Space browsing surfaces, then capture the required evidence in `docs/DELIVERY_WORKFLOW.md`.

- Reconciled completion:
  - This change already captured accepted emulator evidence for unread-badge removal (`2.1`), merged Space browsing (`2.2`), and the welcome-shell entry flow (`3.1`).
  - Archived change `2026-04-13-validate-real-im-auth-app` captured the authenticated emulator/backend evidence chain in task `4.2`, covering login, registration, stored-session restore/fallback, contacts discovery, pending requests, and messaging entry.
  - Archived change `2026-04-13-diagnose-onboarding-and-api-regressions` added follow-up emulator/backend verification for auth target resolution, welcome playback, and login/register back navigation in tasks `4.1` and `4.2`.
  - The original `add-by-ID` wording was again superseded by the accepted user-search/friend-request flow used in the validated Android path.
- Result: `completed across the accepted evidence chain for shell, auth, and live backend validation`
