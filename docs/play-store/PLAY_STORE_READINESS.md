# Cue Play Store Readiness

This branch is the Play Store hardening track for Cue. It is intentionally separate from `main`.

## Technical baseline

- App name: Cue
- Application ID: `com.contextreminder.app`
- minSdk: 29
- compileSdk: 36
- targetSdk: 36
- Play release branch version: 1.4.0 / versionCode 5
- Distribution format: Android App Bundle (`.aab`)
- Data model: local-only reminder rules; no Cue account/backend/ads/analytics

Google Play requires new apps and updates to target Android 16 / API 36 starting August 31, 2026, so Cue's current target is aligned with that requirement.

## Sensitive-access posture

### Location
Background location remains because it is essential to the core arrive/leave reminder feature. Cue shows a dedicated disclosure before the user is directed to Android background-location settings. Play Console still requires the Background Location permissions declaration and reviewer video.

### AccessibilityService
Cue is not an accessibility tool. The service is narrowly used for user-created app-open reminders, listens only to window state/window change events, sets `canRetrieveWindowContent=false`, performs no automated actions, and has a dedicated affirmative disclosure before Accessibility settings. Play Console still requires the AccessibilityService declaration/review.

### Contacts
The Play branch removes `READ_CONTACTS`. Caller reminders use Android's user-initiated contact picker or manual phone-number entry.

### Notifications
Notification-listener access is optional and only needed for notification-trigger rules. Cue explains this before the user opens Android Notification Access settings.

### Calls
Call-screening role is optional and only needed for caller-trigger reminders. Cue does not block or answer calls.

### Overlay
Display-over-other-apps access is optional and only used to keep the caller reminder banner visible during a phone call.

## Privacy policy

Policy source: `docs/privacy-policy.html`

Intended GitHub Pages URL after Pages is enabled from `/docs`:
`https://undacovacobra.github.io/ContextReminder/privacy-policy.html`

The same privacy/data-access explanations are available inside Cue from Setup > Privacy & data access.

## Items that still require Play Console/account action

These cannot be completed by source-code changes alone:

1. Create/verify a Google Play Developer account and complete any identity/device verification shown by Play Console.
2. Create the Cue app record using package `com.contextreminder.app`.
3. Enroll in Play App Signing (new apps are normally enrolled during first release setup).
4. Create a persistent upload key and add its encoded keystore/passwords as GitHub Actions secrets described in `SIGNING.md`.
5. Enable GitHub Pages or host `docs/privacy-policy.html` at another active public non-PDF URL, then put the same URL in Cue's Play listing.
6. Complete Data Safety using `DATA_SAFETY.md`, rechecking against the final binary and Play's current questionnaire.
7. Complete the AccessibilityService declaration using `ACCESSIBILITY_DECLARATION.md`.
8. Complete the Background Location declaration and upload the reviewer video described in `BACKGROUND_LOCATION_DECLARATION.md`.
9. Complete content rating, target audience, ads declaration (`No ads`), app access, and other App content forms presented by Play Console.
10. Upload screenshots/feature graphic described in `STORE_LISTING.md`.
11. Run internal/closed testing. If this is a personal developer account created after November 13, 2023, current Play rules require at least 12 testers continuously opted into a closed test for 14 days before applying for production access.

## Release policy

Do not merge upload keys or passwords into Git. Do not merge this branch into `main` until its disclosure UX has been physically tested on Android and the Play policy declarations match the final shipped behavior.
