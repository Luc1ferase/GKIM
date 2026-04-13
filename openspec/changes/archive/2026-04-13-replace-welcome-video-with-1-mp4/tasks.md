## 1. Welcome Video Coverage

- [x] 1.1 Update Android welcome/onboarding coverage so it verifies the new packaged runtime video resource is present, the superseded welcome video asset is no longer the shipped backdrop resource, and unauthenticated startup still exposes the native-composed welcome surface with login/register entry actions.

## 2. Welcome Video Asset Swap

- [x] 2.1 Package `docs/stitch-design/welcome_screen/1.mp4` into Android raw resources under an Android-safe name, update `WelcomeRoute` to bind the welcome `VideoView` to that resource, and remove the superseded runtime video asset if it is no longer referenced.

## Task Evidence

### Task 1.1: Update Android welcome/onboarding coverage so it verifies the new packaged runtime video resource is present, the superseded welcome video asset is no longer the shipped backdrop resource, and unauthenticated startup still exposes the native-composed welcome surface with login/register entry actions.

- Verification:
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force -ErrorAction SilentlyContinue }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force -ErrorAction SilentlyContinue }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeUsesApprovedReplacementVideoResource,com.gkim.im.android.feature.navigation.GkimRootAppTest#launchShowsWelcomeOnboardingBeforeMainShell,com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeLoginActionEntersAuthenticatedShellPreview" --rerun-tasks` - pass, confirming the new runtime video resource is packaged, the old welcome backdrop resource is gone, and unauthenticated startup still renders the native welcome surface with login/register entry actions.
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force -ErrorAction SilentlyContinue }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force -ErrorAction SilentlyContinue }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the welcome-screen resource binding and updated packaged-asset assertions compile cleanly after the swap.
  - `if (Test-Path 'x:/Repos/GKIM/android/app/src/main/res/raw/welcome_intro_1.mp4') { 'new-present' } else { 'new-missing' }; if (Test-Path 'x:/Repos/GKIM/android/app/src/main/res/raw/welcome_atrium.mp4') { 'old-present' } else { 'old-missing' }` - pass (`new-present`, `old-missing`), confirming the new raw asset exists and the superseded one has been removed from the packaged runtime resources.
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/navigation/WelcomeRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt android/app/src/main/res/raw/welcome_atrium.mp4 android/app/src/main/res/raw/welcome_intro_1.mp4` - pass with line-ending warnings only.
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `0968060`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Package `docs/stitch-design/welcome_screen/1.mp4` into Android raw resources under an Android-safe name, update `WelcomeRoute` to bind the welcome `VideoView` to that resource, and remove the superseded runtime video asset if it is no longer referenced.

- Verification:
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force -ErrorAction SilentlyContinue }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force -ErrorAction SilentlyContinue }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeUsesApprovedReplacementVideoResource,com.gkim.im.android.feature.navigation.GkimRootAppTest#launchShowsWelcomeOnboardingBeforeMainShell,com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeLoginActionEntersAuthenticatedShellPreview" --rerun-tasks` - pass, confirming the welcome route now plays the replacement packaged runtime video while preserving the existing native-composed welcome surface and entry actions.
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force -ErrorAction SilentlyContinue }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force -ErrorAction SilentlyContinue }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the `WelcomeRoute` raw-resource binding compiles cleanly against the new Android-safe video asset name.
  - `if (Test-Path 'x:/Repos/GKIM/android/app/src/main/res/raw/welcome_intro_1.mp4') { 'new-present' } else { 'new-missing' }; if (Test-Path 'x:/Repos/GKIM/android/app/src/main/res/raw/welcome_atrium.mp4') { 'old-present' } else { 'old-missing' }` - pass (`new-present`, `old-missing`), confirming the asset swap actually landed on disk.
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/navigation/WelcomeRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt android/app/src/main/res/raw/welcome_atrium.mp4 android/app/src/main/res/raw/welcome_intro_1.mp4` - pass with line-ending warnings only.
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `0968060`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
