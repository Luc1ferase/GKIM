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
