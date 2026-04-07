## 1. Messages hierarchy refinement

- [x] 1.1 Refactor the Messages screen so the unread summary is a compact supporting element and the conversation list remains the dominant visual block, then verify the updated layout with targeted Compose UI coverage for the messages surface.
- [ ] 1.2 Update the Messages screen interaction tests or previews to assert unread metadata still appears in conversation rows after the hierarchy change, then record the exact verification command and result in the task evidence.

### Task 1.1: Refactor the Messages screen unread hierarchy

- Verification:
  - `cd android && .\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesScreenUsesCompactUnreadSummaryAboveConversationList"` - pass
  - `cd android && .\gradlew.bat testDebugUnitTest` - pass
  - `git diff --check -- android/app/src/main/java/com/gkim/im/android/feature/messages/MessagesRoute.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt` - pass with line-ending warnings only
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `4180760`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

## 2. Chat header and composer restructuring

- [ ] 2.1 Replace the current chat `PageHeader` usage with a compact top identity row that uses the `<` back affordance inline with the contact nickname and preserves access to workshop-related secondary actions, then verify the chat screen still opens and navigates back correctly.
- [ ] 2.2 Replace the full-width AIGC action panel with a messaging-first composer that provides a text input field plus right-aligned send action and a secondary `+` menu trigger, then verify the default composer path works without opening secondary actions.
- [ ] 2.3 Move text-to-image, image-to-image, video-to-video, image picker, and video picker entry points into the secondary `+` menu while keeping their existing callbacks intact, then verify each exposed menu action still maps to the expected chat behavior.

## 3. Message row attribution and regression coverage

- [ ] 3.1 Refactor chat message rows so avatars appear before bubbles and sender labels render above the bubble content for incoming, outgoing, and system entries, then verify the timeline still renders attachments and timestamps cleanly.
- [ ] 3.2 Add or update automated coverage for the compact chat header, secondary action menu, and attributed message layout, then run the relevant Gradle or instrumentation checks and capture the verification evidence required by `docs/DELIVERY_WORKFLOW.md`.
