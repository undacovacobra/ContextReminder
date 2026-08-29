# Context Reminder

Personal Android reminder app that turns phone events into reminders.

## V1 triggers
- Arrive at or leave a saved location.
- Incoming call from a selected contact/number.
- Opening a selected app.
- Notification from a selected app, optionally containing chosen text.

Rules can be limited by weekday and time window, and can fire every time, once, or after a cooldown.

## Android access used
- Notifications: show reminders.
- Fine/background location: capture and monitor places.
- Accessibility service: detect selected apps coming to the foreground.
- Notification listener: react to incoming notifications.
- Contacts + call-screening role: react while selected callers are ringing.

All rules are stored locally in SharedPreferences. There is no server or account.

## Build
The repository includes a GitHub Actions workflow that expands the source bundle, runs the core tests, builds the Android debug APK, and publishes it as the `ContextReminder-debug-apk` artifact.

## Install on a Samsung/Android phone
Download `app-debug.apk` from the latest successful GitHub Actions artifact, tap it on the phone, allow installation from the app used to open the file when Android prompts, then open Context Reminder and visit the Setup tab. Enable only the trigger access you plan to use.
