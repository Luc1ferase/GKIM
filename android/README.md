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
