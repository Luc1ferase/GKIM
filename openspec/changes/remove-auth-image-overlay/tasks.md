## 1. Welcome Surface Composition

- [x] 1.1 Remove the static screenshot/mockup image from the Android welcome/auth runtime composition, rebuild the screen from native layout layers, and add UI coverage proving unauthenticated startup still exposes the welcome entry actions cleanly.
- [ ] 1.2 Clean up the remaining onboarding asset/runtime references so the static reference image is no longer part of the shipped auth entry surface, then verify the adjusted welcome composition still preserves the intended hierarchy and media behavior.

## Task Evidence

### Task 1.1: Remove the static screenshot/mockup image from the Android welcome/auth runtime composition, rebuild the screen from native layout layers, and add UI coverage proving unauthenticated startup still exposes the welcome entry actions cleanly.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#launchShowsWelcomeOnboardingBeforeMainShell,com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeLoginActionEntersAuthenticatedShellPreview" --rerun-tasks` - pass, confirming unauthenticated launch now exposes the welcome screen through native structure layers plus working login/register entry actions without relying on the packaged screenshot composition
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the rebuilt welcome route compiles cleanly after removing the runtime screenshot layer
  - `rg -n "R\.drawable\.welcome_screen|painterResource\(R\.drawable\.welcome_screen\)|welcome_screen" android/app/src/main/java android/app/src/androidTest -g "*.kt"` - pass with no matches, confirming runtime Kotlin code no longer references the packaged welcome screenshot
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/navigation/WelcomeRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `2347030`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
