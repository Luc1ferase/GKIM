# Android Tag Release Operations

Use this flow when you want to turn accepted GKIM Android changes into a tagged GitHub Release with a downloadable APK asset.

## Preconditions

Before you tag a release:

- make sure the Android release signing secrets are configured in GitHub repository settings:
  - `ANDROID_RELEASE_KEYSTORE_BASE64`
  - `ANDROID_RELEASE_STORE_PASSWORD`
  - `ANDROID_RELEASE_KEY_ALIAS`
  - `ANDROID_RELEASE_KEY_PASSWORD`
- make sure `gh auth status` succeeds if you want terminal-based monitoring
- make sure the release commit has already passed the repository's verification/review gate

## Repo-owned release entrypoint

From the repository root, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\release-android-tag.ps1 -TagName v0.2.0
```

The helper performs release preflight before pushing anything. It blocks the release when:

- the worktree is dirty
- the current branch has no upstream
- the current branch is ahead of or behind its upstream
- the requested tag already exists locally or on `origin`

When preflight passes, the helper:

1. pushes the current branch to `origin`
2. creates and pushes the annotated release tag
3. discovers the GitHub Actions run when possible
4. prints the terminal/browser commands needed for monitoring and final asset checks

## Watching the release

If the helper returns a run id, use:

```powershell
gh run watch <run-id> --interval 10 --exit-status
gh run view <run-id>
gh release view v0.2.0 --repo Luc1ferase/GKIM
```

The GitHub workflow now writes a Step Summary that includes:

- the pushed release tag
- the expected APK asset name
- whether signing secrets were configured
- whether publication finished with a verified GitHub Release asset

## Browser fallback

If GitHub CLI is unavailable, use:

- Actions view: `https://github.com/Luc1ferase/GKIM/actions?query=workflow%3A%22Android%20Tag%20Release%22`
- Release page: `https://github.com/Luc1ferase/GKIM/releases/tag/vMAJOR.MINOR.PATCH`

Do not treat the release as complete until the release page actually shows `gkim-android-vMAJOR.MINOR.PATCH.apk`.

## Focused local pre-check

Before pushing a real release tag, you can still validate the Gradle release path locally from `android/`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="${env:JAVA_HOME}\bin;${env:Path}"
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease "-PGKIM_RELEASE_VERSION_NAME=0.2.0" "-PGKIM_RELEASE_VERSION_CODE=2000"
```
