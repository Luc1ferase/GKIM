## 1. Space Chrome

- [x] 1.1 Restore `AI 工具` and `动态` in the `Space` filter row, remove the top-level unread summary card, and update the affected `Space` UI coverage.
- [x] 1.2 Remove the `创作者动态` eyebrow and the current developer-explanatory description text from the `Space` header, then add regression checks that production-facing `Space` chrome no longer renders those helper strings.

## Task Evidence

### Task 1.1: Restore `AI 工具` and `动态` in the `Space` filter row, remove the top-level unread summary card, and update the affected `Space` UI coverage.

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenStartsAtDiscoveryFiltersWithoutUnreadSummaryChrome,com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenMergesWorkshopDiscoveryIntoUnifiedFeed,com.gkim.im.android.feature.navigation.GkimRootAppTest#spacePromptCardsApplyTemplatesIntoStudioChat" --rerun-tasks` - pass, confirming `Space` now shows the four discovery filters, no longer renders the unread summary card, keeps the unified feed visible, and still applies prompt cards into Studio chat.
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the `Space` route compiles cleanly after the unread-summary removal and four-filter restoration.
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/space/SpaceRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only.
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `2311158`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Remove the `创作者动态` eyebrow and the current developer-explanatory description text from the `Space` header, then add regression checks that production-facing `Space` chrome no longer renders those helper strings.

- Verification:
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenStartsAtDiscoveryFiltersWithoutUnreadSummaryChrome,com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenMergesWorkshopDiscoveryIntoUnifiedFeed,com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenUsesTitleOnlyProductionHeaderChrome,com.gkim.im.android.feature.navigation.GkimRootAppTest#spacePromptCardsApplyTemplatesIntoStudioChat" --rerun-tasks` - pass, confirming `Space` now uses production-facing title-only header chrome while preserving the restored filter rail, merged discovery feed, and prompt-apply flow.
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the optional `PageHeader` chrome and `Space` header changes compile cleanly after clearing the stale local KSP `byRounds` backup collision.
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/core/designsystem/AetherComponents.kt android/app/src/main/java/com/gkim/im/android/feature/space/SpaceRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only.
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `e456a68`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
