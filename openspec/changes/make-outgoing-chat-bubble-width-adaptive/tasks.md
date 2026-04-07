## 1. Outgoing bubble width coverage

- [x] 1.1 Add or update targeted Compose UI coverage so short outgoing text-only messages assert a materially narrower bubble width than the full available row while keeping the existing timestamp text visible.
- [x] 1.2 Extend chat timeline coverage so longer outgoing text and attachment-bearing outgoing rows assert readable wrapping and stable footer placement after width adaptation.

### Task 1.1: Short outgoing bubble width coverage

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#shortOutgoingTextBubbleHugsContentWidthInsteadOfRowWidth"` - failed first with `bubbleWidth=860.0 availableWidth=954.0`, then passed after the outgoing text-only width change
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest"` - pass, 15/15 instrumentation tests green
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `776e2bf`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Long outgoing and attachment stability coverage

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#longOutgoingAndAttachmentRowsKeepReadableWidthAndStableFooter"` - pass
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest"` - pass, confirmed width adaptation did not collapse long outgoing or attachment-bearing rows
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `776e2bf`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

## 2. Outgoing bubble layout refinement

- [x] 2.1 Refactor the outgoing text-only bubble layout so short self-authored messages size closer to content width instead of stretching unnecessarily wide.
- [x] 2.2 Decouple outgoing footer alignment from bubble expansion so the lower-right timestamp placement stays intact without re-inflating short bubbles, while attachment-bearing outgoing rows retain stable media-friendly width behavior.

### Task 2.1: Adaptive width for outgoing text-only bubbles

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#shortOutgoingTextBubbleHugsContentWidthInsteadOfRowWidth"` - pass
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest"` - pass
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `776e2bf`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.2: Footer alignment without short-bubble re-inflation

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelinePinsOutgoingTimestampToBubbleFooterWithoutChangingFormat"` - pass
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#longOutgoingAndAttachmentRowsKeepReadableWidthAndStableFooter"` - pass
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `776e2bf`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

## 3. Regression and delivery evidence

- [x] 3.1 Run the relevant chat instrumentation and unit checks, update any geometry assertions affected by the adaptive outgoing width behavior, and capture the verification/review/upload evidence required by `docs/DELIVERY_WORKFLOW.md`.

### Task 3.1: Regression and delivery evidence

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest"` - pass, 15/15 instrumentation tests green
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat testDebugUnitTest` - pass
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt openspec/changes/make-outgoing-chat-bubble-width-adaptive/tasks.md` - pass
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `776e2bf`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
