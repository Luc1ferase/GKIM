# Android Scaffold Notes

## Local paths on this workstation

- Android SDK: `D:\Android\Sdk`
- Recommended AVD home: `D:\Android\Avd`
- Recommended Gradle user home: `D:\Gradle`

## Local environment variables

Use these when setting up Android Studio or command-line builds on this machine:

```powershell
$env:ANDROID_SDK_ROOT = 'D:\Android\Sdk'
$env:ANDROID_HOME = 'D:\Android\Sdk'
$env:ANDROID_AVD_HOME = 'D:\Android\Avd'
$env:GRADLE_USER_HOME = 'D:\Gradle'
```

`android/local.properties` already points the project at `D:\Android\Sdk`.

## Verification targets

Once Gradle wrapper and Android SDK command-line tooling are available, run the scaffold verification commands from `android/`:

```powershell
.\gradlew.bat test
.\gradlew.bat connectedDebugAndroidTest
```

## GitHub tag release automation

The repository's Android release automation is designed around GitHub version tags.

### Supported tag format

- `vMAJOR.MINOR.PATCH`
- Example: `v0.2.0`

### Expected GitHub repository secrets

The tag-release workflow expects these repository secrets to exist before a signed APK can be published:

- `ANDROID_RELEASE_KEYSTORE_BASE64`
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

The workflow reconstructs the keystore at runtime on the GitHub runner. Do not commit a release keystore, keystore passwords, or any filled signing-properties file into this repository.

### Gradle inputs consumed by CI

The Android app build now accepts release metadata and signing inputs through Gradle properties or environment variables:

- `GKIM_RELEASE_VERSION_NAME`
- `GKIM_RELEASE_VERSION_CODE`
- `GKIM_RELEASE_STORE_FILE`
- `GKIM_RELEASE_STORE_PASSWORD`
- `GKIM_RELEASE_KEY_ALIAS`
- `GKIM_RELEASE_KEY_PASSWORD`

If the full signing input set is not supplied, `assembleRelease` can still fall back to an unsigned release build locally, but the GitHub tag workflow is expected to fail before publication instead of shipping an unsigned APK as a release artifact.

### Published asset naming

Successful GitHub tag builds publish an APK named:

- `gkim-android-vMAJOR.MINOR.PATCH.apk`

### Focused local verification

Before relying on the GitHub workflow, you can locally verify the tag-driven version injection path from `android/`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="${env:JAVA_HOME}\bin;${env:Path}"
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease "-PGKIM_RELEASE_VERSION_NAME=0.2.0" "-PGKIM_RELEASE_VERSION_CODE=2000"
```

### Remote rehearsal without signing secrets

If the repository secrets are not configured yet, you can still prove the GitHub-hosted build path by pushing a disposable version tag such as `v0.2.17`.

The expected outcome is:

- the workflow validates the tag and derives release metadata
- `:app:testDebugUnitTest` passes on GitHub
- `:app:assembleRelease` produces the release APK on GitHub
- artifact upload and GitHub Release publication stay blocked by the final `Missing ANDROID_RELEASE_* secrets` guard until all four signing secrets are configured

This is the intended rehearsal path for CI validation before the repository is trusted with the real Android release keystore.

## Live IM Wiring Handoff

The Android shell now boots the IM feature through the live backend repository path instead of the seed-only in-memory repository.

### Runtime configuration

1. Launch the app and open `Settings`.
2. Open `IM VALIDATION` and review:
   - `Resolved backend origin`: the current HTTP base origin the app will use
   - `Derived realtime endpoint`: the WebSocket URL derived automatically from that same origin
3. If you need to validate against a non-default environment in a debug build, tap `Show developer validation controls` and set:
   - `Developer backend origin`: one reachable backend origin, for example `https://chat.lastxuans.sbs/` for the accepted remote deployment or `http://10.0.2.2:18080/` for emulator-to-host validation
   - `Validation user`: a backend user external ID such as `nox-dev`
4. Return to `Messages` and confirm the status card shows a live IM phase or a concrete failure message.

The app now derives the realtime endpoint from the selected backend origin, so operators only manage one IM address instead of separate HTTP and WebSocket fields. The shipped default backend origin comes from `BuildConfig.IM_BACKEND_ORIGIN`, which can be overridden at build time with `GKIM_IM_BACKEND_ORIGIN`.

### What is wired now

- App startup issues a dev session, loads bootstrap data, and connects the authenticated WebSocket gateway through `LiveMessagingRepository`.
- `Messages` shows live integration status instead of silently implying seed-only success.
- Opening a backend-backed chat now triggers message-history hydration for that conversation.
- On `feature/ai-companion-im`, the third primary tab is a tavern-style `酒馆` surface for preset角色、抽卡 and owned-roster activation rather than the old `Space` feed.

### Known scope limits before device validation

- Full adb/SSH/physical-device validation is still a separate change; this slice stops at app-to-backend wiring plus layered regression coverage.
- Conversations created from `Contacts` can still fall back to local room creation if the backend has not provisioned a matching thread yet.
- PostgreSQL host, password, and optional trust material remain backend-only inputs. The Android app should only receive HTTP and WebSocket endpoints, never database secrets.

### Layered regression commands for this wiring change

Run these from `android/` before handing the app off for broader validation:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="${env:JAVA_HOME}\bin;${env:Path}"
.\gradlew.bat :app:testDebugUnitTest --tests com.gkim.im.android.data.remote.im.ImBackendHttpClientTest --tests com.gkim.im.android.data.remote.realtime.RealtimeChatClientTest --tests com.gkim.im.android.data.repository.LiveMessagingRepositoryTest --tests com.gkim.im.android.feature.messages.MessagesViewModelTest --rerun-tasks
```

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
$env:Path="${env:JAVA_HOME}\bin;D:\Android\Sdk\platform-tools;D:\Android\Sdk\emulator;${env:Path}"
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#settingsScreenExposesImBackendValidationControlsAndStatus" --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.GkimRootAppTest#openingConversationRequestsLiveHistoryLoad" --rerun-tasks
```

## Remote IM Validation Workflow

Use this workflow when validating the app against the deployed Ubuntu backend instead of the local
Docker-host path.

### 1. Verify the published backend endpoints

The current accepted remote endpoints are:

- HTTP: `https://chat.lastxuans.sbs/`
- WebSocket: `wss://chat.lastxuans.sbs/ws`

Before launching the app, confirm the published HTTP endpoint responds:

```powershell
Invoke-RestMethod https://chat.lastxuans.sbs/health
```

### 2. Point the app at the deployed backend

Open `Settings > IM VALIDATION` in the app and confirm the resolved backend origin is `https://chat.lastxuans.sbs/`.

If the app is currently pointed elsewhere in a debug build, tap `Show developer validation controls` and enter:

- `Developer backend origin`: `https://chat.lastxuans.sbs/`
- `Validation user`: `nox-dev`

No `adb reverse` step is required for this deployed-server flow.

### 3. Run the focused live validation case

From `android/`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="${env:JAVA_HOME}\bin;C:\Users\Nox\AppData\Local\Android\Sdk\platform-tools;${env:Path}"
.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest `
  "-Pandroid.testInstrumentationRunnerArguments.class=com.gkim.im.android.feature.navigation.LiveImBackendValidationTest#emulatorValidationCoversLiveRoundTripAndReloadRecovery" `
  "-Pandroid.testInstrumentationRunnerArguments.liveImBackendOrigin=https://chat.lastxuans.sbs/" `
  --stacktrace
```

This case covers session issuance, bootstrap/history hydration, realtime send/receive, and
recovery after reconnect or relaunch against the deployed backend.

## Local Emulator IM Validation Workflow

Use this workflow when validating against a locally containerized backend on the workstation. The
existing Android emulator exercises the live API suite through host port `18080`.

The backend source is no longer published in the public repository tip. Before using this local
workflow, restore the maintainer-private backend checkout under `.private/backend/` or another
private path on the workstation.

### 1. Start the local backend container

From your private/local backend checkout, for example `.private/backend/`:

```powershell
Copy-Item .env.example .env.local
# Fill .env.local with the real backend-only PostgreSQL values before continuing.
docker build -t gkim-im-backend:local .
docker run --rm -d `
  --name gkim-im-backend-local `
  --env-file .env.local `
  -e APP_BIND_ADDR=0.0.0.0:8080 `
  -p 18080:8080 `
  gkim-im-backend:local
Invoke-WebRequest http://127.0.0.1:18080/health | Select-Object -ExpandProperty Content
```

### 2. Point the emulator at the host backend

If you want the emulator to hit the host through `adb reverse`, run:

```powershell
& 'D:\Android\Sdk\platform-tools\adb.exe' devices -l
& 'D:\Android\Sdk\platform-tools\adb.exe' reverse tcp:18080 tcp:18080
& 'D:\Android\Sdk\platform-tools\adb.exe' reverse --list
```

Then open `Settings > IM VALIDATION`, tap `Show developer validation controls`, and set one developer backend origin:

- `http://127.0.0.1:18080/` when you are using `adb reverse`
- `http://10.0.2.2:18080/` when you want the emulator to reach the host bridge directly

The app will derive `/ws` automatically from whichever origin you enter.

### 3. Capture emulator-facing evidence

Use these commands while running the validation flow:

```powershell
& 'D:\Android\Sdk\platform-tools\adb.exe' logcat -c
& 'D:\Android\Sdk\platform-tools\adb.exe' shell am start -n com.gkim.im.android/.MainActivity
```

After the validation run:

```powershell
& 'D:\Android\Sdk\platform-tools\adb.exe' logcat -d > android\emulator-im-validation.log
& 'D:\Android\Sdk\platform-tools\adb.exe' exec-out screencap -p > android\emulator-im-validation.png
docker logs gkim-im-backend-local > android\docker-im-validation.log
```

### 4. Expected validation surface

- `Messages` should bootstrap real conversation rows instead of seed-only rows.
- Opening a chat should load backend history.
- Sending a message should surface live status updates or explicit integration failures.
- Reconnect/relaunch should recover from backend bootstrap/history instead of resetting to placeholder-only success.
