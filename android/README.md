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

## Live IM Wiring Handoff

The Android shell now boots the IM feature through the live backend repository path instead of the seed-only in-memory repository.

### Runtime configuration

1. Launch the app and open `Settings`.
2. In the `IM VALIDATION` section, set:
   - `HTTP Base URL`: the reachable backend HTTP origin, for example `http://127.0.0.1:18080/` when validating locally or through a tunnel
   - `WebSocket URL`: the reachable gateway URL, for example `ws://127.0.0.1:18080/ws`
   - `Dev User`: a backend user external ID such as `nox-dev`
3. Return to `Messages` and confirm the status card shows a live IM phase or a concrete failure message.

### What is wired now

- App startup issues a dev session, loads bootstrap data, and connects the authenticated WebSocket gateway through `LiveMessagingRepository`.
- `Messages` shows live integration status instead of silently implying seed-only success.
- Opening a backend-backed chat now triggers message-history hydration for that conversation.

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

## Emulator IM Validation Workflow

This is now the primary validation path for the Android IM app. The backend runs locally in Docker, and the existing Android emulator exercises the live API suite through host port `18080`.

### 1. Start the local backend container

From `backend/`:

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

The app defaults already target `http://127.0.0.1:18080/` and `ws://127.0.0.1:18080/ws`, so the shortest path is to reverse the host port into the emulator:

```powershell
& 'D:\Android\Sdk\platform-tools\adb.exe' devices -l
& 'D:\Android\Sdk\platform-tools\adb.exe' reverse tcp:18080 tcp:18080
& 'D:\Android\Sdk\platform-tools\adb.exe' reverse --list
```

If you prefer not to use `adb reverse`, you can instead enter `http://10.0.2.2:18080/` and `ws://10.0.2.2:18080/ws` manually in the app's `IM VALIDATION` settings.

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
docker logs gkim-im-backend-local > backend\docker-im-validation.log
```

### 4. Expected validation surface

- `Messages` should bootstrap real conversation rows instead of seed-only rows.
- Opening a chat should load backend history.
- Sending a message should surface live status updates or explicit integration failures.
- Reconnect/relaunch should recover from backend bootstrap/history instead of resetting to placeholder-only success.
