## 1. Regression Coverage

- [x] 1.1 Update Android UI coverage for the shared shell-heading rhythm across `Messages`, `Contacts`, and `Space`, including the Contacts top-band sort placement and the tighter first-row position.
- [ ] 1.2 Update Android UI coverage so `Messages` no longer renders the live IM status card and `Settings > IM Validation` shows the live IM status near the backend endpoint inputs.

## 2. Shell Layout and Status Relocation

- [x] 2.1 Implement the aligned top-band heading treatment across `Messages`, `Contacts`, and `Space`, and move the Contacts sort dropdown into the same row as the `Contacts / 联系人` title without changing sorting behavior.
- [ ] 2.2 Move the visible live IM validation status from `Messages` into `Settings > IM Validation`, reusing the existing messaging integration state instead of introducing a duplicate status source.

## Task Evidence

### Task 1.1: Update Android UI coverage for the shared shell-heading rhythm across `Messages`, `Contacts`, and `Space`, including the Contacts top-band sort placement and the tighter first-row position.

- Verification:
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#primaryTabsUseConsistentTopLevelHeadingScale,com.gkim.im.android.feature.navigation.GkimRootAppTest#contactsScreenPlacesSortDropdownInsideTitleBand" --rerun-tasks` - pass, confirming the new regression coverage catches the shared shell-heading rhythm and the Contacts inline sort-toolbar behavior.
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the new test helpers and header-layout touchpoints compile cleanly after the coverage update.
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/core/designsystem/AetherComponents.kt android/app/src/main/java/com/gkim/im/android/feature/contacts/ContactsRoute.kt android/app/src/main/java/com/gkim/im/android/feature/messages/MessagesRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only.
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `602a49b`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Implement the aligned top-band heading treatment across `Messages`, `Contacts`, and `Space`, and move the Contacts sort dropdown into the same row as the `Contacts / 联系人` title without changing sorting behavior.

- Verification:
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#primaryTabsUseConsistentTopLevelHeadingScale,com.gkim.im.android.feature.navigation.GkimRootAppTest#contactsScreenPlacesSortDropdownInsideTitleBand,com.gkim.im.android.feature.navigation.GkimRootAppTest#contactsScreenUsesSingleDropdownSortControl,com.gkim.im.android.feature.navigation.GkimRootAppTest#contactSortingChangesRenderedRowOrder,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesScreenStartsAtRecentConversationsWithoutUnreadSummaryPanel,com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenUsesTitleOnlyProductionHeaderChrome" --rerun-tasks` - pass, confirming the shared shell heading treatment now holds across Messages/Contacts/Space while the Contacts dropdown remains compact and sorting behavior stays intact.
  - `$generated = Resolve-Path 'x:/Repos/GKIM/android/app/build/generated/ksp/debug/java/byRounds' -ErrorAction SilentlyContinue; if ($generated) { Remove-Item -LiteralPath $generated -Recurse -Force }; $backup = Resolve-Path 'x:/Repos/GKIM/android/app/build/kspCaches/debug/backups/java/byRounds' -ErrorAction SilentlyContinue; if ($backup) { Remove-Item -LiteralPath $backup -Recurse -Force }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat :app:compileDebugKotlin` - pass, confirming the new shared shell-header component and the adjusted Messages/Contacts layouts compile cleanly.
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/core/designsystem/AetherComponents.kt android/app/src/main/java/com/gkim/im/android/feature/contacts/ContactsRoute.kt android/app/src/main/java/com/gkim/im/android/feature/messages/MessagesRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only.
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `602a49b`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
