## 1. Outgoing message density coverage

- [x] 1.1 Add or update targeted Compose UI coverage so outgoing self-authored messages assert the absence of self-avatar and `You` sender nodes while incoming and system messages keep their current attribution markers.
- [ ] 1.2 Extend chat timeline coverage to assert outgoing timestamps render inside the bubble footer near the lower-right edge without changing the existing timestamp text format.

### Task 1.1: Outgoing self-marker coverage

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelineKeepsIncomingAndSystemAttributionButDropsOutgoingSelfMarkers"` - failed first at `GkimRootAppTest.kt:180` before the layout change, then passed after implementation
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelineKeepsAttachmentAndTimestampForSystemMessages"` - pass, confirmed system attribution and attachment/timestamp rendering stayed intact
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `0816e95`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

## 2. Chat timeline layout refinement

- [x] 2.1 Refactor the chat message row composable so outgoing messages render as compact self-bubbles without self-avatar or `You` label while incoming and system rows preserve the current avatar-leading structure.
- [ ] 2.2 Move the outgoing timestamp into a tighter in-bubble footer position, then verify long text and attachment-bearing timeline rows still render cleanly.

### Task 2.1: Compact outgoing self-bubble layout

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelineKeepsIncomingAndSystemAttributionButDropsOutgoingSelfMarkers"` - pass
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelineKeepsAttachmentAndTimestampForSystemMessages"` - pass
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `0816e95`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

## 3. Regression and delivery evidence

- [ ] 3.1 Run the relevant chat instrumentation and unit checks, update any selector or geometry assertions affected by the denser outgoing layout, and capture the verification/review/upload evidence required by `docs/DELIVERY_WORKFLOW.md`.
