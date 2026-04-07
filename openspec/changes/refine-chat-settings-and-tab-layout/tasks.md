## 1. Coverage for the revised UX contract

- [x] 1.1 Add or update targeted chat Compose UI coverage so incoming message timestamps assert in-bubble lower-right placement without regressing outgoing bubble footer behavior.
- [x] 1.2 Add or update surface-level Compose UI coverage so Messages starts at `Recent conversations`, Space exposes the unread summary card, and Contacts renders a single dropdown-style sort control instead of horizontal chips.
- [ ] 1.3 Add or update unit or integration coverage for persisted language and theme preferences so Settings changes can be verified independently of screen polish.

## 2. Shared settings preference infrastructure

- [ ] 2.1 Extend the shared preference layer and app-root state so language and theme preferences persist and can be observed across the Android shell.
- [ ] 2.2 Refine the Settings screen to expose Chinese/English language selection and light/dark theme switching while preserving the existing provider and custom endpoint controls.

## 3. Primary-surface layout refinement

- [ ] 3.1 Refactor chat message layout so incoming timestamps move into the lower-right bubble footer while incoming identity cues and outgoing adaptive-width behavior remain intact.
- [ ] 3.2 Move the aggregate unread summary from Messages into Space and simplify the Messages screen chrome so the first non-empty heading begins at `Recent conversations`.
- [ ] 3.3 Replace the Contacts horizontal sort chip row with a single bubble-aligned dropdown control that still supports all existing sort modes.

## 4. Regression and delivery evidence

- [ ] 4.1 Run the relevant settings, chat, Messages, Space, and Contacts verification commands, update any affected geometry or persistence assertions, and capture the evidence required by `docs/DELIVERY_WORKFLOW.md`.

### Task 1.1: Incoming bubble-footer timestamp coverage

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#incomingMetadataPinsTimestampInsideBubbleFooterWhileKeepingSenderAboveBubble,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatTimelinePinsOutgoingTimestampToBubbleFooterWithoutChangingFormat" --rerun-tasks` - failed first at `GkimRootAppTest.kt:212` because the incoming timestamp still rendered above the bubble, then passed after moving incoming/system timestamps into the bubble footer and tightening bubble spacing
  - `git diff --check -- android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt android/app/src/main/java/com/gkim/im/android/feature/chat/ChatRoute.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `2ede2cd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Messages, Space, and Contacts surface coverage

- Verification:
  - `$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; $env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"; .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesScreenStartsAtRecentConversationsWithoutUnreadSummaryPanel,com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenShowsUnreadSummaryAsSupportingContext,com.gkim.im.android.feature.navigation.GkimRootAppTest#contactsScreenUsesSingleDropdownSortControl,com.gkim.im.android.feature.navigation.GkimRootAppTest#contactSortingChangesRenderedRowOrder" --rerun-tasks` - failed first with `headingTop=620.0` plus missing `space-unread-summary` and `contact-sort-dropdown`, then passed after simplifying Messages chrome, adding the Space unread summary card, and switching Contacts to a dropdown trigger
  - `git diff --check -- android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt android/app/src/main/java/com/gkim/im/android/feature/messages/MessagesRoute.kt android/app/src/main/java/com/gkim/im/android/feature/space/SpaceRoute.kt android/app/src/main/java/com/gkim/im/android/feature/contacts/ContactsRoute.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `71bb4b9`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`
