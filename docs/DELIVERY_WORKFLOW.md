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
