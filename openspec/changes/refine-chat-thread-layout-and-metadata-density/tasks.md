## 1. Composer anchoring and layout coverage

- [x] 1.1 Add or update targeted Compose UI coverage so the chat composer asserts as a persistent bottom region even when the timeline or auxiliary chat content grows.
- [x] 1.2 Add or update chat geometry coverage so incoming avatar, sender metadata, and right-aligned timestamp assert the denser header rhythm and reduced timestamp footprint.

## 2. Chat thread layout refinement

- [x] 2.1 Refactor the chat screen layout so the composer remains visually anchored to the bottom while secondary content no longer pushes it downward as messages accumulate.
- [x] 2.2 Refine incoming and outgoing metadata placement so incoming timestamps sit on the right, avatar/timestamp alignment is tighter, and timestamp spacing is reduced in a Telegram-inspired density model without copying Telegram verbatim.

## 3. Regression and delivery evidence

- [ ] 3.1 Run the relevant chat instrumentation and unit checks, update any geometry assertions affected by the denser composer and metadata layout, and capture the verification/review/upload evidence required by `docs/DELIVERY_WORKFLOW.md`.

### Task 1.1: Composer anchoring coverage

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatComposerStaysAnchoredToBottomWhenLatestGenerationAppears,com.gkim.im.android.feature.navigation.GkimRootAppTest#incomingMetadataUsesCompactHeaderWithRightAlignedTimestamp"` - failed first with `bottomGap=1005.0` and `topDelta=239.0`, then passed after the layout refactor and assertion tuning
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatComposerStaysAnchoredToBottomWhenLatestGenerationAppears,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatScreenUsesComposerRowForDefaultSendFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatSecondaryMenuShowsAigcAndMediaActions,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatSecondaryMenuActionsMapToExpectedChatBehavior" --rerun-tasks` - pass
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `52fcb11`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Incoming metadata geometry coverage

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatComposerStaysAnchoredToBottomWhenLatestGenerationAppears,com.gkim.im.android.feature.navigation.GkimRootAppTest#incomingMetadataUsesCompactHeaderWithRightAlignedTimestamp"` - failed first with `bottomGap=1005.0` and `topDelta=239.0`, then passed after the layout refactor and assertion tuning
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#incomingMetadataUsesCompactHeaderWithRightAlignedTimestamp,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelineKeepsIncomingAndSystemAttributionButDropsOutgoingSelfMarkers,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelineKeepsAttachmentAndTimestampForSystemMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelinePinsOutgoingTimestampToBubbleFooterWithoutChangingFormat,com.gkim.im.android.feature.navigation.GkimRootAppTest#shortOutgoingTextBubbleHugsContentWidthInsteadOfRowWidth,com.gkim.im.android.feature.navigation.GkimRootAppTest#longOutgoingAndAttachmentRowsKeepReadableWidthAndStableFooter" --rerun-tasks` - pass
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `52fcb11`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Bottom-anchored composer layout

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatComposerStaysAnchoredToBottomWhenLatestGenerationAppears,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatScreenUsesComposerRowForDefaultSendFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatSecondaryMenuShowsAigcAndMediaActions,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatSecondaryMenuActionsMapToExpectedChatBehavior" --rerun-tasks` - pass, confirmed the composer stays pinned while the latest generation card lives in the scrollable thread and the secondary menu stays above the fixed input row
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `52fcb11`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.2: Denser metadata placement

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#incomingMetadataUsesCompactHeaderWithRightAlignedTimestamp,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelineKeepsIncomingAndSystemAttributionButDropsOutgoingSelfMarkers,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelineKeepsAttachmentAndTimestampForSystemMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelinePinsOutgoingTimestampToBubbleFooterWithoutChangingFormat,com.gkim.im.android.feature.navigation.GkimRootAppTest#shortOutgoingTextBubbleHugsContentWidthInsteadOfRowWidth,com.gkim.im.android.feature.navigation.GkimRootAppTest#longOutgoingAndAttachmentRowsKeepReadableWidthAndStableFooter" --rerun-tasks` - pass, confirmed incoming header timestamps align to the right while outgoing timestamp/footer and adaptive-width bubbles stay intact
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `52fcb11`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
