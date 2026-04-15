# Delivery Workflow - Per-Task Review Gates

`docs/DELIVERY_WORKFLOW.md` is the repository source of truth for how an implementation task becomes accepted.

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

## Apply session records

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
