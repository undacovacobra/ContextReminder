# Cue Play Store Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the existing Cue Android app technically and policy-ready for Google Play submission while preserving the current feature set on an isolated `play-store-release` branch.

**Architecture:** Keep the existing local-only rule engine and Android services. Minimize sensitive permissions, add feature-specific prominent disclosures immediately before sensitive-access setup, expose an in-app privacy page, and add a release-AAB CI path that signs only when secure GitHub Actions secrets are supplied. Store Play Console declarations and listing copy alongside the code so review material stays synchronized with behavior.

**Tech Stack:** Kotlin, Jetpack Compose, Android API 36, Google Play services location/geofencing, GitHub Actions, Android App Bundle.

**Spec:** `docs/play-store/PLAY_STORE_READINESS.md`

## Global Constraints

- Work only on branch `play-store-release`; do not change `main`.
- Keep `targetSdk = 36` and `compileSdk = 36`.
- Keep reminder data local to the device; do not add analytics, ads, accounts, or cloud synchronization.
- Do not commit signing keys or signing passwords.
- Background location remains only for user-created arrival/departure reminders.
- AccessibilityService remains only for detecting when a user-selected app enters the foreground; it performs no automated clicks, gestures, or UI manipulation.
- Notification access remains only for user-created notification-trigger rules.
- Call screening remains only for matching incoming numbers to user-created caller reminders; Cue does not block calls.

---

### Task 1: Minimize permissions and document data access

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/contextreminder/app/SetupScreen.kt`
- Create: `docs/play-store/DATA_SAFETY.md`

**Interfaces:**
- Consumes: existing legacy `ACTION_PICK` contact-selection flow in `AddRuleScreen.kt`.
- Produces: manifest without `READ_CONTACTS`; Setup no longer asks for broad contacts access.

- [ ] Remove `android.permission.READ_CONTACTS` from the manifest.
- [ ] Remove the contacts runtime-permission launcher/status card from Setup.
- [ ] Keep the contact picker itself; it grants temporary URI access to the selected number.
- [ ] Disable Android backup so local reminder data is not copied to cloud backup by Cue.
- [ ] Document each accessed data category and whether it leaves the device.
- [ ] Build to prove the picker code compiles without broad contacts permission.

### Task 2: Add policy-compliant prominent disclosures

**Files:**
- Create: `app/src/main/java/com/contextreminder/app/SensitiveAccessDisclosure.kt`
- Modify: `app/src/main/java/com/contextreminder/app/SetupScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `SensitiveAccessKind` and `SensitiveAccessDisclosureDialog(kind, onContinue, onCancel)`.
- Setup invokes the disclosure before opening the relevant Android permission/settings screen.

- [ ] Add a standalone background-location disclosure containing the words `location`, `background`, and `when the app is closed`, explaining that location stays on-device and is used only for user-created place reminders.
- [ ] Add a separate Accessibility disclosure that describes foreground app/package detection, states it is used only to trigger user-created app-open reminders, states that Cue does not read screen text or perform taps/gestures, and requires affirmative Continue consent before opening Accessibility settings.
- [ ] Add separate concise disclosures for notification access, call-screening role, and overlay access so users understand the requested access before leaving Cue.
- [ ] Make Setup cards say `Cue` rather than the old product name.
- [ ] Build and inspect strings/resources.

### Task 3: Add in-app privacy and Play information

**Files:**
- Create: `app/src/main/java/com/contextreminder/app/PrivacyScreen.kt`
- Modify: `app/src/main/java/com/contextreminder/app/MainActivity.kt`
- Create: `docs/privacy-policy.html`
- Create: `docs/index.html`
- Create: `docs/play-store/PLAY_STORE_READINESS.md`

**Interfaces:**
- Produces: `PrivacyScreen(onBack)` reachable from Setup.
- Public policy URL target: `https://undacovacobra.github.io/ContextReminder/privacy-policy.html` after GitHub Pages is enabled for `/docs` or the included Pages workflow is enabled.

- [ ] Add Privacy & data access entry inside Setup.
- [ ] Add an in-app privacy screen explaining local storage and every sensitive API used.
- [ ] Add a matching HTML privacy policy suitable for public hosting.
- [ ] Add a simple docs landing page linking the policy.
- [ ] Document the remaining Play Console manual steps and policy declarations.

### Task 4: Prepare Play listing and review declarations

**Files:**
- Create: `docs/play-store/STORE_LISTING.md`
- Create: `docs/play-store/ACCESSIBILITY_DECLARATION.md`
- Create: `docs/play-store/BACKGROUND_LOCATION_DECLARATION.md`
- Create: `docs/play-store/REVIEW_CHECKLIST.md`

**Interfaces:**
- Produces copy that can be pasted into Play Console without inventing new behavior.

- [ ] Draft app title, short description, full description, and screenshot plan.
- [ ] Draft AccessibilityService declaration centered on the single app-open reminder feature.
- [ ] Draft background-location declaration centered on the single arrive/leave reminder feature and a <=30-second reviewer video script.
- [ ] Draft Data Safety answers consistent with the app's local-only behavior.
- [ ] Add reviewer instructions for enabling each trigger type.

### Task 5: Create release build/signing configuration

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `.github/workflows/build-play-release.yml`
- Create: `docs/play-store/SIGNING.md`

**Interfaces:**
- Reads optional environment variables `CUE_UPLOAD_STORE_FILE`, `CUE_UPLOAD_STORE_PASSWORD`, `CUE_UPLOAD_KEY_ALIAS`, `CUE_UPLOAD_KEY_PASSWORD`.
- GitHub Actions reconstructs the keystore from secret `CUE_UPLOAD_KEYSTORE_BASE64` and exports the four signing environment variables.
- Produces `app-release.aab` and release mapping/native debug metadata when applicable.

- [ ] Bump Play branch version to `1.4.0` / versionCode `5`.
- [ ] Add a release signing config only when all signing environment variables are present; otherwise allow an unsigned release bundle for CI validation.
- [ ] Add release minification/resource shrinking only if the existing app compiles cleanly with default rules; otherwise keep disabled for first Play release to reduce release risk.
- [ ] Add a GitHub Actions workflow scoped to `play-store-release` that runs core tests, lint, and `bundleRelease`, and uploads the AAB.
- [ ] If signing secrets are absent, label the artifact clearly as unsigned and document that Play upload requires the persistent upload key.

### Task 6: Final verification

**Files:**
- Review all files changed on `play-store-release`.

**Interfaces:**
- Produces a build artifact and a branch comparison against `main`.

- [ ] Run the core test suite.
- [ ] Run Android lint.
- [ ] Run `:app:bundleRelease`.
- [ ] Confirm the workflow artifact exists.
- [ ] Verify the AAB ZIP structure/integrity after download.
- [ ] Compare `main...play-store-release` and ensure no unrelated main-branch files were changed.
- [ ] Report any remaining Play Console-only steps rather than claiming they were completed in code.
