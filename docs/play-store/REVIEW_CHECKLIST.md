# Cue Play Review Checklist

## Before uploading a release

- [ ] `targetSdk` is 36 or higher.
- [ ] `READ_CONTACTS` is absent from the merged manifest.
- [ ] `android:allowBackup="false"` is present.
- [ ] Accessibility service still has `canRetrieveWindowContent="false"` and `isAccessibilityTool="false"`.
- [ ] No analytics, advertising, account, or backend SDK has been added.
- [ ] Core tests pass.
- [ ] Android lint passes.
- [ ] Release AAB builds successfully.
- [ ] Release AAB is signed with the persistent upload key before Play upload.

## Physical Android test

- [ ] Place reminder: create an arrival rule, close Cue, enter the geofence, reminder fires.
- [ ] Place reminder: leave transition fires when Cue is closed.
- [ ] Business/place search returns plausible nearby matches.
- [ ] Background-location disclosure appears before Android settings.
- [ ] App-open disclosure appears before Accessibility settings and requires Continue.
- [ ] App-open reminder fires only for the selected app.
- [ ] Notification disclosure appears before Notification Access settings.
- [ ] Notification reminder fires for the selected app/text.
- [ ] Caller disclosure appears before call-screening role request.
- [ ] Caller reminder fires for a selected number.
- [ ] Overlay disclosure appears before display-over-other-apps settings.
- [ ] Caller banner is visible below call controls and can be dismissed.
- [ ] Home-screen widget adds successfully and all four shortcuts open the expected quick-add flow.
- [ ] Privacy & data access screen opens from Setup and matches the public privacy policy.

## Play Console content

- [ ] App title/description pasted from `STORE_LISTING.md` and adjusted only if behavior changes.
- [ ] Privacy URL is live, public, non-PDF, and points to Cue's policy.
- [ ] Data Safety completed from `DATA_SAFETY.md` after final-binary review.
- [ ] AccessibilityService declaration submitted.
- [ ] Background location declaration submitted.
- [ ] Background-location reviewer video link works without requiring reviewer access.
- [ ] Content rating completed.
- [ ] Target audience completed.
- [ ] Ads declaration says No ads.
- [ ] App access instructions explain that no login is required.
- [ ] Screenshots and feature graphic uploaded.

## Reviewer notes

Cue has no login. On first install, only the permissions needed by the trigger being tested need to be enabled. Reviewers can create each rule directly from the app; advanced permissions are under Setup.

For app-open reminders, enable **Cue app-open reminder detection** in Android Accessibility settings. This service only detects foreground package changes and cannot retrieve window contents.

For location reminders, enable precise foreground location and background/`Allow all the time` location so Android can deliver geofence transitions while Cue is closed.
