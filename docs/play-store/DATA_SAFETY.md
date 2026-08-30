# Cue Data Safety Draft

Use this as the working answer set when completing Play Console. Re-check it against the final shipped binary before submission.

## Developer collection/sharing summary

Cue has no account system, Cue-operated backend, analytics SDK, advertising SDK, crash-reporting SDK, or third-party marketing SDK. Reminder rules are stored in the app's private local storage. The Play release disables Android app backup.

Recommended Play Console starting position:

- Data collected by the developer: **No**, provided the final binary still has no telemetry/backend SDKs.
- Data shared by the developer: **No**.
- Data encrypted in transit: **Not applicable to a Cue backend because Cue has no backend.**
- Users can request deletion: Cue has no online account. Users delete reminder data in-app, clear app data, or uninstall.

## Sensitive data Cue accesses locally

| Data/API | Why Cue accesses it | Persisted by Cue? | Sent to Cue server? |
| --- | --- | --- | --- |
| Precise/approximate location | Choose current place; create/trigger arrival and departure reminders | Saved place coordinates for rules | No Cue server exists |
| Background location/geofencing | Fire arrive/leave reminders while Cue is closed | Geofence coordinates/rule | No |
| Selected contact/phone number | Match caller reminders | Selected name/number can be stored in the rule | No |
| Incoming phone number | Match an incoming call to a caller rule | Only rule state/history as needed | No |
| App package/foreground change | Trigger app-open reminders | Selected package is stored in the rule | No |
| Notification app/title/text | Match notification rules, including optional text matching | Rule's matching text is stored; notification contents are not intentionally archived | No |
| Reminder text | Display the reminder the user created | Yes, locally | No |

## Platform-service qualification

Cue uses Android/Google platform services for fused location, geofencing, and Android Geocoder/place-name resolution. Those platform services may process location or search queries according to the user's device settings and Google's/Android's own terms. Cue does not receive this information on a Cue-operated server.

Before answering the Play Data Safety questionnaire, confirm Google's current guidance on whether platform-service processing needs to be represented in the specific questionnaire version presented in Play Console.

## No broad contacts permission

The Play branch removes `READ_CONTACTS`. Cue uses a user-initiated contact picker and only queries the URI returned by the picker.
