# AccessibilityService Declaration Draft

## Is Cue an accessibility tool?
No. The service metadata explicitly sets `isAccessibilityTool=false`.

## Core feature that requires AccessibilityService
**User-created app-open reminders.** A user selects a specific installed app and writes a reminder. Cue listens only for window state/window changes and reads the foreground package identifier so it can determine whether the selected app just came to the foreground. If it matches the user's rule, Cue displays that reminder.

## Why AccessibilityService is necessary
Android does not provide a general-purpose public callback that reliably tells a normal background app whenever another arbitrary installed app enters the foreground. Cue uses AccessibilityService for this single deterministic rule trigger.

## Narrow scope

- Event types: `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOWS_CHANGED` only.
- `canRetrieveWindowContent=false`.
- Cue does not read screen text through AccessibilityService.
- Cue does not inspect view hierarchies.
- Cue does not perform clicks, gestures, typing, scrolling, or automated UI actions.
- Cue does not use AccessibilityService for advertising, analytics, credential collection, surveillance, or behavior profiling.
- Foreground package identifiers are evaluated locally against user-created reminder rules and are not sent to a Cue-operated backend.

## Prominent disclosure shown before Android settings

> Cue uses Android Accessibility access only to detect when the package for an app you selected comes to the foreground, so Cue can trigger an app-open reminder you created. Cue does not read screen text, does not retrieve window contents, and does not perform taps, gestures, typing, or other automated actions. The detected app package is evaluated on this device against your saved reminders and is not sent to a Cue server.

The user must tap **Continue** before Cue opens Android Accessibility settings.

## Reviewer demonstration

1. Open Cue > Setup.
2. Tap **Review access** for App-open detection.
3. Observe the dedicated Accessibility disclosure.
4. Tap Continue and enable the Cue accessibility service.
5. Create an app-open reminder for a test app.
6. Leave Cue and open the selected app.
7. Observe the reminder notification.

## Play Console answer framing
Describe only the app-open reminder feature. Do not describe Cue as an accessibility aid and do not claim broader automation functionality.
