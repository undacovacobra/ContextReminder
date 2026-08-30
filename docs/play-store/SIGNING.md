# Cue Play Upload Signing

Cue's Play release must use one persistent upload key. Google Play App Signing can hold the app-signing key while the developer uses this upload key to authenticate future AAB uploads.

## 1. Create the upload key once

Run this on a trusted computer with Java installed:

```bash
keytool -genkeypair -v \
  -keystore cue-upload-key.jks \
  -alias cue-upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Choose strong passwords. Keep the `.jks`, store password, key alias, and key password in a secure password manager/offline backup. Losing the upload key creates avoidable recovery work.

**Never commit `cue-upload-key.jks` or any signing password to GitHub.**

## 2. Encode the keystore for GitHub Actions

macOS/Linux:

```bash
base64 < cue-upload-key.jks | tr -d '\n'
```

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("cue-upload-key.jks"))
```

## 3. Add repository Actions secrets

In the GitHub repository, add these Actions secrets:

- `CUE_UPLOAD_KEYSTORE_BASE64` = the complete one-line Base64 string
- `CUE_UPLOAD_STORE_PASSWORD` = keystore password
- `CUE_UPLOAD_KEY_ALIAS` = `cue-upload` (or the alias you chose)
- `CUE_UPLOAD_KEY_PASSWORD` = key password

The workflow reconstructs the keystore only inside the temporary GitHub Actions runner. The file is not committed or uploaded as a separate artifact.

## 4. How the Gradle build behaves

`app/build.gradle.kts` reads these environment variables:

- `CUE_UPLOAD_STORE_FILE`
- `CUE_UPLOAD_STORE_PASSWORD`
- `CUE_UPLOAD_KEY_ALIAS`
- `CUE_UPLOAD_KEY_PASSWORD`

When all four are present, the `release` build is signed with that upload key. When they are absent, Gradle can still produce an **unsigned AAB for CI validation**, but that unsigned bundle is not the one to upload to Google Play.

## 5. First Play upload

Create Cue in Play Console using package name:

`com.contextreminder.app`

Enroll in Play App Signing during the first-release flow. Upload the signed `app-release.aab` created by the branch workflow after the GitHub secrets above are configured.

## 6. Future releases

Never generate a new upload key for routine updates. Increment `versionCode`, build from the release branch, and use the same four GitHub secrets so Play recognizes the update as coming from the same developer upload identity.
