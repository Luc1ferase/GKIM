## 1. Shell defaults and settings navigation

- [x] 1.1 Update Android preference defaults and root shell startup so first-run users start in Chinese and light theme, with regression coverage for persisted overrides.
- [x] 1.2 Replace the current long-form Settings page with a menu-style settings landing flow plus focused detail surfaces for appearance/language, AI provider, IM validation, and account actions.

## 2. Messages and Space information architecture

- [ ] 2.1 Remove unread-count bubble badges from the message shell surfaces and update the affected Messages UI/state tests so incoming messages no longer render badge bubbles.
- [ ] 2.2 Merge Workshop discovery into the Space filter row, normalize post and prompt/workshop cards into one waterfall-style feed model, and remove the separate Workshop navigation entry with updated UI coverage.

## 3. Welcome and Android auth shell

- [ ] 3.1 Add a root auth state machine and welcome/onboarding route that uses the provided `docs/stitch-design/welcome_screen` design direction and media assets to expose `注册` and `登录` entry actions before the main shell.
- [ ] 3.2 Introduce Android-side account session persistence, login/register form flows, and authenticated-shell gating tests so returning users bypass welcome while signed-out users cannot access the app shell.

## 4. Backend account and contact foundation

- [ ] 4.1 Add Rust/PostgreSQL account persistence, registration/login APIs, and authenticated session/token issuance with focused backend verification.
- [ ] 4.2 Add backend add-by-ID contact APIs plus account-backed IM bootstrap/session authorization so messaging data is scoped to authenticated accounts instead of development-only identities.

## 5. Account-backed app integration and acceptance

- [ ] 5.1 Replace the Android development-user IM assumptions with account-backed auth/session wiring, including account-ID contact adding from the app shell and authenticated IM bootstrap usage.
- [ ] 5.2 Run emulator validation for the authenticated welcome/login/add-by-ID/message flow, including the unread-badge removal and merged Space browsing surfaces, then capture the required evidence in `docs/DELIVERY_WORKFLOW.md`.

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
