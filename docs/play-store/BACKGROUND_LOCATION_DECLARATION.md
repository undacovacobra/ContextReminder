# Background Location Declaration Draft

## Main purpose of Cue
Cue is a context-aware reminder app. Users create reminders that fire at the moment a selected context occurs, including arriving at or leaving a saved place.

## Single feature that requires background location
**Arrival/departure place reminders.** A user selects a place and explicitly chooses either `I arrive` or `I leave`. Cue registers an Android geofence around that selected place. Background location is required so Android can deliver the geofence transition while Cue is closed or not in use.

## Why this cannot be implemented with foreground-only location
The defining user benefit is receiving the reminder when the user physically reaches or leaves the place without needing to remember to open Cue first. Foreground-only location would make the feature fail precisely when the reminder is supposed to help.

## Data use

- Location is used only for user-created place reminders and selecting/searching places.
- Cue does not operate a server that receives user location.
- Cue does not use location for advertising, marketing, or analytics.
- Saved place coordinates are stored locally in the reminder rule.
- Android/Google system services may process device location, geofencing, or place-search requests according to device settings and their own terms.

## Prominent in-app disclosure

> Cue uses your location in the background to trigger arrival and departure reminders you create, even when the app is closed or not in use. Your reminder rules and saved place coordinates are stored on this device. Cue does not operate a server that receives your location, and Cue does not use location for advertising. Android and Google system location services may process location or place-search requests according to your device settings and their own terms.

Cue shows this disclosure before sending the user to Android settings to enable background location.

## Reviewer video script (target: 25-30 seconds)

1. Start screen recording on an Android device.
2. Open Cue > Setup.
3. Tap **Review access** beside Background location.
4. Hold on the dedicated disclosure long enough for the reviewer to read the first lines.
5. Tap **Continue** and show Android's app-location settings with `Allow all the time` / background access.
6. Return to Cue and create a Place reminder: `Remind me: Test arrival reminder` > `I arrive` > select a nearby place > Save.
7. Close Cue completely.
8. Use a real movement/test geofence workflow to demonstrate the reminder firing while Cue is not open. If the final video cannot physically move within 30 seconds, make a separate concise reviewer video that demonstrates the actual background transition with a test location/device setup and clearly state the test method in the declaration.

## Store-listing support
The full description explicitly states that location reminders can work when Cue is closed when the user allows background location.
