## 1. Android release inputs

- [x] 1.1 Update the Android Gradle release configuration so CI can inject tag-derived `versionName`, numeric `versionCode`, and release signing inputs without committing keystores or passwords.
- [x] 1.2 Add repository-safe documentation or template guidance for the required GitHub repository secrets, expected tag format, and release asset naming contract.

## 2. GitHub tag release workflow

- [x] 2.1 Create a GitHub Actions workflow that triggers on supported version tags, provisions JDK 17 plus the Android SDK/tooling needed for this project, validates the tag format, and prepares Gradle caching.
- [x] 2.2 Run the lightweight release gate in CI, build the Android release APK from the tagged commit, and normalize the final APK filename so it clearly includes the tag version.
- [x] 2.3 Upload the built APK as a workflow artifact and publish it to the GitHub Release associated with the tag while failing cleanly when signing or publication prerequisites are missing.

## 3. Release validation and evidence

- [x] 3.1 Verify the new release path with focused Android build checks and at least one end-to-end tag-driven release rehearsal against GitHub so the published APK path is proven.
- [x] 3.2 Record the verification, review, score, and GitHub upload evidence in `docs/DELIVERY_WORKFLOW.md`, then update any affected release/operator docs before closing the change.
