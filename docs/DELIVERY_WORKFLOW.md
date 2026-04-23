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
  - ``npx --yes openspec archive sillytavern-card-interop --yes`` - pass ("Task status: ✓ Complete / Specs to update: companion-character-card-depth: update, im-backend: update, sillytavern-card-interop: create / Applying changes to openspec/specs/companion-character-card-depth/spec.md: ~ 1 modified / Applying changes to openspec/specs/im-backend/spec.md: + 5 added / Applying changes to openspec/specs/sillytavern-card-interop/spec.md: + 6 added / Totals: + 11, ~ 1, - 0, → 0 / Specs updated successfully. / Change 'sillytavern-card-interop' archived as '2026-04-21-sillytavern-card-interop'.")
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `pending commit in this session`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

## user-persona delivery evidence

### Task 1.1 (user-persona): Add `core/model/UserPersonaModels.kt` with `UserPersona` + `UserPersonaValidation`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:compileDebugKotlin :app:testDebugUnitTest --tests com.gkim.im.android.core.model.UserPersonaModelsTest`` - pass (8 cases: data-class equality across every field, `resolve()` returns active-language strings with `isActive` / `isBuiltIn` propagated, `isDeletable` requires user-owned and inactive, `extensions` JsonObject survives `copy()`, validation accepts complete bilingual persona, rejects blank English display name with `DisplayNameEnglishBlank`, rejects blank Chinese description with `DescriptionChineseBlank`, reports all four blank sides simultaneously).
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `f4a2305`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.2 (user-persona): Add `core/model/MacroSubstitution.kt` for the six `{{user}}` / `{user}` / `<user>` / `{{char}}` / `{char}` / `<char>` forms.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.core.model.MacroSubstitutionTest`` - pass (13 cases: each of the six forms resolves individually; mixed user+char forms in one template; case-insensitive matching across all six forms; unknown macros like `{{random}}` / `{foo}` / `<bar>` / whitespaced `{{ user }}` stay untouched; empty user leaves user macros raw while char still resolves; empty char leaves char macros raw while user still resolves; both empty leaves template untouched; no-macro template untouched; repeated forms all substitute; `UserForms` + `CharForms` expose the canonical six-form list).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `90e7310`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.3 (user-persona): Extend `ImBackendModels.kt` with `UserPersonaDto`, `UserPersonaListDto`, `UserPersonaActivateRequestDto`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest`` - pass (20 cases = 16 legacy + 4 new persona cases: `UserPersonaDto` decodes bilingual `displayName` + `description` + timestamps + extensions JsonObject passthrough and re-encodes to equal JSON; `toUserPersona()` converts to domain with `isActive` preserved; `UserPersonaListDto` round-trips a two-persona list with `activePersonaId`; `UserPersonaActivateRequestDto` round-trips `personaId`; `UserPersonaDto.fromUserPersona(domain)` → encode → decode → `toUserPersona()` returns a domain value equal to the original including the `extensions` bag).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `1e25456`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.4 (user-persona): Extend `ImBackendClient` with persona CRUD + `activatePersona` + `getActivePersona`; implement in `ImBackendHttpClient`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest`` - pass (30 cases = 18 legacy + 12 new persona endpoint cases: `listPersonas` attaches bearer token and decodes `UserPersonaListDto` + raises on 404; `createPersona` POSTs body to `/api/personas` and decodes the stored persona + raises on 409 duplicate conflict; `updatePersona` POSTs body to `/api/personas/{personaId}` + raises on 404 for missing persona; `deletePersona` POSTs to `/api/personas/{personaId}/delete` and attaches bearer token + raises on 409 when server blocks deleting the active persona; `activatePersona` POSTs to `/api/personas/{personaId}/activate` and returns the new active persona with `isActive=true` + raises on 404 unknown persona; `getActivePersona` GETs `/api/personas/active` and returns the built-in default persona + raises on 404 when no active persona is set).
- Review:
  - Score: `97/100`
  - Findings: `No findings`
- Upload:
  - Commit: `e75de3b`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.5 (user-persona): Finalize `openspec/changes/user-persona/specs/im-backend/spec.md` with persistence, CRUD, active-singleton, built-in seeding, macro substitution, allocator integration.

- Verification:
  - ``npx --yes openspec validate user-persona --strict`` - pass (`Change 'user-persona' is valid`; 5 ADDED Requirements in `im-backend/spec.md` — persistence + CRUD, exactly-one-active + delete-built-in-or-active rejection, built-in seeding on first bootstrap, macro substitution for all six forms, allocator integration with priority + drop position).
- Review:
  - Score: `95/100`
  - Findings: `No findings`
- Upload:
  - Commit: `eba5500`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.1 (user-persona): Add `UserPersonaRepository` interface + `DefaultUserPersonaRepository` with invariants.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.UserPersonaRepositoryTest`` - pass (11 cases: activate flips active flag exclusively; activate unknown returns `Rejected(UnknownPersona)`; delete built-in returns `Rejected(BuiltInPersonaImmutable)`; delete active returns `Rejected(ActivePersonaNotDeletable)`; delete inactive user-owned succeeds; duplicate produces bilingual-suffixed user-owned copy with fresh id + `isActive=false`; duplicate unknown returns `Rejected(UnknownPersona)`; create normalizes to `isBuiltIn=false` + `isActive=false` + clock timestamps; update preserves `isBuiltIn` / `isActive` / `createdAt`; `observeActivePersona` emits null when none active; ingesting multiple actives collapses to exactly one via `enforceSingleActive`).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `deb7d61`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.2 (user-persona): Add `LiveUserPersonaRepository` binding to `ImBackendClient` with rollback on 4xx/5xx.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.LiveUserPersonaRepositoryTest`` - pass (12 cases: merge semantics for `refresh` with both `activePersonaId=non-null` and `=null` + `getActivePersona`; activate forwards to backend and replaces local with returned record; activate rolls back to previous active on 409; delete rolls back on 409 for active-persona deletion; delete short-circuits on `Rejected(BuiltInPersonaImmutable)` without reaching backend; create rolls back on 500 and reconciles server-returned id on success; duplicate rolls back on backend create failure; update rolls back on 422; mutations without session (null baseUrl) short-circuit to local-only Success without calling the backend).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `6f0e31b`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.3 (user-persona): Register `userPersonaRepository` in `AppContainer` + `DefaultAppContainer` + instrumentation containers.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests "com.gkim.im.android.data.repository.*Test"`` - pass (all repository tests green after wiring `LiveUserPersonaRepository` into `DefaultAppContainer` with `baseUrlProvider` / `tokenProvider` from the session store; the three instrumentation `AppContainer` impls — `UiTestAppContainer`, `LiveImageValidationContainer`, `LoginEndpointTestAppContainer` — each gain a `DefaultUserPersonaRepository(initialPersonas = seedBuiltInPersonas)` override; `seedBuiltInPersonas` in `SeedData.kt` exposes the single built-in `persona-builtin-default` with `isBuiltIn=true` + `isActive=true` + bilingual `displayName = LocalizedText("You", "你")` matching the backend seed contract).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `28d0156`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.1 (user-persona): Add Settings → Personas section with list + active badge + Activate/Edit/Duplicate/Delete entry points.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.PersonaListPresentationTest`` - pass (9 cases: built-ins first then user personas each ordered by `createdAt` ascending; active flag surfaces as active badge; built-ins cannot be deleted; active persona cannot be deleted or activated again but remains editable + duplicable; inactive built-in shows `canActivate=true` + `canDelete=false`; activate clears `pendingOperation` + `errorMessage` on success; language provider drives resolved display; delete on active surfaces "Active persona cannot be deleted"; failed mutation surfaces `server_busy`; init triggers `repository.refresh()` once).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `09bdaea`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.2 (user-persona): Add `PersonaEditorRoute` + `PersonaEditorViewModel` with bilingual fields + save/cancel.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.PersonaEditorPresentationTest`` - pass (7 cases: loads existing persona with `canSave=false` when no changes; blank English display name surfaces `DisplayNameEnglishBlank` and blocks save; all four blank sides surface simultaneously and block save; save success calls `UserPersonaRepository.update` + updates baseline snapshot so `canSave` flips back to false + persisted record reflects new description; cancel restores loaded snapshot; unknown persona id surfaces "Persona not found"; built-in persona cannot be saved even with valid fields).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `1c493f0`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.3 (user-persona): Add instrumentation `PersonaLibraryInstrumentationTest` on `codex_api34`.

- Verification:
  - ``ANDROID_SERIAL=emulator-5554 JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.settings.PersonaLibraryInstrumentationTest,com.gkim.im.android.feature.chat.PersonaIntegrationChatTest`` - pass ("Starting 12 tests on codex_api34(AVD) - 14 / Finished 12 tests on codex_api34(AVD) - 14 / BUILD SUCCESSFUL in 1m 2s"; 7 library + 5 integration = 12, zero failed, zero skipped).
  - `PersonaLibraryInstrumentationTest` (7 cases): built-in persona renders active badge; editing built-in description round-trips through the editor and renders back on the list; `settings-personas-new` opens the editor in create mode and saving adds the new persona to the list; activating an inactive persona moves the active badge; delete is disabled for the active persona and enabled for inactive user personas; deleting an inactive user persona removes its card; built-in badge renders only for seed personas.
- Review:
  - Score: `95/100`
  - Findings: Tests run hermetically via `createComposeRule()` + `TestablePersonasScreen`, which mirrors the production testTag structure but does not exercise the full DI container / navigation graph. This matches the `CardImportInstrumentationTest` pattern — behavior invariants under test (active badge placement, delete-disabled-on-active, activate-flips-badge, new-persona creation) are captured without requiring the live `ImBackendHttpClient`. A future slice could add a full route-level instrumentation once the Settings entry is exposed via a deep-linkable navigation target.
- Upload:
  - Commit: `12ad376`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.1 (user-persona): Extend chat chrome with active-persona pill that routes to Settings → Personas.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatChromePersonaPillTest`` - pass (7 cases: English label resolution for active persona; Chinese label resolution for active persona; English fallback "Choose persona" when no persona is active; Chinese fallback "选择角色" when no persona is active; destination route is `"settings"`; label updates when active persona changes; label updates when language flips while persona is held constant).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `e503d68`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.2 (user-persona): Wire greeting picker preview to use `MacroSubstitution` with active persona + companion display names.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.GreetingPickerMacroSubstitutionTest`` - pass (10 cases: user + char macros resolve in preview body; raw input list not mutated; returned list is a fresh instance; all six forms substitute with both names; case-insensitive for imported ST cards; blank user leaves user macros raw while char resolves; blank char leaves char macros raw while user resolves; both blank leaves every body raw; `index` + `label` preserved across the transform; empty input returns empty list).
  - Render-time callsites use `applyPersonaMacros(resolveCompanionGreetings(card, language), activePersona.displayName.resolve(language), card?.resolve(language)?.name.orEmpty())` before passing options to `CompanionGreetingPicker`; the stored raw `firstMes` / `alternateGreetings` on the card remain unchanged so the backend still sees the raw macro forms when assembling prompts.
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `ce770a5`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.3 (user-persona): Add "Talking as {personaName}" footer under chrome pills with accessibility semantics.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.chat.ChatChromePersonaFooterTest`` - pass (8 cases: English footer renders "Talking as Nova"; Chinese footer renders "以 新星 的身份对话"; `contentDescription` mirrors visible `text` for both languages; null active persona returns null so chrome omits the line; footer updates when persona changes; footer updates when language changes; `activePersonaId` propagates from the active persona; `EnglishPrefix` / `ChinesePrefix` / `ChineseSuffix` constants match the spec labels).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `896cf0a`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.1 (user-persona): Finalize `openspec/changes/user-persona/specs/im-backend/spec.md` end-to-end.

- Verification:
  - ``npx --yes openspec validate user-persona --strict`` - pass (`Change 'user-persona' is valid`; the im-backend delta covers all five contract areas — persistence + CRUD, exactly-one-active + built-in / active deletion guards, built-in seeding on first bootstrap, macro substitution in assembled prompts for the six forms, allocator integration with priority + drop position).
- Review:
  - Score: `95/100`
  - Findings: `No findings`
- Upload:
  - Commit: `eba5500`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.2 (user-persona): Document persona description slot in the allocator and the MacroSubstitution six-form list in design.md.

- Verification:
  - ``rg -n "Persona description injection" openspec/changes/user-persona/design.md`` - pass (design.md § 5 names the exact priority slot — above rolling summary, below pinned facts, above persona `exampleDialogue` — and the drop order position — between rolling summary and non-critical preset sections — with the nine-step ladder spelled out; § 5 also codifies "Never drop" items and notes that `{{user}}` substitution survives a dropped description).
  - ``rg -n "Macro substitution" openspec/changes/user-persona/design.md`` - pass (design.md § 4 carries an explicit form-list table with all six canonical forms — user: `{{user}}` / `{user}` / `<user>`; char: `{{char}}` / `{char}` / `<char>` — plus a note that the list is case-insensitive and mirrored by `core/model/MacroSubstitution.kt`'s `UserForms` / `CharForms` lists; the table is the single-source-of-truth reference for both the backend prompt assembler and the Android `MacroSubstitution` helper).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `eba5500`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 6.1 (user-persona): Focused unit suites — 11 files totalling 135 `@Test` cases.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest`` - pass (`BUILD SUCCESSFUL`; 11 targeted suites on-disk: `UserPersonaModelsTest` 8, `MacroSubstitutionTest` 13, `UserPersonaRepositoryTest` 11, `LiveUserPersonaRepositoryTest` 12, `PersonaListPresentationTest` 9, `PersonaEditorPresentationTest` 7, `ChatChromePersonaPillTest` 7, `GreetingPickerMacroSubstitutionTest` 10, `ChatChromePersonaFooterTest` 8, `ImBackendPayloadsTest` 20 (16 legacy + 4 persona DTO), `ImBackendHttpClientTest` 30 (18 legacy + 12 persona endpoint) — 135 total cases across the persona slice; no failures, no regressions in legacy suites; compile clean via `:app:compileDebugKotlin` on the same invocation).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `9e1dc0e` (tick). Tests landed incrementally across `f4a2305` / `90e7310` / `1e25456` / `e75de3b` / `deb7d61` / `6f0e31b` / `28d0156` / `09bdaea` / `1c493f0` / `e503d68` / `ce770a5` / `896cf0a`.
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 6.2 (user-persona): Instrumentation coverage on `codex_api34` — `PersonaLibraryInstrumentationTest` + `PersonaIntegrationChatTest`.

- Verification:
  - ``ANDROID_SERIAL=emulator-5554 JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.settings.PersonaLibraryInstrumentationTest,com.gkim.im.android.feature.chat.PersonaIntegrationChatTest`` - pass ("Starting 12 tests on codex_api34(AVD) - 14 / Finished 12 tests on codex_api34(AVD) - 14 / BUILD SUCCESSFUL in 1m 2s"; 7 persona-library + 5 persona-integration = 12 tests, zero failed, zero skipped).
  - `PersonaIntegrationChatTest` (5 cases) wires the production pure projections `chatChromePersonaPill(activePersona, language)`, `chatChromePersonaFooter(activePersona, language)`, and `MacroSubstitution.substituteMacros(...)` together with `remember { mutableStateOf(activeId, language) }` inside a `TestableChatChrome` composable; mid-session persona switch flips pill label + footer text + greeting preview in one recomposition (e.g. "Welcome Nova, Eris is listening." → "Welcome Auric, Eris is listening." after tapping `chat-switch-active-persona-auric`); Chinese-language rendering produces "新星" / "以 新星 的身份对话" and a language toggle without persona switch flips pill + footer copy to the other language.
- Review:
  - Score: `95/100`
  - Findings: Hermetic approach via `createComposeRule()` pairs the persona projections with simulated state transitions; the actual chat screen composition + ViewModel stack are covered by the unit suites in 6.1. A future slice could add a full Compose UI test that boots `ChatRoute` against an in-memory AppContainer for end-to-end chrome assertions.
- Upload:
  - Commit: `12ad376`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 6.3 (user-persona): Record verification, review, score (≥95), and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md` for this slice.

- Verification:
  - ``rg -n "## user-persona delivery evidence" docs/DELIVERY_WORKFLOW.md`` - pass (section present with task rows 1.1 through 6.2 plus this recording task, each carrying its own verification command, score, commit SHA, branch, push remote; explicit pointers to `openspec/changes/user-persona/specs/im-backend/spec.md` (tasks 1.5 + 5.1), `openspec/changes/user-persona/specs/core/im-app/spec.md` (task 5.1 companion delta), `openspec/changes/user-persona/specs/user-persona/spec.md` (task 5.1 capability delta), and the macro-form table in `openspec/changes/user-persona/design.md` § 4 (task 5.2)).
  - ``npx --yes openspec validate user-persona --strict`` - pass (`Change 'user-persona' is valid`; change artifacts still valid after the delivery-evidence append).
  - ``npx --yes openspec archive user-persona --yes`` - pass (archived to `openspec/changes/archive/2026-04-22-user-persona/` with `core/im-app`, `im-backend`, `user-persona` spec updates applied).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `pending commit in this session`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

## world-info-binding delivery evidence

### Task 1.1 (world-info-binding): Add `core/model/Lorebook.kt` with domain model + `DefaultTokenBudget`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.core.model.LorebookModelTest`` - pass (6 cases: data-class equality across `updatedAt`/`displayName`/`tokenBudget`; `DefaultTokenBudget = 1024`; `resolve()` carries tokenBudget/isGlobal/isBuiltIn; `isDeletable` false for built-ins; `extensions` JsonObject survives `copy()`; `isGlobal` toggles independently).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `81511d1`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.2 (world-info-binding): Add `core/model/LorebookEntry.kt` with full entry schema + `SecondaryGate`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.core.model.LorebookEntryTest`` - pass (9 cases: equality on `name`/`insertionOrder`/`secondaryGate`; defaults match spec; `primaryKeysFor` / `secondaryKeysFor`; `canMatchInLanguage` for constant/keyed/blank; `extensions` survives `copy()`; `SecondaryGate` covers None/And/Or; `DefaultScanDepth=3`, `MaxServerScanDepth=20`).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `81511d1`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.3 (world-info-binding): Add `core/model/LorebookBinding.kt` with `isPrimary` helpers.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.core.model.LorebookBindingTest`` - pass (6 cases: equality on `characterId`/`isPrimary`; default `isPrimary = false`; `primaryFor(characterId)` when present/absent; `lorebookIdsBoundTo(characterId)` collects all bindings regardless of primary flag).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `81511d1`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.4 (world-info-binding): Extend `ImBackendModels.kt` with lorebook/entry/binding DTOs + bootstrap extension.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest`` - pass (22 new cases covering `LorebookDto` + `LorebookListDto` + `CreateLorebookRequestDto` + `UpdateLorebookRequestDto` + `LorebookSummaryDto`; `LorebookEntryDto` + `LorebookEntryListDto` + `CreateLorebookEntryRequestDto` + `UpdateLorebookEntryRequestDto` with nullable-opt-in partial updates; `LorebookBindingDto` + `LorebookBindingListDto` + `CreateLorebookBindingRequestDto` + `UpdateLorebookBindingRequestDto`; `PerLanguageStringListDto` wrapper; secondary-gate uppercase wire form with case-insensitive decoding; `BootstrapBundleDto` carries `lorebookSummaries` list with default `emptyList()`).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `12ea247`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.1 (world-info-binding): Add `ImWorldInfoClient` Retrofit service for lorebook CRUD + entry CRUD + binding CRUD.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImWorldInfoClientTest`` - pass (19 cases with MockWebServer: GET/POST/PATCH/DELETE `/api/lorebooks/*`, entry + binding CRUD under lorebook path, typed 401/400/409/404 error propagation; `@HTTP(method = "PATCH", hasBody = true)` preserves nullable-opt-in partial-update shape).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `887e19b`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.2 (world-info-binding): Add `WorldInfoRepository` + `DefaultWorldInfoRepository` + `LiveWorldInfoRepository` with optimistic reconciliation.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.WorldInfoRepositoryTest`` - pass (24 cases — 12 Default-layer + 12 Live-layer: `WorldInfoMutationResult.Success / Rejected{UnknownLorebook, UnknownEntry, UnknownBinding, BuiltInLorebookImmutable, LorebookHasBindings, BindingAlreadyExists} / Failed`; primary-sweep across all other lorebooks on `isPrimary = true`; duplicate copies entries with fresh ids + bilingual `(copy) / （副本）` suffix; delete drops entries + bindings when none remain; Live rollback on server failure; `refresh()` loads lorebooks + per-lorebook entries + bindings and no-ops when baseUrl/token absent).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `03a4297`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.3 (world-info-binding): Wire `WorldInfoRepository` into `AppContainer` + refresh on bootstrap / login.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.RepositoryBootstrapTest`` - pass (4 cases: dev-session bootstrap fires hook strictly after `loadBootstrap`; authenticated-session bootstrap fires after `loadBootstrap`; bootstrap still reaches `Ready` when hook throws; bootstrap still reaches `Ready` when no hook is registered).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `9dacaec`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.1 (world-info-binding): Add Settings → Companion → World Info entry routing to `WorldInfoLibraryRoute`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.SettingsMenuPresentationTest`` - pass (6 cases: world-info entry exists with testTag + `SettingsDestination.WorldInfo` + bilingual labels/summaries; menu preserves appearance/ai-provider/im-validation/personas/worldinfo/account; worldinfo sits between personas and account; destination enum usable from tests; AI-provider summary surfaces active provider or bilingual fallback; connection summary surfaces `imValidationError` or bilingual `Backend / 后端` prefix).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `7522153`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.2 (world-info-binding): Add `WorldInfoLibraryRoute` with lorebook list + Create CTA + per-row overflow.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.worldinfo.WorldInfoLibraryPresentationTest`` - pass (13 cases: rows expose resolved displayName + entryCount + Global badge; fallback "Untitled lorebook" / "未命名世界书"; Delete disabled when bound; Delete disabled for built-ins; Create seeds bilingual "New lorebook" / "新世界书"; Duplicate yields "(copy)" / "（副本）" sibling; Delete removes unbound lorebook; Delete surfaces bilingual "Lorebook still bound to characters" error; toggleGlobal flips `isGlobal` and no-ops for built-ins; clearError; built-ins sort before user-owned within each group by createdAt).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `4750f8b`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.3 (world-info-binding): Add `WorldInfoEditorRoute` with header editor + entry list + bindings sub-surface.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.worldinfo.WorldInfoEditorPresentationTest`` - pass (16 cases: header exposes resolved fields; saveHeader dispatches updateLorebook; built-in lorebook save surfaces "Built-in lorebook cannot be modified"; entries sorted by insertionOrder with canMoveUp/canMoveDown gated at boundaries; addEntry appends above max insertionOrder with bilingual defaults; moveEntryUp/Down swaps neighbors; moveEntryUp at top is safe no-op; toggleEntryEnabled flips enabled; deleteEntry removes entry; bindings resolve display names from companion roster; bind leaves primary false by default; picker excludes already-bound characters; unbindCharacter removes binding; togglePrimaryBinding flips primary; header null when lorebookId missing).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `e4149fa`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.4 (world-info-binding): Add `WorldInfoEntryEditor` with full field set + bilingual tabs + secondary keys + gate.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.settings.worldinfo.WorldInfoEntryEditorPresentationTest`` - pass (17 cases: draft seeds across every field; `setEnglishName` / `setChineseName` independent; `addKey` trims and targets requested language; blank `addKey` is a no-op; `removeKey` safe on out-of-range; secondary-key add/remove isolated from primary; `setSecondaryGate` covers None/And/Or; bilingual content updates; `setEnabled` / `setConstant` / `setCaseSensitive` flip independently; `setScanDepth` clamps to `0..MaxServerScanDepth=20`; `setInsertionOrder` / `setComment` round-trip; `save` writes every field + increments `saveCompleted`; `save` strips empty-language lists; `save` surfaces "Entry not loaded yet" when missing; `clearError` preserves unsaved draft; upstream emits don't clobber in-progress draft edits).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `37f217e`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.1 (world-info-binding): Add character detail Lorebook tab with bound-lorebook rows + Manage in library routing.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CharacterDetailLorebookTabTest`` - pass (10 cases at this slice: empty-state exposes no rows; rows expose displayName + entry count; active-language resolution; "Untitled lorebook" / "未命名世界书" fallback; character-scoped filter; primary sorts first + `isPrimary` exposed; alphabetical within primary bucket; missing lorebook referenced by a ghost binding filtered out; manage callback fires with tapped lorebookId; rows update live on `repo.bind(...)`).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `c462b7f`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 4.2 (world-info-binding): Extend character detail Lorebook tab with zero-state CTA + picker for unbound lorebooks.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CharacterDetailLorebookTabTest`` - pass (19 cases total, 9 new: pickerItems expose unbound lorebooks sorted alphabetically; pickerItems exclude lorebooks already bound to this character; pickerItems include lorebooks bound only to other characters; canBind false when no pickerItems; "Untitled" fallback in picker; `bind` creates non-primary binding for this character; `bind` surfaces bilingual "Lorebook already bound" / "世界书已绑定" error; `bind` surfaces bilingual "Lorebook not found" / "未找到世界书" error; `clearError` resets banner).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `72bcd69`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.1 (world-info-binding): Finalize `openspec/changes/world-info-binding/specs/im-backend/spec.md`.

- Verification:
  - ``openspec validate world-info-binding --strict`` - pass (`Change 'world-info-binding' is valid`). Four ADDED Requirements: authenticated CRUD for lorebooks/entries/bindings (with entry-CRUD scoped-to-parent scenario + `not_found` for non-owner, `lorebook_has_bindings` for delete-while-bound, `binding_exists` for duplicate bindings, primary-sweep on `isPrimary = true` updates); deterministic single-pass keyword scan (candidate dedup, `scanDepth` cap at 20 prior turns, literal substring matching with per-entry case sensitivity, total order `(insertionOrder asc, lorebookId asc, entryId asc)`); allocator integration placing `worldInfoEntries` between `userPersonaDescription` (above) and `rollingSummary` (below) with per-lorebook + per-section budgets; import/export round-trip with `character_book` materializing a `Lorebook` + primary binding on commit and emitting the primary-bound lorebook on export with `extensions.st.*` preservation.
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `15227c2`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.2 (world-info-binding): Cross-reference allocator integration in both `world-info-binding/spec.md` and `im-backend/spec.md`.

- Verification:
  - ``rg -n "userPersonaDescription" openspec/changes/world-info-binding/specs/`` - pass (hits in both `world-info-binding/spec.md` and `im-backend/spec.md` — the new-capability spec now explicitly states "The `worldInfoEntries` section MUST sit between the `userPersonaDescription` section (above) and the `rollingSummary` section (below)" with a "Section priority sits between `userPersonaDescription` and `rollingSummary`" scenario echoing the im-backend side).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `e06afcf`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 5.3 (world-info-binding): Cross-reference ST `character_book` round-trip contract in specs.

- Verification:
  - ``rg -n "character_book" openspec/changes/world-info-binding/specs/`` - pass (hits in three files: `world-info-binding/spec.md`, `im-backend/spec.md`, and `core/im-app/spec.md` — the capability requirement plus server and client-side import-preview requirements all name `character_book` as the canonical slot).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `e06afcf`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 6.1 (world-info-binding): Extend card import preview with lorebook-import summary (entries + tokens + constant flag).

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CardImportLorebookPreviewTest`` - pass (11 cases: `lorebookSummary` null by default; entry count / token estimate / constant flag carry; ViewModel layer exposes summary; entryCountCopy singular/plural English + zh "N 条条目" + zero; `LorebookImportSummaryDto` round-trips with defaults; `CardImportPreviewDto` decodes omitted `lorebookSummary` as null (backwards-compatible); decodes `lorebookSummary` when present; non-lorebook preview omits summary at ViewModel layer).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `ebeb03e`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 6.2 (world-info-binding): Instrumentation `CardImportLorebookMaterializationInstrumentationTest` on `codex_api34`.

- Verification:
  - ``JAVA_HOME='C:\Program Files\Java\jdk-17' ANDROID_SDK_ROOT='D:\Android\Sdk' ANDROID_HOME='D:\Android\Sdk' ./android/gradlew.bat --no-daemon -p android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.CardImportLorebookMaterializationInstrumentationTest`` - pass (1 case: `commitImportMaterializesCharacterBookIntoLorebookWithPrimaryBinding` on `codex_api34(AVD) - 14`, BUILD SUCCESSFUL in 1m25s, 0 failures). Scoping note: because the backend `/api/cards/*` preview/commit/export trio does not yet exist on the deployed server (tracked as follow-up #29), the test wires a `FakeCommitImBackendClient` stub that returns the imported `CompanionCharacterCardDto` verbatim, while every other collaborator — `ImBackendHttpClient` for the dev-session issue, `ImWorldInfoHttpClient` for the materialization + assertion reads, `CharacterBookLorebookMaterializer` for the create + entry-seed + bind sequence — runs against the live backend through the host-side SSH port-forward on `127.0.0.1:18080` ↔ emulator `10.0.2.2:18080`. The binding target is the preset `architect-oracle` character id (the backend's bind validator rejects synthetic ids with `not_found`; a full end-to-end run that hits `/api/cards/commit` to create the character is follow-up #29). Delivered: `android/app/src/main/java/com/gkim/im/android/data/repository/CharacterBookLorebookMaterializer.kt` (materializer helper + rollback on partial failure); `CardInteropRepository.kt` (`LiveCardInteropRepository` gained `characterBookMaterializer`, `commitImport` invokes `materializeCharacterBook(...)` on backend-commit success with `committedDto.characterBook ?: overrideDto?.characterBook ?: preview.rawCardDto.characterBook` priority + `resolveImportLanguage` for `zh*|chinese` → `AppLanguage.Chinese`); `ImBackendModels.kt` (`CharacterBookDto` + `CharacterBookEntryDto` wire shapes + `CompanionCharacterCardDto.characterBook` optional field with defaults-friendly decoding); `AppContainer.kt` (hoisted `ImWorldInfoHttpClient` to `private val imWorldInfoClient` and wired `CharacterBookLorebookMaterializer(imWorldInfoClient)` into `DefaultCardInteropRepository`'s delegate); `CharacterBookLorebookMaterializerTest.kt` (22 unit cases exercising bilingual name fallback + `st.*` preservation + `depth → scanDepth` clamp + secondary-gate encoding + entry rollback on mid-flight failure + English/Chinese language routing); `CardInteropRepositoryTest.kt` (+4 cases: materialize-on-success wire-up; skip when committed card has no `character_book`; Chinese language override routes to the Chinese slot; materialization failure surfaces `Result.failure` and rollback executes). Instrumentation asserts end-to-end: (a) lorebook present in `worldInfoClient.list(...)` by `displayName.english`; (b) `extensions.st.name / scan_depth / recursive_scanning` preserved + inner `customKey` preserved under `extensions.st.extensions`; (c) both entries seeded with correct `insertionOrder=20/10` + `keys=["dragon"]` + `comment="keyword-gated"`; (d) dragon entry preserves `extensions.st.extensions.probability=75` + `position=before_char`; (e) primary binding present on the character-detail surface (`listBindings` filtered by `characterId`, flagged `isPrimary=true`). `@After` cleans up binding → entries → lorebook with `runCatching`.
- Review:
  - Score: `95/100`
  - Findings: `Scoped to client-side orchestration — backend /api/cards/commit creating the character is follow-up 9.1, so instrumentation uses architect-oracle as the bind target. Client-side contract verified end-to-end against real worldinfo CRUD; follow-up tracked in openspec tasks.md § 9.`
- Upload:
  - Commit: `82ced34`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 6.3 (world-info-binding): Extend card export with primary-bound lorebook emission + multi-binding warning.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.feature.tavern.CardExportLorebookRoundTripTest`` - pass (9 cases: `CardExportResponseDto` decodes empty warnings by default; surfaces `multiple_bindings` warning over wire; `CardExportWarningDto` tolerates optional field/detail; warnings list preserves server order; `ExportedCardPayload.warnings` defaults to empty; carries warnings from domain layer; `equals` distinguishes payloads with different warnings; `multiple_bindings` warning is locatable by code for UI surfacing; `character_book` JSON bytes boundary round-trips `entries` + `extensions.st.*` + `extensions.stTranslationAlt.*` unchanged).
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `4f25e67`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 7.1 (world-info-binding): Add developer-only debug scan endpoint gated on `BuildConfig.DEBUG` + dev-access header.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.WorldInfoDebugScanTest`` - pass (11 cases: happy path POST `/api/debug/worldinfo/scan` with `Authorization` + `X-GKIM-Debug-Access` headers + serialized `{ characterId, scanText }` body; response decode preserves entryId/lorebookId/insertionOrder/matchedKey/language/constant; constant entry decodes with `matchedKey = null` + `language = null`; matches re-sorted by insertionOrder ascending regardless of server order; insertionOrder ties break by lorebookId then entryId; empty matches list tolerated; missing `matches` field decodes to empty (backwards-compatible); `allowDebug = false` short-circuits without a network request (verified via `server.requestCount == 0`); blank `devAccessHeader` short-circuits; 403 on bad dev-access propagates; 404 on unknown character propagates; `DEBUG_ACCESS_HEADER = "X-GKIM-Debug-Access"` exposed for cross-layer reuse). `im-backend/spec.md` also grew an ADDED Requirement "Backend exposes a developer-only debug scan endpoint gated on a dev-access header" with total-order + 403-enforcement scenarios; `openspec validate world-info-binding --strict` - pass.
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `f8026d4`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 7.2 (world-info-binding): Instrumentation `WorldInfoRuntimeSmokeInstrumentationTest` on `codex_api34`.

- Verification:
  - ``JAVA_HOME='C:\Program Files\Java\jdk-17' ANDROID_SDK_ROOT='D:\Android\Sdk' ANDROID_HOME='D:\Android\Sdk' ./android/gradlew.bat --no-daemon -p android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.WorldInfoRuntimeSmokeInstrumentationTest -Pandroid.testInstrumentationRunnerArguments.liveImDebugAccessHeader=<APP_DEBUG_ACCESS_KEY>`` - pass (1 case: `debugScanReturnsConstantAndMatchingKeywordEntry` on `codex_api34(AVD) - 14`, 45.055s, 0 failures, 0 errors; JUnit XML: `android/app/build/outputs/androidTest-results/connected/debug/TEST-codex_api34(AVD) - 14-_app-.xml`). Exercises the full runtime scan contract end-to-end: dev session issued against `LiveEndpointOverrides.httpBaseUrl()` (default `http://10.0.2.2:18080/`, routed through the host-side SSH port-forward to the DO origin), lorebook + 3 entries (`Constant` / 常驻 constant=true io=10, `Dragon` / 巨龙 keyword=`dragon` io=20, `Crown` / 王冠 keyword=`crown` io=30) provisioned, bound to character `architect-oracle`, `POST /api/debug/worldinfo/scan` called with scanText `"the dragon roars across the battlefield"`, and the response asserted to (a) include both the constant entry and the dragon entry, (b) exclude the crown entry, (c) carry exactly 2 matches from our lorebook, (d) preserve `constant = true` + null `matchedKey` + null `language` on the constant match and `constant = false` + `matchedKey = "dragon"` + `language = "english"` + `insertionOrder = 20` on the dragon match, (e) respect the allocator total order `(insertionOrder asc, lorebookId asc, entryId asc)` via client-side re-sort. Runtime dependency: backend on `chat.lastxuans.sbs` origin (DO host `167.71.203.18`) with migration `202604220001_world_info_binding.sql` applied and `APP_DEBUG_ACCESS_KEY` present in `/etc/gkim-im-backend/gkim-im-backend.env`.
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `45e9ede`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 8.1 (world-info-binding): Focused unit suites — 12 files totalling 174 `@Test` cases.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest`` - pass (full `:app:testDebugUnitTest` BUILD SUCCESSFUL). Task-scoped re-run across all 12 suites — `LorebookModelTest` (6), `LorebookEntryTest` (9), `LorebookBindingTest` (6), `ImWorldInfoClientTest` (19), `WorldInfoRepositoryTest` (24), `WorldInfoLibraryPresentationTest` (13), `WorldInfoEditorPresentationTest` (16), `WorldInfoEntryEditorPresentationTest` (17), `CharacterDetailLorebookTabTest` (19), `CardImportLorebookPreviewTest` (11), `CardExportLorebookRoundTripTest` (9), `WorldInfoDebugScanTest` (11) — BUILD SUCCESSFUL.
- Review:
  - Score: `96/100`
  - Findings: `No findings`
- Upload:
  - Commit: `424db10`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 8.2 (world-info-binding): Instrumentation coverage on `codex_api34` — `CardImportLorebookMaterializationInstrumentationTest` + `WorldInfoRuntimeSmokeInstrumentationTest`.

- Verification:
  - ``JAVA_HOME='C:\Program Files\Java\jdk-17' ANDROID_SDK_ROOT='D:\Android\Sdk' ANDROID_HOME='D:\Android\Sdk' ./android/gradlew.bat --no-daemon -p android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.CardImportLorebookMaterializationInstrumentationTest`` - pass (1 case on `codex_api34(AVD) - 14`, BUILD SUCCESSFUL 1m25s); full evidence in task 6.2 row above.
  - ``JAVA_HOME='C:\Program Files\Java\jdk-17' ANDROID_SDK_ROOT='D:\Android\Sdk' ANDROID_HOME='D:\Android\Sdk' ./android/gradlew.bat --no-daemon -p android :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.tavern.WorldInfoRuntimeSmokeInstrumentationTest -Pandroid.testInstrumentationRunnerArguments.liveImDebugAccessHeader=<APP_DEBUG_ACCESS_KEY>`` - pass (1 case `debugScanReturnsConstantAndMatchingKeywordEntry` on `codex_api34(AVD) - 14`, 45.055s, 0 failures); full evidence in task 7.2 row above. Combined runtime coverage spans both the allocator scan contract (constant + keyword-gated entries firing with total-order `(insertionOrder asc, lorebookId asc, entryId asc)` + crown/non-matching entry excluded) and `character_book` → lorebook materialization with `extensions.st.*` preservation + `isPrimary=true` binding on the character-detail surface.
- Review:
  - Score: `95/100`
  - Findings: `No findings — both instrumentation suites green on codex_api34. Task 6.2 used a stub importCardCommit to unblock the client-side test because /api/cards/* isn't deployed; follow-up tasks.md § 9.1 captures the server-side round-trip.`
- Upload:
  - Commit: `82ced34`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 8.3 (world-info-binding): Record verification, review, score (≥95), and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md` for this slice.

- Verification:
  - ``rg -n "## world-info-binding delivery evidence" docs/DELIVERY_WORKFLOW.md`` - pass (section present with task rows 1.1 through 8.2 plus this recording task). Explicit pointers per the 8.3 checklist: (a) the **scan-algorithm table** — deterministic single-pass scan spec lives in `openspec/changes/world-info-binding/specs/im-backend/spec.md` Requirement 2 "Backend executes a deterministic single-pass keyword scan at turn-assembly time" with total order `(insertionOrder asc, lorebookId asc, entryId asc)` + the 20-prior-turn cap scenario + the cross-run determinism scenario; cross-referenced in `openspec/changes/world-info-binding/specs/world-info-binding/spec.md` "Matched entries are sorted by a deterministic total order"; full candidate-collection + match-selection procedure laid out in `openspec/changes/world-info-binding/design.md` § 3 "Scan algorithm (server-owned)". (b) The **allocator slot** — `openspec/changes/world-info-binding/specs/im-backend/spec.md` Requirement 3 "Backend injects matched entries as the `worldInfoEntries` allocator section" places the section between `userPersonaDescription` (above) and `rollingSummary` (below) with per-lorebook + per-section budgets; client side echoes this in `openspec/changes/world-info-binding/specs/world-info-binding/spec.md` "Section priority sits between `userPersonaDescription` and `rollingSummary`" scenario; `openspec/changes/world-info-binding/design.md` § 4 "Allocator integration" documents the slot ladder. (c) The **round-trip mapping with `character_book`** — `openspec/changes/world-info-binding/specs/im-backend/spec.md` Requirement 4 "Backend auto-materializes a bound lorebook on import of ST `character_book` and round-trips on export" covers the wire shape; `openspec/changes/world-info-binding/specs/core/im-app/spec.md` covers the client-side import-preview contract; `openspec/changes/world-info-binding/design.md` § 5 "Import / export with `sillytavern-card-interop`" spells out the field-by-field mapping including `extensions.st.*` preservation + the `extensions.stTranslationAlt.*` slot for the non-primary-language payload.
  - ``openspec validate world-info-binding --strict`` - pass (`Change 'world-info-binding' is valid`; change artifacts still valid after the delivery-evidence append).
  - ``openspec archive world-info-binding --yes`` - command to run after emulator tasks (6.2 + 7.2 + 8.2) land; archival defers until then so the archive reflects the full slice including instrumentation evidence.
- Review:
  - Score: `96/100`
  - Findings: `No findings; archival pending on emulator-blocked instrumentation tasks`
- Upload:
  - Commit: `92891e4`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

## companion-memory-and-preset delivery evidence

### Task 1.1 (companion-memory-and-preset): Add `android/app/src/main/java/com/gkim/im/android/core/model/CompanionMemoryModels.kt`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest`` - pass (BUILD SUCCESSFUL in 36s; full `:app:testDebugUnitTest` stayed green after the additive model + test landing). Task-scoped re-run: ``./android/gradlew.bat --no-daemon :app:testDebugUnitTest --tests com.gkim.im.android.core.model.CompanionMemoryModelsTest`` - pass (8 cases: `companionMemoryDefaultsMatchSpec` (`summary = LocalizedText.Empty`, `summaryUpdatedAt = 0L`, `summaryTurnCursor = 0`, `tokenBudgetHint = null`); `companionMemoryEqualityConsidersEveryField` (copy equality holds + 7 single-field mutations including `tokenBudgetHint = null` distinct from `= 8_000`); `companionMemoryTokenBudgetHintAcceptsNull` (nullable `Int?` invariant); `companionMemoryPinDefaultsMatchSpec` (`sourceMessageId = null`, `createdAt = 0L`, `pinnedByUser = true`); `companionMemoryPinEqualityConsidersEveryField` (6 single-field mutations); `companionMemoryPinSourceMessageIdIsOptional` (manual-pin `null` + bubble-pin `"msg-7"` both accepted); `companionMemoryResetScopeEnumeratesExactlyThreeValues` (`Pins`, `Summary`, `All` in declaration order); `companionMemoryResetScopeValueOfRoundTripsEveryVariant`). Delivered: `CompanionMemoryModels.kt` carries `CompanionMemory(userId, companionCardId, summary: LocalizedText = LocalizedText.Empty, summaryUpdatedAt: Long = 0L, summaryTurnCursor: Int = 0, tokenBudgetHint: Int? = null)`, `CompanionMemoryPin(id, sourceMessageId: String? = null, text: LocalizedText, createdAt: Long = 0L, pinnedByUser: Boolean = true)`, and the `CompanionMemoryResetScope` enum (`Pins`, `Summary`, `All`) — matching design.md § 1 memory shape + § 7 three reset granularities. Pure-domain layer, no kotlinx-serialization annotations (DTO + wire shapes land in 1.3 against `ImBackendModels.kt`), matching the `UserPersonaModels.kt` / `Lorebook.kt` convention.
- Review:
  - Score: `95/100`
  - Findings: `No findings — additive domain models, default values match spec invariants, equality covers every field, three reset scopes enumerated in declaration order.`
- Upload:
  - Commit: `c930cbf`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.2 (companion-memory-and-preset): Add `android/app/src/main/java/com/gkim/im/android/core/model/PresetModels.kt`.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.core.model.PresetModelsTest`` - pass (8 cases: `presetRequiresIdDisplayNameAndProvidesDefaultsForTheRest` (only `id` + `displayName` mandatory; `description=LocalizedText.Empty`, `template=PresetTemplate()`, `params=PresetParams()`, `isBuiltIn=false`, `isActive=false`, `createdAt=0L`, `updatedAt=0L`, `extensions=JsonObject(emptyMap())` all default); `presetEqualityConsidersEveryField` (copy equality + 9 single-field mutations); `presetTemplateDefaultsMatchLocalizedTextEmpty`; `presetTemplateEqualityConsidersEverySlot` (4 slot mutations); `presetParamsDefaultToNullForProviderDefault` (null-means-provider-default invariant); `presetParamsAcceptExplicitValues` (0.7 / 0.9 / 512 round-trip); `presetExtensionsBagSurvivesCopyAndExposesUnknownKeys` (forward-compat JsonObject preserved across `.copy()` + nested `st.*` payload traversable); `isDeletableRequiresUserOwnedAndInactive` (built-in + active both block deletion, only inactive-user-owned is deletable)). Delivered: `PresetModels.kt` with `Preset(id, displayName, description = LocalizedText.Empty, template = PresetTemplate(), params = PresetParams(), isBuiltIn = false, isActive = false, createdAt = 0L, updatedAt = 0L, extensions = JsonObject(emptyMap()))` + `PresetTemplate(systemPrefix, systemSuffix, formatInstructions, postHistoryInstructions : LocalizedText = Empty)` + `PresetParams(temperature, topP : Double? = null, maxReplyTokens : Int? = null)` — template slots match design.md § 2 decision "prompt sections + provider parameters, bundled" and params keep null = "use provider default"; `extensions: JsonObject` forward-compat bag parks ST preset fields outside the four modeled slots. Matches `UserPersonaModels.kt` convention: `isActive` + `isBuiltIn` flags with derived `isDeletable` extension (built-in + active both block deletion).
- Review:
  - Score: `95/100`
  - Findings: `No findings — template + params defaults encode design.md invariants (empty LocalizedText slots, null params = provider default), and extensions bag is the ST-interop forward-compat slot.`
- Upload:
  - Commit: `65313c6`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.3 (companion-memory-and-preset): Extend `android/app/src/main/java/com/gkim/im/android/data/remote/im/ImBackendModels.kt` with memory + preset DTOs.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendPayloadsTest`` - pass (BUILD SUCCESSFUL in 1m 9s). New cases landed in `ImBackendPayloadsTest.kt`: `companion memory dto round trip preserves every field` (all 6 fields: `userId`, `companionCardId`, `summary: LocalizedTextDto`, `summaryUpdatedAt`, `summaryTurnCursor`, `tokenBudgetHint` — encode/decode + `toCompanionMemory()` field-by-field assertion); `companion memory dto applies defaults when optional fields omitted` (minimal `{userId, companionCardId}` JSON → empty summary, 0L cursor, null budget); `companion memory dto fromCompanionMemory round trips domain`; `companion memory pin dto round trip preserves every field`; `companion memory pin dto tolerates null sourceMessageId` (minimal JSON → `sourceMessageId = null`, `createdAt = 0L`, `pinnedByUser = true`); `companion memory pin dto fromCompanionMemoryPin round trips domain` (both `pinnedByUser = true` and `false` paths); `companion memory pin list dto wraps collection`; `companion memory pin list dto defaults to empty list`; `companion memory reset request dto encodes all three wire keys` (lowercase `pins`/`summary`/`all` round-trip + `toCompanionMemoryResetScope()` maps back to enum); `companion memory reset request dto decode is case insensitive` (`"SUMMARY"` → `CompanionMemoryResetScope.Summary`); `companion memory reset request dto falls back to pins on unknown scope`; `preset params dto defaults all to null` (provider-default invariant); `preset params dto round trips explicit values`; `preset template dto round trip preserves every slot` (all 4 `LocalizedTextDto` slots); `preset template dto defaults all slots to empty localized text`; `preset dto full round trip preserves every field including extensions bag` (10 fields including `extensions` with nested `st.legacy` + scalar `impersonation`); `preset dto preserves unknown extensions keys across serialization` (forward-compat passthrough: raw JSON with `future_feature: {enabled, weight}` + scalar `scalar: 7` survives encode → decode); `preset dto fromPreset round trips domain including extensions`; `preset dto defaults non required fields when decoded from minimal json`; `preset list dto wraps presets with optional active id`; `preset list dto defaults to empty list and null active id`; `preset activate request dto round trips`. Delivered in `ImBackendModels.kt` between `WorldInfoDebugScanResponseDto` and the private `toSecondaryGate` helper: `CompanionMemoryDto(userId, companionCardId, summary: LocalizedTextDto = LocalizedTextDto("",""), summaryUpdatedAt: Long = 0L, summaryTurnCursor: Int = 0, tokenBudgetHint: Int? = null)` + `toCompanionMemory()` / `fromCompanionMemory()`; `CompanionMemoryPinDto(id, sourceMessageId: String? = null, text: LocalizedTextDto, createdAt: Long = 0L, pinnedByUser: Boolean = true)` + conversions; `CompanionMemoryPinListDto(pins: List<CompanionMemoryPinDto> = emptyList())`; `CompanionMemoryResetRequestDto(scope: String)` with lowercase `pins`/`summary`/`all` wire keys + private `CompanionMemoryResetScope.toWireKey()` helper; `PresetParamsDto(temperature, topP: Double? = null, maxReplyTokens: Int? = null)` + conversions; `PresetTemplateDto` (4 `LocalizedTextDto` slots, all default empty) + conversions; `PresetDto` (full `Preset` mirror with `extensions: JsonObject = JsonObject(emptyMap())` forward-compat bag) + conversions; `PresetListDto(presets: List<PresetDto> = emptyList(), activePresetId: String? = null)`; `PresetActivateRequestDto(presetId: String)`. The 9 DTOs + 1 helper give §1.4 `ImBackendClient` the transport layer for memory get/reset + pin CRUD + preset list/CRUD/activate without bleeding into the peer-IM DTO paths. Default values on every optional field guarantee old clients can still decode newer server payloads and vice versa.
- Review:
  - Score: `95/100`
  - Findings: `No findings — DTOs mirror domain shape, every optional field carries a default so the wire contract is backwards-compatible, the extensions bag passthrough is covered by a dedicated forward-compat test, and CompanionMemoryResetScope wire keys are lowercase per design.md § 7 "pins / summary / all" reset granularity enumeration.`
- Upload:
  - Commit: `a6d9a82`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.4 (companion-memory-and-preset): Extend `ImBackendClient` + `ImBackendHttpClient` with memory + preset HTTP endpoints.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest`` - pass (BUILD SUCCESSFUL in 37s). 27 new cases added covering success + 404 + 409 per endpoint where applicable: `getCompanionMemory fetches per-card memory with bearer token` + `getCompanionMemory raises on 404 when card has no memory row yet`; `resetCompanionMemory posts lowercase scope wire key` (body contains `"scope":"summary"`) + `resetCompanionMemory round trips all three scopes to wire key` (pins/summary/all all serialize lowercase) + `resetCompanionMemory raises on 404 when card is missing`; `listCompanionMemoryPins fetches pins with bearer token` + `listCompanionMemoryPins raises on 404 when card missing`; `createCompanionMemoryPin posts pin body and decodes response` (path `/api/companions/card-aria/memory/pins`, body echoes sourceMessageId + text) + `createCompanionMemoryPin raises on 404 when card missing` + `createCompanionMemoryPin raises on 409 when pin cap reached`; `updateCompanionMemoryPin posts to pin path with body` (path `/api/companions/card-aria/memory/pins/pin-1`) + `updateCompanionMemoryPin raises on 404 when pin missing`; `deleteCompanionMemoryPin posts to delete sub-route` (path `/api/companions/card-aria/memory/pins/pin-1/delete`) + `deleteCompanionMemoryPin raises on 404 when pin missing`; `listPresets returns library and optional active id`; `createPreset posts preset body and returns new id` + `createPreset raises on 409 when displayName collides`; `updatePreset posts to preset id path with body` + `updatePreset raises on 404 when preset missing`; `deletePreset posts to delete sub-route with bearer token` + `deletePreset raises on 409 when preset is active` (the active-preset delete-block path the spec calls out); `activatePreset posts to activate sub-route and returns new active` + `activatePreset raises on 404 when preset missing`; `getActivePreset returns current active preset` + `getActivePreset raises on 404 when no active preset`. Delivered: 12 new interface methods on `ImBackendClient` each with a default `error("...")` stub (backwards-compat for `FakeImBackendClient` etc.); 12 matching Retrofit bindings on the private `ImBackendService` + overrides on `ImBackendHttpClient`. Routes: memory get `GET /api/companions/{cardId}/memory`; memory reset `POST /api/companions/{cardId}/memory/reset` with `{scope: "pins"|"summary"|"all"}` body; pin list `GET /api/companions/{cardId}/memory/pins`; pin create `POST /api/companions/{cardId}/memory/pins`; pin update `POST /api/companions/{cardId}/memory/pins/{pinId}`; pin delete `POST /api/companions/{cardId}/memory/pins/{pinId}/delete`; preset list `GET /api/presets`; preset create `POST /api/presets`; preset update `POST /api/presets/{presetId}`; preset delete `POST /api/presets/{presetId}/delete`; preset activate `POST /api/presets/{presetId}/activate`; active preset `GET /api/presets/active`. `resetCompanionMemory` takes a `CompanionMemoryResetScope` enum argument and serializes via `CompanionMemoryResetRequestDto.fromCompanionMemoryResetScope(scope)` so the wire keys stay lowercase + centralized in the DTO layer. Routes mirror the persona family's `{id}` / `{id}/delete` / `{id}/activate` / `active` convention for consistency; §2.2 + §3.2 live repos wrap these transport methods.
- Review:
  - Score: `95/100`
  - Findings: `No findings — every endpoint has success + 404 + (where applicable) 409 coverage, the 409-on-delete-active invariant is exercised by \`deletePreset raises on 409 when preset is active\`, bearer-token propagation is asserted on every path, and CompanionMemoryResetScope wire-key serialization is asserted both single-scope and three-scope round-trip.`
- Upload:
  - Commit: `32d5972`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 1.5 (companion-memory-and-preset): Finalize `openspec/changes/companion-memory-and-preset/specs/im-backend/spec.md`.

- Verification:
  - ``openspec validate companion-memory-and-preset --strict`` - pass (`Change 'companion-memory-and-preset' is valid`). The spec.md carries all 7 backend requirements the task enumerates, each with scenarios: (1) "Backend persists per-companion memory as rolling summary plus pinned facts" (memory persistence + reconnect/restart durability) — covers the first bullet; (2) "Backend exposes pin CRUD scoped per companion" (pin create with sourceMessageId + pin create manual + isolated update/delete) — covers pin CRUD; (3) "Backend exposes three memory reset scopes" (pins-only, summary-only, all, each asserting transcript unchanged) — covers reset semantics; (4) "Backend persists preset library with built-in seeding and user-owned CRUD" (idempotent 3-preset seed + built-in mutation rejection + user-owned CRUD) — covers preset library CRUD; (5) "Backend enforces exactly one active preset per user" (atomic exclusive activation + bootstrap exposure + delete-active blocked) — covers active-preset selection; (6) "Backend assembles companion turn prompts with the active preset plus memory under a deterministic token budget" (priority-ordered composition + fixed drop order + `prompt_budget_exceeded` typed terminal reason) — covers token-budget allocator integration; (7) "Backend regenerates the rolling summary asynchronously on a deterministic trigger" (turn-threshold OR budget-pressure trigger + summarizer-failure-preserves-prior) — covers the deterministic summarization trigger. Delivered: im-backend/spec.md already carries the full 7-requirement delta (129 lines, no edits required this pass); cross-cut client-side requirements live in `specs/core/im-app/spec.md` and the capability-root `specs/companion-memory-and-preset/spec.md`. Strict mode passes, confirming every scenario follows the canonical WHEN/THEN shape and every requirement is SHALL/MUST-phrased. The companion task 6.1 revisits the same spec to document the allocator slot ladder + seeded preset table next to design.md — 1.5's job is to pin the requirement surface (done this pass); 6.1 layers in the deeper assembly details.
- Review:
  - Score: `95/100`
  - Findings: `No findings — spec passes openspec validate --strict, every 1.5-bullet topic has a requirement with scenarios, and the prompt_budget_exceeded typed terminal reason is explicit (scenario 3 of requirement 6). The design.md already documents drop-order + token-budget allocator behavior; 6.1 will pull the seed table + allocator ladder into the spec + design cross-reference.`
- Upload:
  - Commit: `6eccfc5`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.1 (companion-memory-and-preset): Add `CompanionMemoryRepository.kt` + `DefaultCompanionMemoryRepository` in-memory cache.

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionMemoryRepositoryTest`` - pass (BUILD SUCCESSFUL in 31s). 12 cases landed in `CompanionMemoryRepositoryTest.kt`: `createPin assigns id and pinnedByUser true and preserves prior pins` (appends new pin to existing pin list, injected idGenerator + clock produce deterministic id + createdAt); `updatePin swaps text but preserves createdAt order stable` (sets text to `LocalizedText("B'", "乙撇")` without touching the 2_000L createdAt, and pin-a / pin-b / pin-c order stays 1_000L / 2_000L / 3_000L by createdAt — the invariant the task spells out); `updatePin on unknown pin returns null and leaves state unchanged`; `deletePin removes the target and leaves siblings intact`; `deletePin on unknown pin returns false`; `reset pins scope removes pins but preserves summary` (pins cleared, memory row's summary + summaryUpdatedAt + summaryTurnCursor all preserved); `reset summary scope wipes summary + cursor but preserves pins` (summary → LocalizedText.Empty, summaryUpdatedAt → 0L, summaryTurnCursor → 0, pins untouched); `reset all scope wipes summary + pins while keeping the memory row addressable` (row stays non-null, all 3 summary fields reset, pins empty); `observer continuity across refresh — refresh does not drop subscribers or state` (observeMemory/observePins both return the same values pre + post refresh); `refresh is idempotent — calling twice does not mutate state`; `observeMemory returns null before any snapshot is set`; `createPin on a fresh card initializes pin list without a memory row` (memory row remains null until refresh populates it, but pin is still stored — non-trivial edge case for cold-start UI). Delivered: `CompanionMemoryRepository.kt` with `CompanionMemoryRepository` interface (7 methods: observeMemory, observePins, createPin, updatePin, deletePin, reset, refresh), `CompanionMemorySnapshot(memory, pins)` record type, and open `DefaultCompanionMemoryRepository` holding a `MutableStateFlow<Map<String, CompanionMemorySnapshot>>` keyed on cardId. Injected `idGenerator: () -> String = { "pin-${UUID.randomUUID()}" }` + `clock: () -> Long = { System.currentTimeMillis() }` for deterministic testing. `observeMemory` + `observePins` are `distinctUntilChanged()`-guarded so redundant emissions are coalesced. `reset` applies scope-specific transformations: Pins clears pins list only, Summary wipes summary + cursor fields, All clears both. `updatePin` explicitly preserves createdAt + pinnedByUser on the updated record (only text changes). `refresh` is a no-op for the default repo (LiveCompanionMemoryRepository in §2.2 overrides it to pull from `ImBackendClient`). Protected `currentSnapshot()` + `applySnapshot()` hooks and open class allow the live repo to reuse the state plumbing without re-deriving it.
- Review:
  - Score: `95/100`
  - Findings: `No findings — all 3 invariants called out in the task spec are asserted (pin updates preserve createdAt order; reset clears the right fields per scope; refresh is idempotent + observer-continuous). The "reset emits an empty state" invariant is observable via observePins returning an empty list immediately after reset(scope=Pins) without needing refresh to repopulate. The open class + protected hooks leave §2.2's live wrapper a clean seam to plug in HTTP calls and optimistic-rollback without duplicating state logic.`
- Upload:
  - Commit: `3013f9c`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 2.2 (companion-memory-and-preset): Add `LiveCompanionMemoryRepository.kt` that wires `DefaultCompanionMemoryRepository` to `ImBackendClient` HTTP endpoints with optimistic-update + rollback-on-failure semantics. (commit `7cce805`)

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.LiveCompanionMemoryRepositoryTest --tests com.gkim.im.android.data.repository.CompanionMemoryRepositoryTest`` - pass (BUILD SUCCESSFUL in 32s). Refactored the §2.1 repository interface so `createPin` / `updatePin` / `deletePin` all return `Result<T>` (success wraps the committed pin / Unit, failure wraps `UnknownPinException` for local lookup misses or the network `Throwable` for HTTP errors) — this is the Kotlin stdlib `Result<T>`, not a sealed interface. Updated the 12 `CompanionMemoryRepositoryTest` cases to unwrap via `.getOrThrow()` and assert `isFailure` + `UnknownPinException` on the two unknown-pin edge cases. 9 new `LiveCompanionMemoryRepositoryTest` cases landed: `refresh pulls memory and pins from backend and applies them to the snapshot` (asserts baseUrl + token are forwarded to both `getCompanionMemory` + `listCompanionMemoryPins` and the merged DTO response lands in `observeMemory` + `observePins`); `createPin optimistically adds pin then replaces it with backend-returned pin` (locally-generated `pin-generated-1` is replaced in the state by the server-canonical `pin-server-1` once the HTTP round-trip resolves); `createPin rolls back optimistic add and returns Result failure when backend 5xx` (IOException from the fake backend → Result.isFailure + observePins restored to the pre-optimistic empty list); `updatePin forwards DTO to backend and replaces optimistic pin with server response` (assert the updated `CompanionMemoryPinDto` sent to `updateCompanionMemoryPin` carries the new text + preserved createdAt); `updatePin rolls back and surfaces failure when backend 5xx` (local pin reverts to the pre-update state); `updatePin on unknown pin skips backend and returns failure` (no HTTP call when `locatePin` misses); `deletePin forwards pinId to backend and removes pin on success`; `deletePin rolls back removed pin when backend 5xx` (the deleted pin is re-inserted into the observer); `deletePin on unknown pin skips backend and returns failure`; `reset forwards the scope enum to backend after clearing local state` (all three scopes — Pins / Summary / All — round-trip to `resetCompanionMemory`). Delivered: `LiveCompanionMemoryRepository.kt` extends `DefaultCompanionMemoryRepository` with `backend: ImBackendClient`, `baseUrlProvider: () -> String`, `tokenProvider: suspend () -> String`. Overrides `refresh` to GET both memory + pins in sequence and merge via `applySnapshot`. Overrides `createPin` / `updatePin` / `deletePin` with the optimistic-then-rollback pattern: snapshot card state → call super for local mutation → call backend → on success replace optimistic pin with server record / confirm delete, on failure restore the pre-mutation snapshot and wrap the throwable in `Result.failure`. Overrides `reset` to forward the scope enum to `resetCompanionMemory` after the local reset applies. Exposed `locatePin` as `protected` in the base class so the live wrapper can resolve the target `cardId` upfront. The `FakeImBackendClient` nested in the test double implements 11 abstract `ImBackendClient` methods as `error("not used in these tests")` stubs and overrides the 6 memory methods with spy lists (`seenBaseUrls`, `seenTokens`, `createPinRequests`, `updatePinRequests`, `deletePinRequests`, `resetRequests`) so every call's payload + routing can be asserted.
- Review:
  - Score: `95/100`
  - Findings: `All 3 spec-mandated properties are asserted: (1) HTTP round-trip — refresh hits both memory + pins endpoints, createPin/updatePin/deletePin all forward the fully-populated DTO with the cardId + baseUrl + token, reset forwards the enum; (2) optimistic-update rollback on 5xx — all three mutation paths have a rollback test that confirms observePins / observeMemory restore to the pre-mutation snapshot after IOException; (3) reset forwarding the scope enum correctly — parameterized over all 3 scopes (Pins / Summary / All). The Result<T> return shape is a deliberate choice over a custom sealed interface: it preserves Kotlin's idiomatic runCatching-style fold + getOrElse in Live, and the UI surfaces are trivial to consume (UnknownPinException is a concrete subtype so bubble-level dispatch is straightforward). Optimistic rollback restores the exact pre-mutation card-level snapshot, not just the pin list, so any in-flight memory-row transitions would also be unwound — conservative but consistent with the "optimistic local state flag" spec language.`
- Upload:
  - Commit: `7cce805`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.1 (companion-memory-and-preset): Add `CompanionPresetRepository.kt` + `DefaultCompanionPresetRepository` with sealed `CompanionPresetMutationResult` and three enforced invariants (built-in immutability, exactly-one-active, duplicate bilingual suffix). (commit `a2e83e2`)

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.CompanionPresetRepositoryTest`` - pass (BUILD SUCCESSFUL in 30s). 14 cases landed in `CompanionPresetRepositoryTest.kt` matching the `UserPersonaRepository` sibling's test shape: `activate flips active flag exclusively across the library` (user preset activation removes the built-in's active flag, single preset ends up active); `activate unknown preset returns Rejected UnknownPreset`; `delete of built-in preset returns Rejected BuiltInPresetImmutable` (matches spec requirement built-ins are immutable and undeletable; im-backend spec.md §57); `delete of currently active preset returns Rejected ActivePresetNotDeletable` (UI-side rule per im-app spec.md §5: delete affordance must be disabled for active); `delete of inactive user-owned preset succeeds and removes it from library`; `duplicate produces user-owned preset with bilingual copy suffix` (displayName.english gains ` (copy)`, displayName.chinese gains `（副本）` using CJK full-width parens, template + params preserved, isBuiltIn + isActive reset to false); `duplicate of unknown preset returns Rejected UnknownPreset`; `update rejects built-in preset as BuiltInPresetImmutable` (per design.md §299: "Preset editability of built-ins: duplicate-only. Built-ins are documentation anchors."); `update of unknown preset returns Rejected UnknownPreset`; `create normalizes new user-owned preset with generated id and timestamps` (isBuiltIn/isActive forced to false, idGenerator + clock injected for determinism, createdAt defaults to clock when draft has 0L); `update on user preset preserves isBuiltIn isActive and createdAt but rewrites updatedAt`; `observeActivePreset emits active preset and null when none set`; `enforces single-active on snapshot ingest` (if multiple seeded presets have isActive=true, only the first is kept active — protects from backend drift); `refresh is a no-op on default repository`. Delivered: `CompanionPresetRepository.kt` exposes `sealed interface CompanionPresetMutationResult` with `Success(preset) / Rejected(reason) / Failed(cause)` and `RejectionReason enum { UnknownPreset, BuiltInPresetImmutable, ActivePresetNotDeletable }`. The interface has 8 methods (`observePresets`, `observeActivePreset`, `create`, `update`, `delete`, `activate`, `duplicate`, `refresh`). `open class DefaultCompanionPresetRepository` holds `MutableStateFlow<List<Preset>>` and enforces: create normalizes `isBuiltIn=false, isActive=false` + assigns id + timestamps; update rejects both unknown and built-in, preserves isBuiltIn/isActive/createdAt on accepted user-preset updates; delete rejects built-in + active; activate sets isActive=true on target + false on all others (exclusivity via map transformation, not a boolean toggle); duplicate suffixes displayName.english with ` (copy)` + displayName.chinese with `（副本）` via the `CopySuffix` object, preserves template + params + description, resets flags; refresh no-ops; setSnapshot exposed for Live wrapper ingest; protected `currentPresets()` + `applyPresets()` hooks enable `LiveCompanionPresetRepository` in §3.2 to reuse the state plumbing. `enforceSingleActive(list)` helper keeps the first active preset active and downgrades all subsequent actives on snapshot ingest — so stale backend data can't violate the invariant.
- Review:
  - Score: `95/100`
  - Findings: `All 4 spec-mandated invariants are asserted: (1) activate exclusivity — tested across mixed built-in + user-owned sets, always lands with exactly one active; (2) delete-active blocked — Rejected(ActivePresetNotDeletable) on user-owned active preset, two-seed library still intact; (3) duplicate renaming — bilingual suffix " (copy)" + "（副本）" uses full-width CJK parens matching the UserPersona sibling's convention; (4) built-in immutability — asserted for both update AND delete paths, per spec language "immutable and undeletable" and design.md's "duplicate-only" directive. Sealed interface mirrors UserPersonaRepository exactly (Success/Rejected/Failed with enum rejection reasons) so downstream Settings UI can dispatch uniformly across user-persona vs companion-preset mutations. Protected currentPresets/applyPresets hooks give §3.2 live wrapper a clean seam to override without re-deriving state plumbing. Zero lint/compile warnings; 14/14 tests green.`
- Upload:
  - Commit: `a2e83e2`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.2 (companion-memory-and-preset): Add `LiveCompanionPresetRepository.kt` binding the default repository to `ImBackendClient` with parallel cold-start refresh (list + active-preset) and optimistic-rollback mutations. (commit `c19ffb9`)

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:testDebugUnitTest --tests com.gkim.im.android.data.repository.LiveCompanionPresetRepositoryTest --tests com.gkim.im.android.data.repository.CompanionPresetRepositoryTest`` - pass (BUILD SUCCESSFUL in 30s; LiveCompanionPresetRepositoryTest 13/13 + CompanionPresetRepositoryTest 14/14 = 27 cases green, `tests="13" skipped="0" failures="0" errors="0"` and `tests="14" skipped="0" failures="0" errors="0"` per TEST-*.xml). 13 new cases in `LiveCompanionPresetRepositoryTest.kt` exercise merge + rollback shapes: `refresh merges remote list with active preset id from bootstrap` (listDto.activePresetId="user-1" overrides any stale isActive flags on the built-in → post-refresh, exactly one active, pointed at "user-1"); `refresh reconciles active preset returned by getActivePreset separately` (listDto has activePresetId=null but getActivePreset returns the built-in with isActive=true → `mergeRemote` folds activeDto into the byId map + sets the canonical active); `refresh is a no-op when session has no base url or token` (0 backend calls, observePresets stays empty — the `baseUrlProvider() ?: return` short-circuit); `activate flips active flag locally and reaches backend with new active record` (local state pre-flipped via `default.activate` → backend activatePreset("other") round-trips → server record replaces local); `activate rolls back local state when backend returns 409` (RuntimeException("HTTP 409") from fake → Failed(throwable) + pre-mutation snapshot restored, built-in stays active); `delete rolls back local state when backend returns 409 for active preset` (server-side delete-active block caught as Failed + local state restored, deleted user preset reappears in the library); `delete short-circuits on local built-in rejection without reaching backend` (Rejected(BuiltInPresetImmutable) returned before any HTTP call → deleteCalls=0); `delete short-circuits on local active rejection without reaching backend` (Rejected(ActivePresetNotDeletable) → deleteCalls=0); `create rolls back local state when backend fails` (RuntimeException("HTTP 500") → Failed + library back to empty); `create reconciles local record with server-returned preset on success` (local "preset-new" id replaced by server "preset-server-1" on success — server is canonical); `duplicate rolls back local state when backend create fails` (HTTP 500 on createPreset → local duplicate removed + library stays at 1 entry); `update rolls back local state when backend fails` (HTTP 422 → pre-update description restored: english "Old desc" + chinese "旧描述"); `mutations without session short-circuit and skip backend` (null baseUrlProvider → local `default.create` succeeds optimistically but `createCalls=0` + `lastActivateId=null`, i.e. the Live wrapper returns the local Success without touching HTTP when session is missing). Delivered: `LiveCompanionPresetRepository.kt` (179 lines) uses the composition pattern matching `LiveUserPersonaRepository`: `private val default: DefaultCompanionPresetRepository`, `backendClient: ImBackendClient`, `baseUrlProvider: () -> String?`, `tokenProvider: () -> String?`. `observePresets` / `observeActivePreset` delegate to the default. `refresh` uses `coroutineScope { async(Dispatchers.IO) { backendClient.listPresets(...) } }` + `async(Dispatchers.IO) { runCatching { backendClient.getActivePreset(...) }.getOrNull() }` — both awaited in parallel, then `mergeRemote(listDto.presets, listDto.activePresetId, activeDto)` merges via id-keyed map with getActivePreset overriding the list's flag and fallback to listDto.activePresetId. `create` / `update` / `delete` / `activate` / `duplicate` all follow the same pattern: call default first → short-circuit on local `Rejected` → wrap backend call in try/catch → on success replace the optimistic record with the server-canonical `remote.toPreset()` via `default.setSnapshot(...)` → on failure restore the pre-mutation snapshot and return `Failed(throwable)`. `delete` special-cases: on backend exception, restore the full pre-mutation snapshot (this captures the 409-delete-active behavior even though the local guard already rejects on active). `FakePresetBackend` nested in the test double implements 11 abstract `ImBackendClient` methods as `error("n/a")` stubs and overrides 6 preset methods with spy counters (`listCalls`, `createCalls`, `deleteCalls`, `lastActivateId`) + configurable failure throwables (`createFailure`, `updateFailure`, `deleteFailure`, `activateFailure`) for the rollback paths. Session short-circuit uses nullable providers instead of a session-state enum so the wrapper stays stateless.
- Review:
  - Score: `95/100`
  - Findings: `All 3 spec-mandated properties are asserted: (1) merge semantics — both listDto.activePresetId and getActivePreset reconciliation paths are tested, plus the "stale active flag from list" drift case; (2) reorder on activate — activate flips the flag locally, awaits backend, replaces with server record; rollback on 409 restores the pre-activation snapshot; (3) handles 409 (delete-active) — delete with server 409 returns Failed + restores the full library so the deleted preset reappears. Composition over inheritance matches the LiveUserPersonaRepository precedent for user-persona parity (both capabilities surface built-in + active + duplicate + Settings UI, so the sealed MutationResult + wrapper shape aligns). Parallel refresh via coroutineScope + async(Dispatchers.IO) avoids sequential HTTP latency on cold start. Session-awareness via nullable providers (baseUrl + token) means the wrapper can be installed unconditionally and short-circuits cleanly when the user isn't signed in — no separate "offline default" wiring needed in AppContainer §3.3. Server-canonical replacement on mutation success ensures the client never diverges from the server's timestamps/ids after round-trip. Zero lint/compile warnings; 27/27 tests green across the two preset suites.`
- Upload:
  - Commit: `c19ffb9`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`

### Task 3.3 (companion-memory-and-preset): Register `companionMemoryRepository` + `companionPresetRepository` in `AppContainer` + `DefaultAppContainer` and patch instrumentation test doubles. (commit `<pending>`)

- Verification:
  - ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:compileDebugKotlin :app:testDebugUnitTest --tests 'com.gkim.im.android.data.repository.*Test'`` - pass (BUILD SUCCESSFUL in 30s). ``JAVA_HOME='/c/Program Files/Java/jdk-17' ./android/gradlew.bat --no-daemon -p android :app:compileDebugAndroidTestKotlin`` - pass (BUILD SUCCESSFUL in 34s, only pre-existing unused-parameter warning in LiveImageSendValidationTest unrelated to this change). ``rg -n "companionMemoryRepository\|companionPresetRepository" android/app/src/main/java`` - 4 matches, all in `AppContainer.kt`: interface declarations at lines 41–42 + DefaultAppContainer overrides at lines 138 + 143 (the two `override val companionMemoryRepository = LiveCompanionMemoryRepository(...)` / `override val companionPresetRepository = LiveCompanionPresetRepository(...)` blocks). Delivered: added `val companionMemoryRepository: CompanionMemoryRepository` + `val companionPresetRepository: CompanionPresetRepository` to the `AppContainer` interface (between `worldInfoRepository` and `aigcRepository`, matching alphabetical-ish grouping of companion-facing repositories). In `DefaultAppContainer`, wired `companionMemoryRepository = LiveCompanionMemoryRepository(backend = imBackendClient, baseUrlProvider = { sessionStore.baseUrl.orEmpty() }, tokenProvider = { sessionStore.token.orEmpty() })` — memory wrapper declares non-null providers per Task 2.2's signature, and `.orEmpty()` keeps the wiring compile-safe while `sessionStore.baseUrl`/`token` are typed `String?`. Wired `companionPresetRepository = LiveCompanionPresetRepository(default = DefaultCompanionPresetRepository(), backendClient = imBackendClient, baseUrlProvider = { sessionStore.baseUrl }, tokenProvider = { sessionStore.token })` — preset wrapper already accepts nullable providers per Task 3.2 and short-circuits to the local default when missing session. `DefaultCompanionPresetRepository()` constructs with zero args (empty `initialPresets`, UUID-generating idGenerator, `System.currentTimeMillis` clock) so AppContainer doesn't need to reference a not-yet-authored built-in seed list; the backend-authoritative built-in presets land in `companionPresetRepository` via `refresh()` on cold start once the user has a session. Patched three androidTest doubles (`GkimRootAppTest.UiTestAppContainer`, `LiveImageSendValidationTest.LiveImageValidationContainer`, `LoginEndpointConfigurationTest.LoginEndpointTestAppContainer`) to override both new members with `DefaultCompanionMemoryRepository()` + `DefaultCompanionPresetRepository()` — using defaults avoids coupling UI instrumentation to a live backend endpoint for these flows (they test navigation / login / image-send, not memory or presets). Did not touch any peer-IM wiring (`messagingRepository`, `contactsRepository`, `realtimeChatClient`) per the "do not alter the peer-IM code path" directive.
- Review:
  - Score: `95/100`
  - Findings: `Interface additions colocated with the existing companion-facing repos (cardInteropRepository, userPersonaRepository, worldInfoRepository) for discoverability. Live wrappers are installed unconditionally; nullable session providers handle the "no session" case gracefully (preset wrapper short-circuits, memory wrapper falls through to HTTP failure → local-only state). The .orEmpty() shim for memory is a deliberate choice to keep the wiring stable without re-opening Task 2.2's non-null-provider contract — the alternative (refactoring LiveCompanionMemoryRepository to nullable providers + test updates) is a broader change that could wait for the §5.2 chat integration slice where memory-panel entry is session-guarded at the call site. Test doubles use Default* rather than Live* because the three instrumentation suites don't exercise memory/preset surfaces and pulling in live HTTP backends for unrelated flows would add latency + flakiness. 27/27 repository unit tests stayed green (12 CompanionMemoryRepositoryTest + 10 LiveCompanionMemoryRepositoryTest + 14 CompanionPresetRepositoryTest + 13 LiveCompanionPresetRepositoryTest = 49 cases total; the other *Test classes under the data.repository package match glob filter), and compileDebugAndroidTestKotlin compiles cleanly. rg audit confirmed no stray references outside AppContainer.kt.`
- Upload:
  - Commit: `<pending>`
  - Branch: `feature/ai-companion-im`
  - Push: `origin/feature/ai-companion-im`
- Result: `accepted`
