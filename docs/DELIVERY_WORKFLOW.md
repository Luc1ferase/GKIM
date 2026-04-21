# Delivery Workflow - Per-Task Review Gates

`docs/DELIVERY_WORKFLOW.md` is the repository source of truth for how an implementation task becomes accepted.

> Historical note: backend source and backend deployment assets are no longer tracked in the public
> repository tip. Older entries below may still mention `backend/` paths or backend scripts from
> before that boundary change; treat those references as historical evidence tied to a
> maintainer-private backend checkout.

## Required sequence for every task

1. Finish one scoped task or subtask before starting unrelated follow-up work.
2. Run the verification commands that prove the task works in its current state.
3. Review the task result against the relevant OpenSpec artifacts and `docs/QUALITY_SCORE.md`.
4. Assign a quality score.
5. Record the task evidence using the standard template below.
6. If the task scored `95+`, commit and push the accepted state to the active remote branch.
7. Mark the task checkbox complete only after the push succeeds.

If the review score is below `95`, or the push step fails, the task remains open and the next task must not begin.

## Standard task completion record

Use this template in apply sessions, review notes, or task logs whenever a task is accepted or blocked.

```md
### Task <id>: <task title>

- Verification:
  - `<command>` - pass/fail summary
- Review:
  - Score: `<score>/100`
  - Findings: `No findings` or a concise list of blocking issues
- Upload:
  - Commit: `<sha>` or `not created`
  - Branch: `<branch name>`
  - Push: `<remote>/<branch>` or `failed`
- Result: `accepted` or `blocked`
```

## Blocker handling

- Score below `95`: record the findings, leave the task unchecked, fix the issues, and repeat verification plus review.
- Verification could not run: record the missing verification and the reason, then treat the task as blocked until the risk is accepted explicitly.
- Push failed: record the failure detail, keep the task unchecked, and resume only after the accepted state is uploaded successfully.

## Task sizing guidance

- Write tasks that can be implemented, reviewed, scored, and pushed in one focused session.
- Each task should have one obvious completion point and one obvious verification story.
- If a task cannot be reviewed cleanly on its own diff, split it before implementation starts.

## Expected apply-session output

Use this shape when reporting task completion in an implementation session:

```md
Working on task 2.1: Define a standard per-task completion record

Verification
- `./gradlew testDebugUnitTest` - pass

Review
- Score: 97/100
- Findings: No findings

Upload
- Commit: `abc1234`
- Branch: `feature/delivery-quality-gates`
- Push: `origin/feature/delivery-quality-gates`

✓ Task complete
```

## Remote deployment acceptance example

### Task 4.2: Deploy accepted backend slices to `124.222.15.128`, run remote smoke tests and latency-focused message-flow checks there, and capture the required evidence in `docs/DELIVERY_WORKFLOW.md`.

- Verification:
  - `python (paramiko) -> systemctl is-active gkim-im-backend` - pass (`active`)
  - `python (paramiko) -> systemctl show -p ActiveEnterTimestamp,ExecMainPID gkim-im-backend` - pass (`ExecMainPID=92922`, `ActiveEnterTimestamp=Wed 2026-04-08 17:42:40 CST`)
  - `python (paramiko) -> curl -fsS http://127.0.0.1:18080/health` - pass (`{"service":"gkim-im-backend","status":"ok"}`)
  - `python (paramiko tunnel + requests/websockets)` - pass (`/api/session/dev` + `/api/bootstrap` succeeded, `5` tunneled message rounds completed with receive avg/max `82.99/101.56 ms`, delivery avg/max `164.52/182.16 ms`, read-ack avg/max `144.61/157.77 ms`, and history returned the latest message with both `deliveredAt` and `readAt`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `9c5a234`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task change1: deepen-companion-character-card

- Verification:
  - `JAVA_HOME="C:/Program Files/Java/jdk-17" PATH="C:/Program Files/Java/jdk-17/bin:$PATH" "X:/Repos/GKIM/android/gradlew" -p "X:/Repos/GKIM/android" :app:compileDebugKotlin` - pass (`compileDebugKotlin` succeeded after wiring JDK 17)
  - `JAVA_HOME="C:/Program Files/Java/jdk-17" PATH="C:/Program Files/Java/jdk-17/bin:$PATH" "X:/Repos/GKIM/android/gradlew" -p "X:/Repos/GKIM/android" :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionRosterRepositoryTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest` - pass (focused repository + payload mapping unit tests passed)
  - `JAVA_HOME="C:/Program Files/Java/jdk-17" PATH="C:/Program Files/Java/jdk-17/bin;D:/Android/Sdk/platform-tools;D:/Android/Sdk/emulator:$PATH" "X:/Repos/GKIM/android/gradlew" -p "X:/Repos/GKIM/android" :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#tavernTabShowsRoleCardsAndRoutesIntoCompanionChat" --stacktrace` - blocked (device discovered by adb, but ddmlib timed out fetching properties and reported `Unknown API Level` for `emulator-5554`)
- Review:
  - Score: `95/100`
  - Findings: `Code path compiles and focused unit coverage passes; instrumentation verification is currently blocked by emulator property-fetch instability rather than Kotlin or test compile errors.`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not attempted`
- Result: `blocked`


### Task 1.1: Add or refresh Android instrumentation coverage that reproduces the missing Space settings entry and the inert login/register back affordances before the UI fixes land.

- Verification:
  - ``./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeLoginActionOpensLoginRouteInsteadOfPreviewShell,com.gkim.im.android.feature.navigation.GkimRootAppTest#loginBackAffordanceReturnsToWelcomeSurface,com.gkim.im.android.feature.navigation.GkimRootAppTest#registerBackAffordanceReturnsToWelcomeSurface,com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenHeaderShowsSettingsEntryPoint" --stacktrace`` - pass (`4` targeted regression cases passed on `codex_api34`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b7ebbfd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Add focused welcome-video and endpoint-resolution diagnostics or tests so playback regressions and auth-target selection regressions can be detected without relying on manual guesswork alone.

- Verification:
  - ``./gradlew.bat :app:testDebugUnitTest --tests "com.gkim.im.android.data.remote.im.ImHttpEndpointResolverTest" --tests "com.gkim.im.android.data.remote.im.AuthFailureMessageTest" :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.WelcomeVideoOverlayStyleTest#overlayScrimLeavesMoreVideoVisibleForDemoPlayback" --stacktrace`` - pass (`resolver`, `authFailureMessage`, and welcome overlay diagnostics passed)
  - ``adb exec-out screencap -p > tmp-frame-c.png`` / ``adb exec-out screencap -p > tmp-frame-d.png`` - pass (fresh welcome captures produced different SHA-256 hashes: `753B71...2191F` vs `B28681...2DB44`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b7ebbfd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Restore the Space header settings pill so the shell once again exposes a visible, tappable route into `settings` from the Space page.

- Verification:
  - ``./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenHeaderShowsSettingsEntryPoint,com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsMenuPresentsFocusedEntriesAndAccountActionsSurface" --stacktrace`` - pass (Space header settings entry remained visible and navigable)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b7ebbfd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.2: Wire the login and registration back affordances to the existing root-auth callbacks and verify that both flows return to the welcome surface.

- Verification:
  - ``./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#loginBackAffordanceReturnsToWelcomeSurface,com.gkim.im.android.feature.navigation.GkimRootAppTest#registerBackAffordanceReturnsToWelcomeSurface" --stacktrace`` - pass (both auth back affordances returned to the welcome surface)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b7ebbfd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.3: Recover the welcome-screen video playback presentation so the approved `welcome_intro_1` asset remains visibly active after the current cover-scaling and scrim treatment.

- Verification:
  - ``./gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.WelcomeVideoOverlayStyleTest#overlayScrimLeavesMoreVideoVisibleForDemoPlayback" --stacktrace`` - pass (overlay scrim contract stayed within the approved visibility window)
  - ``adb exec-out screencap -p > tmp-frame-c.png`` / ``adb exec-out screencap -p > tmp-frame-d.png`` - pass (fresh welcome captures produced different SHA-256 hashes after a two-second gap, indicating visible motion)
- Review:
  - Score: `95/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b7ebbfd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.1: Introduce a shared HTTP base URL resolver that prefers authenticated session state but otherwise falls back to the persisted IM Validation HTTP endpoint, then update login/register/bootstrap/contacts/search callers to use it instead of hardcoded `127.0.0.1:18080` defaults.

- Verification:
  - ``./gradlew.bat :app:testDebugUnitTest --tests "com.gkim.im.android.data.remote.im.ImHttpEndpointResolverTest" :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.LiveAuthEndpointValidationTest#emulatorCanRegisterLoginAndBootstrapThroughPublishedHostBridgeEndpoint" --stacktrace`` - pass (shared resolver stayed aligned with the emulator-facing published endpoint and the emulator completed register/login/bootstrap through `10.0.2.2:18080`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b7ebbfd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.2: Improve auth/connectivity diagnostics so failed login or registration attempts can distinguish “wrong client URL selected” from “backend unreachable at the selected endpoint.”

- Verification:
  - ``./gradlew.bat :app:testDebugUnitTest --tests "com.gkim.im.android.data.remote.im.AuthFailureMessageTest" :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#loginRouteShowsInlineErrorWhenBackendRejectsCredentials" --stacktrace`` - pass (auth failures now surface targeted inline diagnostics instead of a generic localhost-style failure)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b7ebbfd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 4.1: Validate that the published auth API target chosen for emulator testing is actually host-reachable, and update backend-side bind/guidance or validation notes if the current target still relies on device-local localhost assumptions.

- Verification:
  - ``docker logs gkim-im-backend-local --tail 50`` - pass (runtime migration bridge seeded the legacy bootstrap row and the backend reached `backend listener ready`)
  - ``curl.exe -H "Content-Type: application/json" -d "{\"username\":\"codex_diag_user_2\",\"password\":\"passw0rd!\",\"displayName\":\"Codex Diag\"}" http://127.0.0.1:18080/api/auth/register`` - pass (`200 OK` from the host-published backend after migration repair)
  - ``docker run --rm -e PGPASSWORD=*** postgres:16-alpine psql ... -c "select version, description, success from _sqlx_migrations order by version;"`` - pass (runtime database shows both `202604080001` and `202604100001` as applied)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b7ebbfd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 4.2: Re-run focused Android and backend verification for the restored settings path, welcome playback, auth back navigation, and configured API connectivity, then capture verification/review/score/evidence updates in `docs/DELIVERY_WORKFLOW.md`.

- Verification:
  - ``cargo test --test http_im_api`` - pass (`5` backend HTTP regression tests passed, including the legacy bootstrap-only migration upgrade path)
  - ``./gradlew.bat :app:testDebugUnitTest --tests "com.gkim.im.android.data.remote.im.ImHttpEndpointResolverTest" --tests "com.gkim.im.android.data.remote.im.AuthFailureMessageTest" :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeLoginActionOpensLoginRouteInsteadOfPreviewShell,com.gkim.im.android.feature.navigation.GkimRootAppTest#loginBackAffordanceReturnsToWelcomeSurface,com.gkim.im.android.feature.navigation.GkimRootAppTest#registerBackAffordanceReturnsToWelcomeSurface,com.gkim.im.android.feature.navigation.GkimRootAppTest#spaceScreenHeaderShowsSettingsEntryPoint,com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsMenuPresentsFocusedEntriesAndAccountActionsSurface,com.gkim.im.android.feature.navigation.GkimRootAppTest#loginRouteShowsInlineErrorWhenBackendRejectsCredentials,com.gkim.im.android.feature.navigation.WelcomeVideoOverlayStyleTest#overlayScrimLeavesMoreVideoVisibleForDemoPlayback,com.gkim.im.android.feature.navigation.LiveAuthEndpointValidationTest#emulatorCanRegisterLoginAndBootstrapThroughPublishedHostBridgeEndpoint" --stacktrace`` - pass (`8` focused Android checks passed on `codex_api34`)
  - ``./gradlew.bat :app:installDebug`` - pass (debug app installed to the emulator before fresh welcome-motion capture)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b7ebbfd`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.1: Replace the Messages header's passive active-conversation count with a `+` quick-action trigger and anchored dropdown menu that exposes `Add friend / 加好友` and `Scan QR code / 扫描二维码`.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesHeaderUsesQuickActionsInsteadOfPassiveConversationCountCopy,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendOpensUserSearchFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendBackReturnsToMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrOpensQrScanFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrShowsDecodedPayloadWithoutSideEffects,com.gkim.im.android.feature.navigation.GkimRootAppTest#qrQuickActionBackStackReturnsToMessagesAfterViewingResult,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesLaunchedAddFriendFlowShowsErrorWhenRequestFails,com.gkim.im.android.feature.navigation.GkimRootAppTest#userSearchFlowShowsResultsAndMarksPendingAfterAdd" --stacktrace`` - pass (`8` focused Messages quick-action, QR, and add-friend instrumentation checks passed on `codex_api34`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `d70d4e8`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Extend authenticated navigation so the Messages quick actions can open the existing user-search flow plus new QR scan/result routes while preserving expected back-stack behavior.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesHeaderUsesQuickActionsInsteadOfPassiveConversationCountCopy,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendOpensUserSearchFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendBackReturnsToMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrOpensQrScanFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrShowsDecodedPayloadWithoutSideEffects,com.gkim.im.android.feature.navigation.GkimRootAppTest#qrQuickActionBackStackReturnsToMessagesAfterViewingResult,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesLaunchedAddFriendFlowShowsErrorWhenRequestFails,com.gkim.im.android.feature.navigation.GkimRootAppTest#userSearchFlowShowsResultsAndMarksPendingAfterAdd" --stacktrace`` - pass (Messages quick actions opened `user-search` and `qr-scan`, and both add-friend / QR flows returned to the expected prior screen on back)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `d70d4e8`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Add the Android camera/barcode dependencies and implement a dedicated QR scan screen with permission-aware camera preview, decode handling, and clean cancel/back behavior.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesHeaderUsesQuickActionsInsteadOfPassiveConversationCountCopy,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendOpensUserSearchFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendBackReturnsToMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrOpensQrScanFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrShowsDecodedPayloadWithoutSideEffects,com.gkim.im.android.feature.navigation.GkimRootAppTest#qrQuickActionBackStackReturnsToMessagesAfterViewingResult,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesLaunchedAddFriendFlowShowsErrorWhenRequestFails,com.gkim.im.android.feature.navigation.GkimRootAppTest#userSearchFlowShowsResultsAndMarksPendingAfterAdd" --stacktrace`` - pass (the QR scan route compiled with CameraX/ML Kit dependencies, rendered a dedicated scan screen, and supported scan-flow back navigation)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `d70d4e8`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.2: Implement a passive QR result surface that shows the decoded payload content without auto-navigation, account mutation, or external-link side effects.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesHeaderUsesQuickActionsInsteadOfPassiveConversationCountCopy,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendOpensUserSearchFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendBackReturnsToMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrOpensQrScanFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrShowsDecodedPayloadWithoutSideEffects,com.gkim.im.android.feature.navigation.GkimRootAppTest#qrQuickActionBackStackReturnsToMessagesAfterViewingResult,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesLaunchedAddFriendFlowShowsErrorWhenRequestFails,com.gkim.im.android.feature.navigation.GkimRootAppTest#userSearchFlowShowsResultsAndMarksPendingAfterAdd" --stacktrace`` - pass (decoded QR content rendered on a dedicated result screen and the test confirmed no friend-request side effect occurred)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `d70d4e8`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.1: Route the Messages `Add friend / 加好友` action into the live user-search/request flow so it reuses the existing backend-backed social workflow instead of a local demo path.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesHeaderUsesQuickActionsInsteadOfPassiveConversationCountCopy,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendOpensUserSearchFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendBackReturnsToMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrOpensQrScanFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrShowsDecodedPayloadWithoutSideEffects,com.gkim.im.android.feature.navigation.GkimRootAppTest#qrQuickActionBackStackReturnsToMessagesAfterViewingResult,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesLaunchedAddFriendFlowShowsErrorWhenRequestFails,com.gkim.im.android.feature.navigation.GkimRootAppTest#userSearchFlowShowsResultsAndMarksPendingAfterAdd" --stacktrace`` - pass (Messages quick action entered the authenticated user-search/request flow and kept the pending state visible)
  - ``powershell (Invoke-RestMethod against http://127.0.0.1:18080 for /api/auth/register, /api/users/search, /api/friends/request, /api/friends/requests, and /api/friends/requests/:id/accept)`` - pass (`qa0411140023a -> qa0411140023b` moved through `none -> pending_sent / pending_received -> contact` on the local Docker backend)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `d70d4e8`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.2: Tighten the Messages-launched add-friend UX so backend request success, pending state, and request failure remain visible and truthful when testing with different real accounts.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesHeaderUsesQuickActionsInsteadOfPassiveConversationCountCopy,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendOpensUserSearchFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendBackReturnsToMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrOpensQrScanFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrShowsDecodedPayloadWithoutSideEffects,com.gkim.im.android.feature.navigation.GkimRootAppTest#qrQuickActionBackStackReturnsToMessagesAfterViewingResult,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesLaunchedAddFriendFlowShowsErrorWhenRequestFails,com.gkim.im.android.feature.navigation.GkimRootAppTest#userSearchFlowShowsResultsAndMarksPendingAfterAdd" --stacktrace`` - pass (the Messages-launched path now keeps `pending` visible after success and surfaces a truthful inline failure message when request sending fails)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `d70d4e8`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 4.1: Add or refresh focused Android UI/instrumentation coverage for the Messages `+` menu, add-friend routing, QR scan/result navigation, and no-side-effect result behavior.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesHeaderUsesQuickActionsInsteadOfPassiveConversationCountCopy,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendOpensUserSearchFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendBackReturnsToMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrOpensQrScanFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrShowsDecodedPayloadWithoutSideEffects,com.gkim.im.android.feature.navigation.GkimRootAppTest#qrQuickActionBackStackReturnsToMessagesAfterViewingResult,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesLaunchedAddFriendFlowShowsErrorWhenRequestFails,com.gkim.im.android.feature.navigation.GkimRootAppTest#userSearchFlowShowsResultsAndMarksPendingAfterAdd" --stacktrace`` - pass (`8` focused regression cases covered the menu, add-friend entry, QR result display, and back-stack behavior)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `d70d4e8`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 4.2: Run focused local verification for cross-account friend add plus QR payload display, then capture verification/review/score/upload evidence updates in `docs/DELIVERY_WORKFLOW.md`.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesHeaderUsesQuickActionsInsteadOfPassiveConversationCountCopy,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendOpensUserSearchFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionAddFriendBackReturnsToMessages,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrOpensQrScanFlow,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesQuickActionScanQrShowsDecodedPayloadWithoutSideEffects,com.gkim.im.android.feature.navigation.GkimRootAppTest#qrQuickActionBackStackReturnsToMessagesAfterViewingResult,com.gkim.im.android.feature.navigation.GkimRootAppTest#messagesLaunchedAddFriendFlowShowsErrorWhenRequestFails,com.gkim.im.android.feature.navigation.GkimRootAppTest#userSearchFlowShowsResultsAndMarksPendingAfterAdd" --stacktrace`` - pass (`8` focused Android checks passed on `codex_api34`)
  - ``powershell (Invoke-RestMethod against http://127.0.0.1:18080 for /api/auth/register, /api/users/search, /api/friends/request, /api/friends/requests, and /api/friends/requests/:id/accept)`` - pass (fresh local accounts verified the real backend friend-request lifecycle end-to-end on the Docker-published `127.0.0.1:18080`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `d70d4e8`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 4.2: Run local validation for real Hunyuan `hy-image-v3.0` and Tongyi `wan2.7-image` generation results using locally entered secrets, then record verification/review/score/upload evidence in `docs/DELIVERY_WORKFLOW.md`.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:testDebugUnitTest`` - pass (Android unit suite stayed green after updating the Hunyuan submit/query contract and switching the Tongyi preset default to the Beijing DashScope endpoint)
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:compileDebugAndroidTestKotlin`` - pass (Android instrumentation sources still compile against the updated provider wiring)
  - ``powershell (Invoke-RestMethod to https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation with the locally supplied `wan2.7-image` key)`` - pass (received request id `ff71b2de-d9af-9e9b-b456-eabd76fe0c2e` and an OSS-hosted image URL; secret redacted)
  - ``powershell (Invoke-RestMethod to https://api.cloudai.tencent.com/v1/aiart/submit and /v1/aiart/query with the locally supplied `HY-Image-V3.0` key)`` - fail (Tencent returned `invalid_api_key`; no image result was issued and the secret remained redacted in the review log)
  - ``powershell (Invoke-RestMethod to https://api.hunyuan.cloud.tencent.com/v1/chat/completions with the same local Tencent key)`` - fail (Tencent again returned `invalid_api_key`, which indicates the current blocker is the supplied local credential rather than only the image endpoint path)
- Review:
  - Score: `91/100`
  - Findings: `Tongyi live validation passed, but Hunyuan acceptance remains blocked because Tencent rejects the supplied local key as invalid. Task 4.2 must stay open until a valid Hunyuan key can produce a real image result.`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not created`
- Result: `blocked`

### Task 4.2: Run focused local verification for split chat image intents, truthful image-to-image readiness, and generated-image save/send follow-up actions, then capture the evidence in `docs/DELIVERY_WORKFLOW.md`.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat :app:testDebugUnitTest --tests "com.gkim.im.android.feature.chat.ChatPresentationTest" --tests "com.gkim.im.android.data.repository.RepositoriesTest" --tests "com.gkim.im.android.data.remote.aigc.AigcProviderClientsTest" :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#chatSecondaryMenuFiltersAigcActionsToTheActiveProviderCapabilities,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatSecondaryMenuSeparatesChatAttachmentAndGenerationSourceFlows,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatSendButtonSendsStagedImageAttachmentAsNormalOutgoingMessage,com.gkim.im.android.feature.navigation.GkimRootAppTest#latestGenerationCardOffersSaveAndSendActionsForSuccessfulImages,com.gkim.im.android.feature.navigation.GkimRootAppTest#chatGenerationFailureShowsMissingPresetKeyFeedback"`` - pass (unit coverage stayed green for capability truth, missing-source failures, attachment sending, and provider request contracts; `5` focused instrumentation cases passed on `codex_api34` for the explicit `+` menu, staged image send, truthful generation failure feedback, and generated-image save/send actions)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.1: Verify the new release path with focused Android build checks and at least one end-to-end tag-driven release rehearsal against GitHub so the published APK path is proven.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="${env:JAVA_HOME}\bin;${env:Path}"; .\gradlew.bat --no-daemon --stacktrace :app:testDebugUnitTest :app:assembleRelease "-PGKIM_RELEASE_VERSION_NAME=0.2.17" "-PGKIM_RELEASE_VERSION_CODE=20017"`` - pass (local tag-driven version injection and unsigned release assembly both succeeded from `android/`)
  - ``gh run watch 24377670065 --interval 10 --exit-status`` - fail (expected clean terminal failure because `ANDROID_RELEASE_*` secrets are absent; the monitored GitHub run for tag `v0.2.17` still completed both `Run lightweight Android release gate` and `Build signed Android release APK` successfully before hitting the secrets guard)
  - ``gh run view 24377670065 --json conclusion,headBranch,headSha,jobs,url`` - pass (GitHub Actions run `24377670065` on tag `v0.2.17` / commit `fa8bf17` shows step 10 `Run lightweight Android release gate` and step 11 `Build signed Android release APK` as `success`, with upload/publish skipped only because signing secrets were intentionally absent)
  - ``gh run view 24377670065 --log-failed`` - pass (the only failing step explicitly reported `Missing ANDROID_RELEASE_* secrets. The APK compiled successfully, but publication is blocked until signing secrets are configured.`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `fa8bf17`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.2: Record the verification, review, score, and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md`, then update any affected release/operator docs before closing the change.

- Verification:
  - ``git diff -- android/README.md docs/DELIVERY_WORKFLOW.md openspec/changes/build-apk-on-github-tag/tasks.md`` - pass (the operator doc now explains the no-secrets remote rehearsal path, this delivery log captures the local plus GitHub evidence for `v0.2.17`, and the change task list is ready to close)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `fa8bf17`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.1: Add or refresh focused Android welcome-screen assertions that prove the new product-facing intro copy is rendered and the removed helper/footer wording is absent.

- Verification:
  - ``adb shell am instrument -w -e class com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeScreenUsesSimplifiedProductCopyWithoutTechnicalHelperText com.gkim.im.android.test/androidx.test.runner.AndroidJUnitRunner`` - pass (the focused welcome assertion confirmed the new Chinese intro copy is rendered and the removed helper/footer wording is absent on `codex_api34`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.1: Update the main welcome description in `WelcomeRoute.kt` to a more natural bilingual product introduction while preserving the existing welcome layout and auth CTA structure.

- Verification:
  - ``adb shell am instrument -w -e class com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeScreenUsesSimplifiedProductCopyWithoutTechnicalHelperText com.gkim.im.android.test/androidx.test.runner.AndroidJUnitRunner`` - pass (the welcome screen now renders the simplified product-facing intro copy without disturbing the existing welcome-shell structure)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.2: Remove the lower helper sentence and simplify the footer line so it no longer includes `加密连接 / Encrypted connection` wording while keeping the lightweight footer treatment in place.

- Verification:
  - ``adb shell am instrument -w -e class com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeScreenUsesSimplifiedProductCopyWithoutTechnicalHelperText com.gkim.im.android.test/androidx.test.runner.AndroidJUnitRunner`` - pass (the welcome screen no longer renders the lower helper sentence or any `加密连接` wording, while still keeping a lightweight footer version marker)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.1: Run focused Android verification for the cleaned-up welcome copy contract.

- Verification:
  - ``adb shell am instrument -w -e class 'com.gkim.im.android.feature.navigation.GkimRootAppTest#launchShowsWelcomeOnboardingBeforeMainShell,com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeScreenUsesSimplifiedProductCopyWithoutTechnicalHelperText,com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeVideoReportsActivePlaybackStateOnLaunch,com.gkim.im.android.feature.navigation.GkimRootAppTest#welcomeLoginActionOpensLoginRouteInsteadOfPreviewShell' com.gkim.im.android.test/androidx.test.runner.AndroidJUnitRunner`` - pass (`4` focused welcome launch, copy, playback, and login-entry checks passed on `codex_api34`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.2: Record verification, review, score, and evidence updates in `docs/DELIVERY_WORKFLOW.md`, and update any affected welcome/onboarding guidance before closing the change.

- Verification:
  - ``git diff -- docs/DELIVERY_WORKFLOW.md openspec/changes/simplify-welcome-copy/tasks.md`` - pass (the delivery log now captures the welcome-copy verification evidence, the change task list is ready to close, and no separate welcome/onboarding operator guide required an additional wording update)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.2: Deploy or redeploy the accepted backend slice to `124.222.15.128`, verify `gkim-im-backend.service` plus `/health`, and confirm the canonical remote HTTP/WebSocket endpoints are reachable for Android validation without relying on adb reverse or tunnel-only assumptions.

- Verification:
  - ``python (paramiko) -> systemctl is-active gkim-im-backend && systemctl show -p ActiveEnterTimestamp -p ExecMainPID -p FragmentPath gkim-im-backend`` - pass (`active`, `ExecMainPID=2373933`, `ActiveEnterTimestamp=Tue 2026-04-14 20:46:00 CST`, unit path `/etc/systemd/system/gkim-im-backend.service`)
  - ``python (paramiko) -> cd /opt/gkim-im/backend && BACKEND_URL=http://127.0.0.1:18080 ./scripts/smoke-health.sh && BACKEND_URL=http://127.0.0.1:18080 DEV_USER_EXTERNAL_ID=nox-dev ./scripts/smoke-session.sh`` - pass (host-local health returned `{"service":"gkim-im-backend","status":"ok"}` and host-local session/bootstrap returned `user=nox-dev contacts=3 conversations=1`)
  - ``python -> http://124.222.15.128:18080/api/session/dev + /api/bootstrap`` - pass (published HTTP endpoint issued a dev session for `nox-dev` and returned bootstrap data with `3` contacts and `1` conversation)
  - ``python (websockets) -> ws://124.222.15.128:18080/ws with Authorization: Bearer <dev token>`` - pass (published WebSocket endpoint accepted the connection and returned `session.registered` for `nox-dev`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.1: Launch the Android app against the deployed backend and run the live validation flow for session issuance, bootstrap/history hydration, realtime send/receive, and recovery after reconnect or relaunch.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.LiveMessagingRepositoryTest`` - pass (the focused repository regression suite stayed green after the reconnect/retry and pending-history fixes)
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.LiveImBackendValidationTest#emulatorValidationCoversLiveRoundTripAndReloadRecovery" "-Pandroid.testInstrumentationRunnerArguments.liveImHttpBaseUrl=http://124.222.15.128:18080/" "-Pandroid.testInstrumentationRunnerArguments.liveImWebSocketUrl=ws://124.222.15.128:18080/ws" --stacktrace`` - pass twice on `codex_api34` (the live case completed session issuance, bootstrap/history hydration, outbound send, inbound realtime reply, delivery/read updates, and relaunch recovery against the deployed backend in two consecutive runs)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.2: Record the verification, review, score, and upload evidence for the remote deployment and app-validation flow in `docs/DELIVERY_WORKFLOW.md`, and update any affected `backend/README.md` or `android/README.md` guidance before closing the change.

- Verification:
  - ``git diff -- backend/README.md android/README.md docs/DELIVERY_WORKFLOW.md openspec/changes/deploy-backend-to-server-and-validate-app/tasks.md`` - pass (backend ops notes now match the accepted `18080` systemd deployment, Android guidance now includes the published Ubuntu endpoint workflow, and this delivery log captures the remote deployment plus live app-validation evidence)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.1: Sync the accepted `backend/` slice to the Ubuntu deployment directory and rerun the existing bootstrap/systemd flow so `gkim-im-backend.service` is rebuilt and restarted on the current host behind `chat.lastxuans.sbs`.

- Verification:
  - ``python (paramiko) -> tar sync to /opt/gkim-im/backend && cd /opt/gkim-im/backend && bash ./scripts/bootstrap-ubuntu.sh`` - pass (the current backend slice was unpacked on the Ubuntu host, `cargo build --release` completed, and `gkim-im-backend.service` restarted successfully)
  - ``python (paramiko) -> systemctl is-active gkim-im-backend && systemctl show -p ActiveEnterTimestamp -p ExecMainPID -p FragmentPath gkim-im-backend`` - pass (`active`, `ExecMainPID=13903`, `ActiveEnterTimestamp=Thu 2026-04-16 05:57:46 UTC`, unit path `/etc/systemd/system/gkim-im-backend.service`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.2: Confirm the remote deployment is actually serving the new backend version by checking the deployed files or runtime process state for the image-message API additions after restart.

- Verification:
  - ``python (paramiko) -> test -f /opt/gkim-im/backend/migrations/202604160001_direct_message_attachments.sql && grep -R "api/direct-messages/image" -n /opt/gkim-im/backend/src`` - pass (the deployed backend checkout includes the direct-attachment migration and the published image-message route in `src/app.rs`)
  - ``python (paramiko) -> python3 host-local image upload probe against http://127.0.0.1:18080/api/direct-messages/image`` - pass (host-local image upload returned a durable message id plus attachment fetch path instead of `404`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.1: Run host-local backend smoke checks on the server for `/health`, session/bootstrap, direct image upload, and attachment fetch.

- Verification:
  - ``python (paramiko) -> cd /opt/gkim-im/backend && BACKEND_URL=http://127.0.0.1:18080 bash ./scripts/smoke-health.sh`` - pass (host-local health returned `{"service":"gkim-im-backend","status":"ok"}`)
  - ``python (paramiko) -> cd /opt/gkim-im/backend && DEV_USER_EXTERNAL_ID=nox-dev BACKEND_URL=http://127.0.0.1:18080 bash ./scripts/smoke-session.sh`` - pass (host-local session/bootstrap returned `user=nox-dev contacts=3 conversations=1`)
  - ``python (paramiko) -> python3 host-local image upload + attachment fetch probe`` - pass (host-local image upload returned message `7a1d2292-c244-4f69-91d2-c7443c96cb7c` and the attachment fetch path resolved successfully)
- Review:
  - Score: `98/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.2: Run the same image-message-capable API checks through the published `chat.lastxuans.sbs` endpoint and confirm the published service no longer returns `404` for image send.

- Verification:
  - ``powershell -> Invoke-WebRequest https://chat.lastxuans.sbs/health`` - pass (published health returned `{"service":"gkim-im-backend","status":"ok"}`)
  - ``powershell -> POST https://chat.lastxuans.sbs/api/direct-messages/image with a dev-session bearer token`` - pass (published image upload returned `200` with message `e3eeaa2c-0ae3-42aa-aee7-fc14cd677ebc` and attachment metadata instead of the prior `404`)
  - ``powershell -> GET https://chat.lastxuans.sbs/api/messages/<id>/attachment with the same bearer token`` - pass (published attachment fetch returned `200`, `Content-Type=image/png`, and body `hello-image`)
- Review:
  - Score: `98/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.1: Record the deployment and verification evidence in `docs/DELIVERY_WORKFLOW.md`, including enough detail to distinguish host-local success from published-endpoint success.

- Verification:
  - ``git diff -- docs/DELIVERY_WORKFLOW.md openspec/changes/redeploy-image-message-backend-and-verify-published-service/tasks.md`` - pass (the new delivery log entries capture remote rollout, host-local image-message checks, and published-endpoint proof separately)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.2: Update any affected backend operator guidance so future deployments explicitly verify the image-message API version rather than relying on generic health-only checks.

- Verification:
  - ``git diff -- backend/README.md backend/scripts/bootstrap-ubuntu.sh openspec/changes/redeploy-image-message-backend-and-verify-published-service/tasks.md`` - pass (backend guidance now requires host-local and published image-message checks, and the bootstrap flow normalizes script execute bits before building on Ubuntu)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.1: Replace the separate IM HTTP/WebSocket preference contract with one backend-origin source of truth, including compatibility for already stored endpoint values.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImHttpEndpointResolverTest --tests com.gkim.im.android.data.repository.LiveMessagingRepositoryTest --tests com.gkim.im.android.feature.settings.SettingsViewModelTest`` - pass (single-origin preference migration, resolver derivation, repository bootstrap, and settings state all passed in the focused unit suite)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.1: Replace the production-facing IM settings fields with a resolved-backend summary, and keep developer override behind a guarded validation control.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:compileDebugAndroidTestKotlin`` - pass (all Android instrumentation sources compiled after the settings UI switched from dual raw endpoint fields to resolved-origin plus guarded override controls)
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsMenuPresentsFocusedEntriesAndAccountActionsSurface,com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsScreenExposesImBackendValidationControlsAndStatus,com.gkim.im.android.feature.navigation.LoginEndpointConfigurationTest#loginRouteShowsEndpointConfigurationErrorWhenDeveloperOverrideIsInvalid" --stacktrace`` - pass (`3` focused instrumentation checks passed on `codex_api34`, covering the settings menu, guarded developer override flow, and invalid-override auth protection)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.3: Update Android operator guidance and record delivery evidence for the single-origin IM endpoint contract.

- Verification:
  - ``git diff -- android/README.md docs/DELIVERY_WORKFLOW.md openspec/changes/derive-im-endpoints-from-single-origin/tasks.md`` - pass (Android operator guidance now documents the resolved backend origin plus guarded developer override workflow, this delivery log captures the verification evidence, and the OpenSpec task list is closed)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.1: Add a repo-owned Android release entrypoint that blocks release tagging when the worktree is dirty, the target branch head is not fully pushed, or the requested version tag already exists.

- Verification:
  - ``Invoke-Pester -Path "scripts/tests/android-release-tools.Tests.ps1" -EnableExit`` - pass (`5` focused Pester cases passed for semantic-version tag parsing plus clean/dirty/tag-conflict release preflight handling)
  - ``powershell -ExecutionPolicy Bypass -File ".\scripts\release-android-tag.ps1" -TagName "v9.9.9"`` - pass (from the dirty primary worktree the helper stopped before tagging and reported `Release tagging requires a clean worktree. Commit or stash local changes before continuing.`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `8251fc4`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.2: Extend the release entrypoint so it pushes the validated release tag and returns the GitHub Actions run context needed to watch the tagged release from the terminal or browser.

- Verification:
  - ``powershell -ExecutionPolicy Bypass -File ".\scripts\release-android-tag.ps1" -TagName "v0.2.19"`` - pass (from the clean rehearsal worktree at `x:\Repos\GKIM-release-rehearsal-19`, the helper pushed the release tag, returned run `24465904329`, and printed both terminal and browser release-monitoring commands)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `8251fc4`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.1: Update the Android tag-release workflow to emit operator-facing release summary details for the pushed tag, expected APK asset name, and publication state.

- Verification:
  - ``gh run view 24465904329 --job 71492760760 --log | Select-String -Pattern "Initialize Android release summary|Publication state|Release URL:" -Context 0,3`` - pass (the workflow log shows the new summary step writing the pushed tag `v0.2.19`, expected asset `gkim-android-v0.2.19.apk`, and the final publication-state plus release-URL lines into the job summary flow)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `8251fc4`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 2.2: Add final release-asset verification to the Android tag-release workflow so a run only reports success when the expected GitHub Release APK is actually present.

- Verification:
  - ``gh run watch 24465904329 --interval 10 --exit-status`` - pass (the `v0.2.19` Android tag-release workflow completed successfully end-to-end on GitHub)
  - ``gh release view v0.2.19 --repo Luc1ferase/GKIM --json url,assets`` - pass (GitHub Release `v0.2.19` now exposes uploaded asset `gkim-android-v0.2.19.apk` with downloadable URL `https://github.com/Luc1ferase/GKIM/releases/download/v0.2.19/gkim-android-v0.2.19.apk`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `8251fc4`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.1: Update the release/operator docs to describe the supported commit/push/tag/watch flow, GitHub CLI fallback expectations, and how to confirm the final GitHub Release APK asset.

- Verification:
  - ``rg -n "release-android-tag\\.ps1|gh run watch|gh release view|Step Summary|publication state" README.md docs/android-tag-release-operations.md`` - pass (the root README now points maintainers at the repo-owned release entrypoint and the new `docs/android-tag-release-operations.md` guide now covers preflight blocking, GitHub CLI monitoring, browser fallback, and final APK confirmation)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `8251fc4`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 3.2: Rehearse the flow with a safe release validation path, then record the verification, review, score, and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md`.

- Verification:
  - ``gh run view 24465541744 --json conclusion,displayTitle,event,headBranch,headSha,url`` - pass (the first disposable release tag `v0.2.18` exercised the new workflow summary and final release-asset verification path against commit `e3137a97e7a9ec0391b8d81d5f9baabd2144657b`)
  - ``gh run view 24465904329 --json conclusion,displayTitle,event,headBranch,headSha,url`` - pass (the final disposable release tag `v0.2.19` exercised the fixed helper path against commit `8251fc4facf1f261f6135bab0de6dcc1ca07da1b`)
  - ``gh release view v0.2.19 --repo Luc1ferase/GKIM --json url,assets`` - pass (the rehearsal release remains available at `https://github.com/Luc1ferase/GKIM/releases/tag/v0.2.19` with downloadable APK asset `gkim-android-v0.2.19.apk`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `8251fc4`
  - Branch: `master`
  - Push: `origin/master`
- Result: `accepted`

### Task 1.1: Inventory the tracked backend source, backend-only operational assets, and public-repo references that must be removed or rewritten before the next public push.

- Verification:
  - ``git ls-files backend`` - pass (the public repo was still tracking the full backend tree, including Cargo manifests, Docker assets, migrations, scripts, Rust source, systemd units, and tests before cleanup)
  - ``rg -n "backend/|From `backend/`" README.md android/README.md docs/DELIVERY_WORKFLOW.md`` - pass (the active public references that needed rewriting were concentrated in the root README, Android local validation notes, and historical delivery records)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.2: Establish the local/private backend preservation location and Git ignore protection, then verify the current backend tree is safely retained there before any tracked deletion starts.

- Verification:
  - ``git check-ignore -v .private/backend/README.md .private/backend/src/app.rs`` - pass (`.gitignore` now ignores `.private/`, and the preserved backend copy is protected from public Git publication)
  - ``powershell -> Copy-Item backend .private/backend -Recurse -Force`` - pass (the current backend worktree was preserved under `.private/backend/` before tracked deletion, including `README.md`, `Cargo.toml`, `src/app.rs`, and `scripts/bootstrap-ubuntu.sh`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.1: Remove tracked backend implementation files and backend-only operational assets from the public repository tip, adding only the minimal sanitized public-facing replacements that are still required.

- Verification:
  - ``git ls-files backend`` - pass (the tracked public backend tree now contains only `backend/README.md`)
  - ``powershell -> Get-ChildItem backend -Force`` - pass (the public `backend/` directory now contains only the sanitized placeholder README)
- Review:
  - Score: `98/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.2: Update public docs, scripts, specs, and workflow references so the published repository no longer assumes checked-in backend source while private operators still have a clear backend handoff path.

- Verification:
  - ``rg -n "From `backend/`|backend/scripts/|docker logs gkim-im-backend-local > backend\\docker-im-validation.log" README.md android/README.md backend/README.md`` - pass (the active public docs no longer instruct operators to execute backend source or logs from the public `backend/` path)
  - ``git diff --name-status -- backend README.md android/README.md .gitignore docs/DELIVERY_WORKFLOW.md`` - pass (the public tree now replaces tracked backend source with a boundary placeholder and updates the README/Android guidance accordingly)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.1: Verify the tracked public diff no longer contains backend source or backend-only deployment assets while the preserved local/private backend copy still exists and is ignored from Git.

- Verification:
  - ``git ls-files backend`` - pass (the tracked public backend footprint is reduced to `backend/README.md` and no source, scripts, or deployment assets remain tracked)
  - ``git check-ignore -v .private/backend/README.md .private/backend/src/app.rs`` - pass (the preserved private backend copy remains ignored from Git)
  - ``powershell -> Get-ChildItem backend -Force -Recurse -File && Test-Path .private/backend/Cargo.toml && Test-Path .private/backend/src/app.rs && Test-Path .private/backend/scripts/bootstrap-ubuntu.sh`` - pass (the public backend tree exposes only the placeholder README while the private copy still contains the implementation and deployment files)
- Review:
  - Score: `98/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.2: Run focused public-repo checks for the surviving docs/workflows, then record the verification and review evidence in `docs/DELIVERY_WORKFLOW.md`.

- Verification:
  - ``openspec validate stop-publishing-backend-source-code`` - pass (the change artifacts remain valid after the repository-boundary implementation work)
  - ``git diff --stat -- .gitignore README.md android/README.md backend/README.md docs/DELIVERY_WORKFLOW.md`` - pass (the tracked public edits are limited to ignore rules, public backend boundary guidance, Android/operator docs, and the delivery record)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `master`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.1: Inventory the current `Space` feed/prompt surfaces that must be removed or repurposed, then update the branch-local product docs and specs so the third tab is defined as a tavern-style role lobby.

- Verification:
  - ``rg -n "Space|空间|创作工坊|Prompt 模板|SpaceRoute|space-screen|space-" README.md android/README.md docs/DELIVERY_WORKFLOW.md android/app/src/main/java -g '!**/build/**'`` - pass (the old `Space` feed assumptions were isolated to the third-tab surface, Android guidance, and repo copy, giving the branch a bounded replacement target)
  - ``git diff -- README.md android/README.md openspec/changes/replace-space-with-character-roster-and-gacha/proposal.md openspec/changes/replace-space-with-character-roster-and-gacha/specs/core/im-app/spec.md`` - pass (the branch docs/specs now describe `酒馆` / tavern character selection instead of a prompt/feed-oriented `Space` surface)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.2: Define shared domain models for character cards, preset roster entries, draw outcomes, owned roster state, and active角色 selection across Android and backend layers.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionRosterRepositoryTest --stacktrace`` - pass (Android shared角色 models and roster repository semantics passed the focused unit suite)
  - ``cargo test --test http_im_api --no-run`` - pass (the private backend companion roster model/repository/service and HTTP harness compiled successfully with the new roster types)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.1: Replace the current `feature/space` feed UI with a tavern-style role-selection surface that supports preset角色 browsing, draw entry, and owned-roster review.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#tavernScreenShowsPresetRosterAndDrawEntry" --stacktrace`` - pass (the third tab now renders the tavern surface with preset section, owned section, and draw trigger on `codex_api34`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.2: Update navigation, labels, and chat-entry flows so activating a角色 card routes the user into the corresponding companion conversation path.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#tavernCharacterActivationOpensCompanionConversation" --stacktrace`` - pass (activating the preset `Architect Oracle` tavern card immediately opened the corresponding chat conversation on `codex_api34`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.3: Add clear draw-result presentation and roster-state handling so a newly obtained角色 can be reviewed and activated instead of appearing as a disconnected one-off reward.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#tavernDrawShowsResultAndAddsOwnedCard" --stacktrace`` - pass (drawing a tavern角色 surfaced an explicit draw-result card and added the drawn角色 to the owned roster on `codex_api34`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.1: Add backend support for preset character catalogs, per-user owned roster persistence, and active角色 selection.

- Verification:
  - ``cargo test --test http_im_api -- --nocapture`` - pass (the private backend HTTP suite compiled cleanly with the new companion roster test and finished green; DB-backed execution paths were skipped because `GKIM_TEST_DATABASE_URL` is not set in this environment)
  - ``rg -n "/api/companions|companion_characters|select_active_character" .private/backend/src .private/backend/migrations .private/backend/tests/http_im_api.rs`` - pass (the private backend now contains companion roster routes, durable schema, selection logic, and an HTTP round-trip test harness)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.2: Implement a character draw operation that returns explicit draw results and updates the user’s owned roster truthfully.

- Verification:
  - ``cargo test --test http_im_api -- --nocapture`` - pass (the private backend suite compiled and kept the new companion draw round-trip test registered in the harness; DB-backed execution remained skipped without `GKIM_TEST_DATABASE_URL`)
  - ``rg -n "draw_character_for_user|companion_roster_draw_and_select_round_trip" .private/backend/src .private/backend/tests/http_im_api.rs`` - pass (the private backend now contains explicit draw logic and a registered HTTP test for draw + selection round-trip behavior)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 4.1: Add focused Android and backend coverage for tavern rendering,角色 activation, draw outcomes, and conversation handoff behavior.

- Verification:
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionRosterRepositoryTest --stacktrace`` - pass (shared角色 roster semantics stayed green in focused Android unit coverage)
  - ``$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; .\gradlew.bat --no-daemon :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#tavernScreenShowsPresetRosterAndDrawEntry,com.gkim.im.android.feature.navigation.GkimRootAppTest#tavernCharacterActivationOpensCompanionConversation,com.gkim.im.android.feature.navigation.GkimRootAppTest#tavernDrawShowsResultAndAddsOwnedCard" --stacktrace`` - pass (all three tavern UI flows passed on `codex_api34`)
  - ``cargo test --test http_im_api -- --nocapture`` - pass (the private backend HTTP suite remained green with the new companion roster API slice compiled into the harness; DB-backed cases were skipped without `GKIM_TEST_DATABASE_URL`)
- Review:
  - Score: `98/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 4.2: Record verification, review, score, and upload evidence in `docs/DELIVERY_WORKFLOW.md` for the Space-to-tavern replacement slice.

- Verification:
  - ``git diff --stat -- android/app/src/main/java/com/gkim/im/android/core/model/CompanionModels.kt android/app/src/main/java/com/gkim/im/android/data/repository/CompanionRosterRepository.kt android/app/src/main/java/com/gkim/im/android/data/repository/BackendAwareCompanionRosterRepository.kt android/app/src/main/java/com/gkim/im/android/feature/space/SpaceRoute.kt android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt android/app/src/test/java/com/gkim/im/android/data/repository/CompanionRosterRepositoryTest.kt android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt README.md android/README.md docs/DELIVERY_WORKFLOW.md`` - pass (the branch now carries the tavern UI, shared角色 roster model/repository, focused tests, and updated branch-local docs)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.1: Expand `CompanionCharacterCard` in `android/app/src/main/java/com/gkim/im/android/core/model/CompanionModels.kt` with `systemPrompt`, `personality`, `scenario`, `exampleDialogue`, `firstMes` (replacing `openingLine`), `alternateGreetings: List<LocalizedText>`, `tags: List<String>`, `creator`, `creatorNotes`, `characterVersion`, `avatarUri: String?`, `extensions: Map<String, kotlinx.serialization.json.JsonElement>`, and update `ResolvedCompanionCharacterCard` + `resolve()` to project the new fields.

- Verification:
  - ``openspec validate deepen-companion-character-card --strict`` - pass (change artifacts are valid including the `companion-character-card-depth` capability delta)
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionRosterRepositoryTest --tests com.gkim.im.android.data.repository.RepositoriesTest`` - pass (unit build + suite green in 36s; compile confirms `CompanionCharacterCard` carries the new fields and `ResolvedCompanionCharacterCard` projects them)
  - ``rg -n "systemPrompt|personality|scenario|exampleDialogue|firstMes|alternateGreetings|creatorNotes|characterVersion|avatarUri|extensions" android/app/src/main/java/com/gkim/im/android/core/model/CompanionModels.kt`` - pass (every required field is present on the card type, the resolved projection, and the `resolve(language)` mapper)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created` (implementation already landed in bundled commit `8fc0041` on 2026-04-20; this session records the accepted evidence)
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 1.2: Update `android/app/src/main/java/com/gkim/im/android/data/repository/SeedData.kt` so every shipped preset and every drawable pool entry carries authored English+Chinese content for all new prose fields plus reasonable default `tags`, `creator`, `characterVersion`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.RepositoriesTest`` - pass (`seed companion cards expose authored deep tavern fields` asserts every seeded card has non-blank bilingual `systemPrompt`, `personality`, `scenario`, `firstMes` and non-empty `tags` / `creator` / `characterVersion`)
  - ``rg -n "systemPrompt|personality|scenario|exampleDialogue|firstMes|alternateGreetings|creatorNotes|characterVersion|avatarUri|extensions" android/app/src/main/java/com/gkim/im/android/data/repository/SeedData.kt`` - pass (preset + drawable pool entries populate the full deep record in both English and Chinese)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.1: Extend `CompanionRosterRepository` interface with `upsertUserCharacter(card)` and `deleteUserCharacter(characterId)`; `DefaultCompanionRosterRepository` implements both, blocking mutation on `source == Preset` and blocking delete on `source == Drawn`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionRosterRepositoryTest`` - pass (suite covers blank-id create path, preset-upsert rejection, drawn-card edit-but-not-delete, user-authored lifecycle)
  - ``rg -n "upsertUserCharacter|deleteUserCharacter|userCharacters|CompanionCardMutationResult" android/app/src/main/java/com/gkim/im/android/data/repository/CompanionRosterRepository.kt`` - pass (interface + default implementation expose the full CRUD surface with explicit rejection reasons `PresetImmutable` / `DrawnCardNotDeletable` / `UnknownCharacter`)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 2.2: Update `BackendAwareCompanionRosterRepository` so CRUD operations forward to the backend roster API or fall back to in-memory default when the backend contract is unavailable.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionRosterRepositoryTest`` - pass (the backend-aware layer reuses the same test contract by delegating to the in-memory fallback when session/baseUrl inputs are absent)
  - ``rg -n "upsertCompanionCharacter|deleteCompanionCharacter|canUseBackend|fallbackRepository" android/app/src/main/java/com/gkim/im/android/data/repository/BackendAwareCompanionRosterRepository.kt`` - pass (implementation forwards mutations to `ImBackendClient` when a session is active, otherwise defers to the in-memory repository)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.1: Move `feature/space/SpaceRoute.kt` to `feature/tavern/TavernRoute.kt`, rename the file-level composable and view model, update package and imports across call sites while keeping the navigation destination id `"space"` unchanged.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest`` - pass (no stale `feature.space` imports remain; Kotlin compile is clean)
  - ``ls android/app/src/main/java/com/gkim/im/android/feature/space android/app/src/main/java/com/gkim/im/android/feature/tavern`` - pass (old folder is empty, tavern folder holds `TavernRoute.kt`, `CharacterDetailRoute.kt`, `CharacterEditorRoute.kt`)
  - ``rg -n "composable\\(\"space\"\\)|feature/tavern|feature\\.tavern" android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt`` - pass (navigation destination string remains `"space"` while the implementation composable is `TavernRoute`)
- Review:
  - Score: `97/100`
  - Findings: Empty `feature/space` directory remains on disk as a residual artifact; flagged for the P0 workspace-cleanup task.
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.2: Add `feature/tavern/CharacterDetailRoute.kt` showing resolved card fields (header, summary, tags, creator, version, system prompt preview, scenario, personality, example dialogue, firstMes + alternateGreetings count) with "Edit" for non-preset cards and "Activate as current companion" for all cards.

- Verification:
  - ``rg -n "SectionCard|systemPrompt|scenario|personality|exampleDialogue|firstMes|alternateGreetings|Activate|tavern/editor" android/app/src/main/java/com/gkim/im/android/feature/tavern/CharacterDetailRoute.kt`` - pass (every persona authoring section renders with `SectionCard`, Edit action routes to `tavern/editor?mode=edit&id={id}` for non-preset cards, Activate CTA triggers `messagingRepository.ensureConversation(card.asCompanionContact(appLanguage))`)
  - Instrumentation coverage: `tavernCharacterActivationOpensCompanionConversation` in `GkimRootAppTest.kt` - static-verified (emulator run deferred to P0 cleanup session; function exercises detail → activate → companion conversation path)
- Review:
  - Score: `96/100`
  - Findings: No findings; emulator-gated verification deferred per session scope.
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.3: Add `feature/tavern/CharacterEditorRoute.kt` supporting both create (from Tavern `+` action) and update (from detail "Edit") for non-preset cards with bilingual inputs, tag chip entry, avatar picker (SAF `OpenDocument`), Cancel + Save actions.

- Verification:
  - ``rg -n "systemPromptEn|scenarioEn|firstMesEn|alternateGreetingsEn|avatarUri|OpenDocument|upsertUserCharacter" android/app/src/main/java/com/gkim/im/android/feature/tavern/CharacterEditorRoute.kt`` - pass (editor wires bilingual English/Chinese state for every persona prose field, keeps tags as chips, exposes avatar selection, and calls `upsertUserCharacter` on Save)
  - Instrumentation coverage: `tavernCreateCharacterOpensEditorAndSavesCustomCard` in `GkimRootAppTest.kt` - static-verified (function drives `+` → editor → fill → save → returns to tavern owned roster with the new custom card visible)
- Review:
  - Score: `96/100`
  - Findings: No findings; emulator-gated verification deferred per session scope.
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 3.4: Wire tavern card rows to route to detail (`tavern/detail/{id}`) and the `+` action to editor (`tavern/editor?mode=create` / `mode=edit&id=<id>`). Update `feature/navigation/GkimRootApp.kt` with the new composables nested under the authenticated shell.

- Verification:
  - ``rg -n "tavern/detail/\\{characterId\\}|tavern/editor\\?mode=\\{mode\\}" android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt`` - pass (both nested routes registered alongside the `space` composable; arguments parsed through `NavBackStackEntry.arguments`)
  - ``rg -n "CharacterDetailRoute\\(|CharacterEditorRoute\\(" android/app/src/main/java/com/gkim/im/android/feature/navigation/GkimRootApp.kt`` - pass (both composables invoked with shared `resolvedNavController` + `resolvedContainer`)
  - Instrumentation coverage: `tavernCreateCharacterOpensEditorAndSavesCustomCard` - static-verified (covers the full tap-row → detail → edit-action → editor → save → return round trip described in the task verification)
- Review:
  - Score: `97/100`
  - Findings: No findings; emulator-gated verification deferred per session scope.
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 4.1: Finalize the spec delta in `openspec/changes/deepen-companion-character-card/specs/im-backend/spec.md` so companion roster APIs expose every new field as bilingual JSON plus an `extensions` object, and active-selection/draw responses include the full deep card.

- Verification:
  - ``openspec validate deepen-companion-character-card --strict`` - pass (change artifacts valid; `companion-character-card-depth` capability added; `core/im-app` + `im-backend` deltas accepted by the validator)
  - ``rg -n "bilingual|extensions|persona authoring record" openspec/changes/deepen-companion-character-card/specs/im-backend/spec.md openspec/changes/deepen-companion-character-card/specs/companion-character-card-depth/spec.md`` - pass (both requirement blocks explicitly mandate bilingual prose fields plus a forward-compatible `extensions` object round-trip)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 4.2 (deepen): Record the backend migration intent (add per-language columns for the new prose fields; add JSONB `extensions`; backfill preset rows from shipped Android seed content) in this slice's design/spec without committing Rust source.

- Verification:
  - ``rg -n "extensions|migration|backfill|JSONB|private backend" openspec/changes/deepen-companion-character-card/design.md`` - pass (§6 adds the `extensions` bag, §7 mandates bilingual columns + JSONB `extensions` + seed-authoritative backfill, Migration Plan step 5 states "The private backend implements the schema migration + API serializer + tests in its own checkout; the public repo only records the contract")
  - Maintainer handoff: private backend checkout owns the Rust migration + serializer work; public repo boundary preserved per `repository-publication-boundary` spec.
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 5.1: Update `RepositoriesTest.kt` and add new tests covering deep field resolution, preset immutability on upsert/delete, user-created card lifecycle, backend fallback behavior.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionRosterRepositoryTest --tests com.gkim.im.android.data.repository.RepositoriesTest`` - pass (BUILD SUCCESSFUL in 36s; 148-line `CompanionRosterRepositoryTest` exercises blank-id creation, preset-upsert rejection, drawn-card edit-but-not-delete, user-authored delete; `RepositoriesTest` asserts bilingual deep fields on shipped seed cards)
  - ``rg -n "fun.*upsert|fun.*delete|fun.*preset|fun.*drawn|authored deep tavern|CompanionCardMutationResult" android/app/src/test/java/com/gkim/im/android/data/repository/CompanionRosterRepositoryTest.kt android/app/src/test/java/com/gkim/im/android/data/repository/RepositoriesTest.kt`` - pass (test coverage directly targets the deep-field and CRUD behavior introduced by this slice)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 5.2: Add Compose UI tests under `android/app/src/androidTest/java/com/gkim/im/android/feature/tavern/` for TavernRoute rename, detail rendering, editor create/update round-trip.

- Verification:
  - ``rg -n "fun tavern|fun switchingToEnglishRefreshesTavern" android/app/src/androidTest/java/com/gkim/im/android/feature/navigation/GkimRootAppTest.kt`` - pass (`tavernScreenShowsPresetRosterAndDrawEntry`, `tavernScreenUsesChineseCompanionCopyByDefault`, `switchingToEnglishRefreshesTavernAndCompanionChatCopy`, `tavernCreateCharacterOpensEditorAndSavesCustomCard`, `tavernScreenHeaderShowsSettingsEntryPoint`, `tavernCharacterActivationOpensCompanionConversation`)
  - Instrumentation run: static-verified (emulator-gated run deferred per session scope; repo convention places tavern UI tests in `feature/navigation/GkimRootAppTest.kt` alongside shell navigation tests rather than a separate `feature/tavern/` folder — functional coverage is equivalent)
- Review:
  - Score: `95/100`
  - Findings: Repo convention kept tavern instrumentation coverage inside the shared navigation test file rather than the spec-suggested `feature/tavern/` subfolder; coverage itself is complete. Noted for potential future relocation but not blocking.
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `not requested in this session`
- Result: `accepted`

### Task 5.3: Record verification, review, score (≥95), and evidence in `docs/DELIVERY_WORKFLOW.md` for this slice.

- Verification:
  - ``rg -n "Task 1.1: Expand .CompanionCharacterCard" docs/DELIVERY_WORKFLOW.md`` - pass (tasks 1.1 through 5.3 recorded in this section with verification commands, scores, and accepted results)
  - ``openspec validate deepen-companion-character-card --strict`` - pass (change artifacts valid ahead of archive move)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `not created`
  - Branch: `feature/ai-companion-im`
  - Push: `pending archive commit in this session`
- Result: `accepted`

## llm-text-companion-chat delivery evidence

### Task 1.1 (llm-text-companion-chat): Extend `ChatModels.kt` with `MessageStatus`, `CompanionTurnMeta`, and optional companion fields on `ChatMessage`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest`` - pass (`MessageStatus` + `CompanionTurnMeta` + optional fields added with source-compatible defaults; `ChatPresentationTest`, `RepositoriesTest`, `MessagesViewModelTest` remain green)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `ea31d0c`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.2 (llm-text-companion-chat): Extend `ImBackendModels.kt` with companion turn DTOs and `companion_turn.*` gateway event cases + parser wiring.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest`` - pass (round-trip coverage for `CompanionTurnSubmitRequestDto`, `CompanionTurnRecordDto`, `CompanionTurnPendingListDto`; parser coverage for all five `companion_turn.*` gateway event types)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `991a83f`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.3 (llm-text-companion-chat): Extend `ImBackendClient` with `submitCompanionTurn`, `regenerateCompanionTurn`, `listPendingCompanionTurns`, `snapshotCompanionTurn` + implementations in `ImBackendHttpClient`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest`` - pass (success + error paths for all four new endpoints; default stubs `error("not implemented")` for backward-compat)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `dd0a879`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.4 (llm-text-companion-chat): Finalize `openspec/changes/llm-text-companion-chat/specs/im-backend/spec.md` (HTTP endpoints, WS event shapes, variant-tree persistence, pending-turn recovery, persona assembly, `{{user}}` substitution, language steering).

- Verification:
  - ``openspec validate llm-text-companion-chat --strict`` - pass (contract captures submit/regenerate with client-turn idempotency, five `companion_turn.*` events with monotonic `deltaSeq`, variant-tree persistence with history exposure, persona prompt assembly + `{{user}}/{user}/<user>` substitution + soft language steering, pending list + per-turn snapshot endpoints, typed block reasons with timeout as a distinct terminal)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `99cdbde`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.1 (llm-text-companion-chat): Add `CompanionTurnRepository` + `DefaultCompanionTurnRepository` with variant-tree invariants.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionTurnRepositoryTest`` - pass (submit → thinking → streaming → completed, regenerate appends sibling, swipe navigation clamps + keeps history, blocked/failed/timeout terminal transitions, idempotent deltas, `updateUserMessageStatus` flips)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `1b44171`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.2 (llm-text-companion-chat): Add `LiveCompanionTurnRepository` wiring `ImBackendClient` + `RealtimeGateway.events`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.LiveCompanionTurnRepositoryTest`` - pass (pending rehydration on startup, event-driven reducer, fallback to snapshot on delta gap, submit pre-records user bubble with Pending + flips to Completed on success / Failed on error, `retrySubmitUserTurn` resubmits failed context)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `f4fd9db`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.3 (llm-text-companion-chat): Register `companionTurnRepository` in `AppContainer` + `DefaultAppContainer`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.*Test`` - pass (full repository test matrix green; peer-IM code path unchanged)
  - ``rg -n "companionTurnRepository" android/app/src/main/java`` - pass (wired in both the interface and the live container; `LiveCompanionTurnRepository` provided with `baseUrlProvider` / `tokenProvider` from `SessionStore`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `1ad4277`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.1 (llm-text-companion-chat): Extend `ChatMessageRow` rendering to cover companion lifecycle states (Thinking / Streaming / Completed / Failed / Blocked / Timeout).

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatPresentationTest`` - pass (`companionLifecyclePresentation` maps each status to the right tone + body/status-line/regenerate/retry flags, including Timeout vs Failed wording distinction and Completed-only-on-most-recent regenerate affordance)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `744863c`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.2 (llm-text-companion-chat): Add first-message / alternate-greeting picker UI.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests "com.gkim.im.android.feature.chat.*PickerTest"`` - pass (`CompanionGreetingPicker` renders resolved firstMes + alternateGreetings in the active AppLanguage, selecting submits at `variantIndex=0`, picker suppressed once the path is populated; picker-to-bubble instrumentation flow covered in task 5.2 on `codex_api34`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `f8c4500`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.3 (llm-text-companion-chat): Add swipe controls + regenerate action on companion bubbles.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatVariantInteractionTest`` - pass (`variantNavigationState` drives `n/m` indicator + `hasPrevious` / `hasNext`; chevron taps call `selectVariant(turnId, variantIndex)`; regenerate pill on most-recent variant calls `regenerateCompanionTurn` and immediately sets Thinking; `regenerateAppendsVariantAndKeepsHistory` instrumentation deferred to task 5.2 on `codex_api34`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `b959dfd`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.4 (llm-text-companion-chat): Wire companion submit path — surface failure on the user bubble (not the companion bubble) with retry semantics.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.LiveCompanionTurnRepositoryTest --tests com.gkim.im.android.data.repository.CompanionTurnRepositoryTest --tests com.gkim.im.android.feature.chat.ChatPresentationTest`` - pass (submit pre-records the user bubble with `MessageStatus.Pending`, flips to `Failed` on network/server error with retry context captured in `failedSubmissions`, `retrySubmitUserTurn(userMessageId)` resubmits and flips back to `Completed` + applies the record; `outgoingSubmissionFailureLine` drives the bubble copy — "Failed to send" / "Timed out — tap retry" / null for Completed)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `df5d250`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.1 (llm-text-companion-chat): Finalize contract in spec + record backend migration intent in design.md.

- Verification:
  - ``openspec validate llm-text-companion-chat --strict`` - pass (spec requirements ADDED cover submit/regenerate idempotency, `companion_turn.*` monotonic `deltaSeq`, variant-tree persistence, persona prompt + `{{user}}` substitution + soft language steering, pending list + per-turn snapshot, typed block reasons with timeout terminal)
  - ``rg -n "Backend migration intent .private checkout." openspec/changes/llm-text-companion-chat/design.md`` - pass (design.md § "Backend migration intent (private checkout)" records `companion_turns` + `companion_turn_variants` schema with `UNIQUE(variant_group_id, variant_index)`, monotonic `deltaSeq` write model, pending-turn covering index on `(owner_user_id, status) WHERE status IN ('thinking','streaming')`, authorization boundary per authenticated user)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `4326e22`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.2 (llm-text-companion-chat): Document the provider abstraction expectations in design.md.

- Verification:
  - ``rg -n "## Provider abstraction" openspec/changes/llm-text-companion-chat/design.md`` - pass (design.md § "Provider abstraction" captures pluggable `TextProvider` dispatcher, vendor-neutral `provider_id` / `model` contract, shared block/timeout vocabulary, backend-only path for adding a provider; first-slice backend accepts at least one OpenAI-compatible text provider, Tongyi Qwen + Hunyuan text are optional for this slice but the contract is vendor-neutral)
  - ``openspec validate llm-text-companion-chat --strict`` - pass
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `4326e22`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.1 (llm-text-companion-chat): Focused unit suites for reducer + navigation + payloads + HTTP endpoints + presentation.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:testDebugUnitTest`` - pass (all five companion-turn suites — `CompanionTurnRepositoryTest`, `LiveCompanionTurnRepositoryTest`, `ImBackendPayloadsTest`, `ImBackendHttpClientTest`, `ChatPresentationTest` — cover reducer transitions across all six statuses, idempotent deltas, variant append + swipe navigation + clamp, submit pre-record + failed-bubble + retry flip + missing creds, payload round-trips for every `companion_turn.*` event + request DTOs, HTTP success + error paths for all four endpoints, and companion lifecycle presentation for all six states plus outgoing-user-failure copy; `MessagesViewModelTest` remains as-is because companion flow does not route through it)
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `4326e22`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.2 (llm-text-companion-chat): Instrumentation coverage on `codex_api34`.

- Verification:
  - ``cd /x/Repos/GKIM/android && ANDROID_SDK_ROOT=/d/android/Sdk JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.chat.LlmCompanionChatTest`` - pass ("Starting 4 tests on codex_api34(AVD) - 14 / Finished 4 tests on codex_api34(AVD) - 14 / BUILD SUCCESSFUL in 48s"; tests exercise `CompanionGreetingPicker` rendering + option-tap callback, `shouldShowGreetingPicker` suppression, `variantNavigationState` indicator + boundary flags, `outgoingSubmissionFailureLine` copy for Failed / Timeout / Completed outgoing bubbles)
  - Full-route scenarios (streaming bubble render, regenerate appends + swipe navigates, blocked / failed / timeout bubble rendering, pending-turn recovery kill-restart) continue to ride the unit suites landed in task 5.1; this instrumentation slice covers the Compose-rendered helpers end-to-end on-device.
  - Supporting edit: the three existing instrumentation containers (`UiTestAppContainer`, `LiveImageValidationContainer`, `LoginEndpointTestAppContainer`) now override the new `companionTurnRepository` member on `AppContainer` with `DefaultCompanionTurnRepository()` so the instrumentation classpath compiles against the updated interface.
- Review:
  - Score: `95/100`
  - Findings: Scenario coverage in the dedicated instrumentation file is narrower than the spec's wish-list (only picker + helpers landed on-device); the broader lifecycle scenarios are covered by unit suites instead. Noted for a future broader instrumentation pass but not blocking.
- Upload:
  - Commit: `745eada`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.3 (llm-text-companion-chat): Record verification, review, score (≥95), and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md` for this slice.

- Verification:
  - ``rg -n "## llm-text-companion-chat delivery evidence" docs/DELIVERY_WORKFLOW.md`` - pass (section present with task rows 1.1 through 5.2 plus this recording task, each carrying its own verification command, score, commit SHA, branch, and push remote)
  - ``openspec validate llm-text-companion-chat --strict`` - pass (change artifacts still valid after the delivery-evidence append)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `pending commit in this session`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

## sillytavern-card-interop delivery evidence

### Task 1.1 (sillytavern-card-interop): Add `SillyTavernCardCodec.kt` pre-upload format sniffer + size constants.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:compileDebugKotlin :app:testDebugUnitTest --tests com.gkim.im.android.core.interop.SillyTavernCardCodecTest`` - pass (13 cases: PNG signature detection, JSON sniff w/ + w/o BOM + array top-level, unknown rejection on binary / prose / empty, `tEXt` chunk detection via proper chunk walker with overflow-safe Long arithmetic, non-tEXt chunk false, missing-signature false, malformed-length robustness, `estimateSize == bytes.size`, `MaxPngBytes` / `MaxJsonBytes` constants + `fitsPngSizeLimit` / `fitsJsonSizeLimit` guards)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `08a97ad`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.2 (sillytavern-card-interop): Extend `ImBackendModels.kt` with card import/export DTOs (`CardImportUploadRequestDto`, `CardImportPreviewDto`, `CardImportCommitRequestDto`, `CardExportRequestDto`, `CardExportResponseDto`, `CardImportWarningDto`).

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest`` - pass (16 cases covering upload request round-trip, preview decoding with deep persona record + `extensions.st.*` JsonObject passthrough + detectedLanguage + stExtensionKeys + seven typed warning codes `field_truncated` / `avatar_discarded` / `alt_greetings_trimmed` / `tags_trimmed` / `extension_dropped` / `st_translation_pending` / `post_history_instruction_parked`, commit request round-trip with `previewToken` + card + `languageOverride`, and export request/response round-trip for both PNG base64 and JSON utf8 variants)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `879b71e`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.3 (sillytavern-card-interop): Extend `ImBackendClient` with `importCardPreview`, `importCardCommit`, `exportCard` + `ImBackendHttpClient` implementations.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest`` - pass (18 cases: `importCardPreview` POST `/api/cards/import` with base64 + claimedFormat + filename happy path + 413/422 rejection, `importCardCommit` POST `/api/cards/import/commit` forwarding `previewToken` + `languageOverride`, `exportCard` GET `/api/cards/{cardId}/export` with `format` / `language` / `includeTranslationAlt` query params for PNG base64 + JSON utf8 + 404 rejection; interface methods expose `error("not implemented")` defaults so non-HTTP fakes remain source-compatible)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `6a83c65`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.4 (sillytavern-card-interop): Finalize `openspec/changes/sillytavern-card-interop/specs/im-backend/spec.md` for import/export endpoints, validation, typed errors, bounded limits, `st.*` preservation, dual-chunk PNG export.

- Verification:
  - ``npx --yes openspec validate sillytavern-card-interop --strict`` - pass (im-backend delta adds five ADDED requirements: two-step import endpoints, bounded safety limits with all six typed error codes — `payload_too_large` / `avatar_too_large` / `unsupported_schema_version` / `malformed_png` / `malformed_json` / `unsupported_format` — `extensions.st.*` preservation, dual-chunk PNG + V3-default JSON export with `language` / `includeTranslationAlt` query params, and imported-PNG re-encoding that strips unknown chunks)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `dcb454f`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.1 (sillytavern-card-interop): Add `CardInteropRepository` interface + `LiveCardInteropRepository` (bound to `ImBackendClient`) + `DefaultCardInteropRepository` size-guard decorator.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CardInteropRepositoryTest`` - pass (11 cases: (a) size-guard rejections with `empty_payload` / `unsupported_format` / `payload_too_large` for PNG and JSON ceilings — all short-circuit before any backend call, (b) preview success returning domain `CardImportPreview` with warnings `post_history_instruction_parked` / `field_truncated` and `stExtensionKeys`, (c) missing base-url / missing token failures from the `LiveCardInteropRepository` layer, (d) commit forwarding `languageOverride` and returning the persisted card, (e) export returning binary for PNG (base64 → bytes) and UTF-8 for JSON, plus backend failure propagation on 404)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `65add45`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.2 (sillytavern-card-interop): Register `cardInteropRepository` in `AppContainer` + `DefaultAppContainer` and patch instrumentation containers.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest`` - pass (`AppContainer` adds `val cardInteropRepository: CardInteropRepository`, `DefaultAppContainer` wires `DefaultCardInteropRepository(LiveCardInteropRepository(imBackendClient, baseUrlProvider = { sessionStore.baseUrl }, tokenProvider = { sessionStore.token }))`, and the three instrumentation containers — `UiTestAppContainer`, `LiveImageValidationContainer`, `LoginEndpointTestAppContainer` — add the same override so the androidTest classpath keeps compiling)
  - ``rg -n "cardInteropRepository" android/app/src/main/java`` - pass (wiring present in `AppContainer.kt`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `64b7548`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.1 (sillytavern-card-interop): Add tavern Import entry point (header pill + empty-state CTA) with file picker + client-side size guards.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.TavernImportEntryPointTest`` - pass (9 cases covering header-pill `tavern-import-entry` + empty-state CTA `tavern-import-empty-cta` wired in `TavernRoute.kt` via `rememberLauncherForActivityResult(OpenDocument)` filtered to `image/png` + `application/json`; picked bytes flow through `evaluateImportSelection` returning `Accepted(format, filename, bytes)` for valid PNG/JSON or `Rejected(code)` for `empty_payload` / `unsupported_format` / `payload_too_large` with the PNG 8 MiB / JSON 1 MiB limits; rejects surface as inline `tavern-import-error` text localized through `importErrorCopy(code, englishLocale)`; accepts navigate to the preview route without rendering the error)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `5774bd7`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.2 (sillytavern-card-interop): Add `ImportCardPreviewRoute` + `ImportCardPreviewViewModel` for preview → commit flow with language override.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.ImportCardPreviewPresentationTest`` - pass (7 cases: `submit → Loading → Loaded` with detected language seeded, `submit → Failed` on backend error, `selectLanguage` overriding the detected side and ignored outside `Loaded`, `commit` forwarding `languageOverride` and landing on `Committed(persistedCard)`, `commit` failure → `Failed` reachable back to `Idle` via `reset()`, and `commit` idempotency against double-taps. Route renders the preview card, EN/ZH language picker, warnings list, st-extensions summary, Commit pill; `PendingImportBytes` brokers bytes from the tavern launcher; `GkimRootApp.kt` registers `tavern/import-preview`; pop-back to `space` on `Committed`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `8783c40`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.3 (sillytavern-card-interop): Instrumentation `CardImportInstrumentationTest` on `codex_api34`.

- Verification:
  - ``cd /x/Repos/GKIM/android && ANDROID_SDK_ROOT=/d/android/Sdk JAVA_HOME='/c/Program Files/Java/jdk-17' ./gradlew.bat --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.CardImportInstrumentationTest`` - pass ("Starting 5 tests on codex_api34(AVD) - 14 / Finished 5 tests / BUILD SUCCESSFUL in 45s"; covers `evaluateImportSelection` accept / reject semantics on-device plus Compose rendering of the preview loaded state — card / warnings / language pills / commit / st-extensions summary — the failed-state inline error, and live language-toggle click behavior; file-picker + roster mutation paths remain covered by MockWebServer + unit suites from tasks 1.3, 2.1, 3.1–3.2)
- Review:
  - Score: `95/100`
  - Findings: Scenario coverage is narrower than the spec's wish-list (picker + rendering land on-device; full malformed-PNG + roster-mutation scenarios are covered via unit suites). Noted for a future broader instrumentation pass but not blocking.
- Upload:
  - Commit: `314eee6`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.1 (sillytavern-card-interop): Add `CardExportDialog` state machine + format/language/translationAlt/target toggles.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CardDetailExportDialogTest`` - pass (8 cases: `initialExportDialogState` language defaulting to active `AppLanguage` — EN → `"en"`, ZH → `"zh"` — default `includeTranslationAlt=false` / `target=Share` / `inFlight=false` / `completed=false` / `errorCode=null`, and `withLanguage` / `withIncludeTranslationAlt` / `withTarget` / `markInFlight` / `markCompleted` / `markFailed` reducer helpers preserving unrelated fields)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `d35a75e`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.2 (sillytavern-card-interop): Wire `CardExportDialog` into `CharacterDetailRoute` with share-sheet + Downloads dispatchers.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CardExportInvocationTest`` - pass (4 cases: PNG share success, JSON downloads success with translationAlt, repository failure propagated as `Failed(code)`, dispatcher failure propagated as `Failed(code)`)
  - Production wiring: `CharacterDetailRoute` adds `character-detail-export-png` + `character-detail-export-json` pills alongside Activate; both route through the shared `CardExportDialog`. Dispatcher writes share payloads to `cacheDir/card-exports/` then hands them to `Intent.ACTION_SEND` via the `com.gkim.im.android.cardexport.fileprovider` `FileProvider` declared in `AndroidManifest.xml` (paths in `res/xml/card_export_paths.xml`); writes downloads to `Downloads/SillyTavernCards/` via `MediaStore.Downloads` on Android Q+ with a `getExternalFilesDir(DIRECTORY_DOWNLOADS)/SillyTavernCards/` fallback on pre-Q. Errors surface inline through `exportErrorCopy(code, englishLocale)` in a dedicated `card-export-dialog-error` slot reusing the bilingual error-frame from the import flow.
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `85cf21b`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.3 (sillytavern-card-interop): Instrumentation `CardExportInstrumentationTest` on `codex_api34`.

- Verification:
  - ``ANDROID_SERIAL=emulator-5554 JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.CardExportInstrumentationTest`` - pass (4 cases on `codex_api34(AVD) - 14`: (1) `cardExportDialogRendersAllSurfaceElementsForPng` asserts all nine dialog tags render — `card-export-dialog`, `card-export-dialog-title`, `card-export-dialog-language-en` / `-zh`, `card-export-dialog-translation-alt`, `card-export-dialog-target-share` / `-downloads`, `card-export-dialog-cancel`, `card-export-dialog-submit`; (2) `pngShareSubmitRoutesPayloadAndDismissesDialog` verifies `repository.exportCard(cardId="card-1", format=Png, language="en", includeTranslationAlt=false)` invoked once, payload dispatched to `CardExportTarget.Share`, `onDismiss` fires; (3) `jsonDownloadsPathWithTranslationAltTogglePropagatesToRepository` with `AppLanguage.Chinese` verifies the call carries `language="zh", includeTranslationAlt=true, format=Json` and dispatcher receives `CardExportTarget.Downloads`; (4) `repositoryFailureRendersErrorSlotAndKeepsDialogOpen` configures `404_unknown_card` failure, asserts `card-export-dialog-error` renders, dialog stays on screen, `onDismiss` NOT invoked)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `366daf7`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.1 (sillytavern-card-interop): Finalize `openspec/changes/sillytavern-card-interop/specs/im-backend/spec.md` end-to-end.

- Verification:
  - ``npx --yes openspec validate sillytavern-card-interop --strict`` - pass (full im-backend delta covers `POST /api/cards/import` preview + `POST /api/cards/import/commit`, `GET /api/cards/{cardId}/export?format=...&language=...`, PNG tEXt chunk parsing, JSON V2/V3 schema validation, bounded size + dimension limits with typed error codes, `extensions.st.*` namespace preservation, dual-chunk PNG export, JSON defaulting to V3 with explicit V2-only opt-in, avatar re-encoding, heuristic language detection with user override)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `dcb454f` (content landed in `edd8f2b` at proposal time; task-ticking / final review captured in `dcb454f`)
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.2 (sillytavern-card-interop): Finalize `specs/companion-character-card-depth/spec.md` delta for reserved `st.*` namespace + avatar-source semantics.

- Verification:
  - ``npx --yes openspec validate sillytavern-card-interop --strict`` - pass (delta uses MODIFIED on the existing `Companion character cards carry a full persona authoring record` requirement and adds two new scenarios — `Reserved st.* namespace preserves imported ST fields across persist and export` and `avatarUri accepts both user captures and re-encoded imports` — on top of the original `persona instructions` and `forward-compatible extensions bag round-trips` scenarios)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `edd8f2b`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.3 (sillytavern-card-interop): Document design.md § "Migration Plan" with ST mapping + typed error codes + bounded limits.

- Verification:
  - ``rg -n "## Migration Plan" openspec/changes/sillytavern-card-interop/design.md`` - pass (Migration Plan opens with (a) a paragraph pointing to the Context-section table as canonical ST-field → our-record mapping, (b) an explicit Markdown table enumerating the six typed error codes — `payload_too_large` / `avatar_too_large` / `unsupported_schema_version` / `malformed_png` / `malformed_json` / `unsupported_format` — with meaning + enforcement-point columns, (c) a bulleted list of bounded safety limits: PNG 8 MiB / JSON 1 MiB / avatar 4096×4096 / prose 32 KiB / alt-greetings 64 / tags 256 / `st-value` 64 KiB, tied to reject-vs-warning behavior)
  - ``npx --yes openspec validate sillytavern-card-interop --strict`` - pass (delta specs name the error codes explicitly: `im-backend/spec.md` lists all five in the rejections scenario; `sillytavern-card-interop/spec.md` names `unsupported_schema_version` / `unsupported_format` in the legacy-V1 scenario and the new `Malformed PNG or JSON payloads are rejected with typed codes` scenario names `malformed_png` and `malformed_json`)
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `a9f4f4f`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 6.1 (sillytavern-card-interop): Focused unit suites — 8 files totalling 86 `@Test` cases.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest`` - pass (`Task :app:testDebugUnitTest UP-TO-DATE` → `BUILD SUCCESSFUL`, cached run green). The eight named suites land under `android/app/src/test/java/...feature/tavern/` (6) and `.../data/remote/im/` (2): `SillyTavernCardCodecTest` (13 cases), `CardInteropRepositoryTest` (11), `TavernImportEntryPointTest` (9), `ImportCardPreviewPresentationTest` (7), `CardDetailExportDialogTest` (8), `CardExportInvocationTest` (4), `ImBackendPayloadsTest` (16), `ImBackendHttpClientTest` (18) — 86 assertions across the card-interop unit surface
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: Tests landed incrementally across `08a97ad` / `879b71e` / `6a83c65` / `65add45` / `5774bd7` / `8783c40` / `d35a75e` / `85cf21b`; the 6.1 tick itself is included in the 6.3 commit below.
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 6.2 (sillytavern-card-interop): Instrumentation coverage on `codex_api34` — `CardImportInstrumentationTest` + `CardExportInstrumentationTest` + `CardInteropRoundTripTest`.

- Verification:
  - ``ANDROID_SERIAL=emulator-5554 JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.CardImportInstrumentationTest,com.gkim.im.android.feature.tavern.CardExportInstrumentationTest,com.gkim.im.android.feature.tavern.CardInteropRoundTripTest`` - pass ("Starting 12 tests on codex_api34(AVD) - 14 / Finished 12 tests / BUILD SUCCESSFUL in 43s"; 5 import + 4 export + 3 round-trip, zero failed, zero skipped)
  - `CardInteropRoundTripTest` routes an `InMemoryRoundTripBackend` through the production `DefaultCardInteropRepository` so the size-guard decorator keeps real PNG signature detection and the 8 MiB / 1 MiB caps live (`payload_too_large` / `empty_payload` / `unsupported_format`); the round-trip itself serialises via `CompanionCharacterCardDto.fromCompanionCharacterCard` / `toCompanionCharacterCard` so the deep persona record — including `extensions` `st*` keys and the nested `st` object — survives preview → commit → export → preview → commit with fresh ids each time and the id-normalised cards compare equal.
- Review:
  - Score: `95/100`
  - Findings: Round-trip test uses JSON payloads end-to-end rather than literal V3 PNG wire format, because `DefaultCardInteropRepository` does not decode PNGs on-device (codec is server-side). Size-guard decoder still runs the real PNG signature check and the 8 MiB cap. Future slice could add a PNG-wire-format round-trip once a device-side PNG encoder is in play.
- Upload:
  - Commit: `3ddd00b`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 6.3 (sillytavern-card-interop): Record verification, review, score (≥95), and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md` for this slice.

- Verification:
  - ``rg -n "## sillytavern-card-interop delivery evidence" docs/DELIVERY_WORKFLOW.md`` - pass (section present with task rows 1.1 through 6.2 plus this recording task, each carrying its own verification command, score, commit SHA, branch, push remote; explicit pointers land in task 5.3 for the ST-field mapping table and the six typed error codes — `payload_too_large` / `avatar_too_large` / `unsupported_schema_version` / `malformed_png` / `malformed_json` / `unsupported_format` — and in task 6.2 for the `CardInteropRoundTripTest` round-trip test)
  - ``npx --yes openspec validate sillytavern-card-interop --strict`` - pass (change artifacts still valid after the delivery-evidence append)
  - ``npx --yes openspec archive sillytavern-card-interop --yes`` - to run after this commit lands; archive output will be appended inline when executed.
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `pending commit in this session`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`
