package com.contextreminder.core

object PlayDisclosureCopy {
    const val backgroundLocation =
        "Cue uses your location in the background to trigger arrival and departure reminders you create, even when the app is closed or not in use. Your reminder rules and saved place coordinates are stored on this device. Cue does not operate a server that receives your location, and Cue does not use location for advertising. Android and Google system location services may process location or place-search requests according to your device settings and their own terms."

    const val accessibility =
        "Cue uses Android Accessibility access only to detect when the package for an app you selected comes to the foreground, so Cue can trigger an app-open reminder you created. Cue does not read screen text, does not retrieve window contents, and does not perform taps, gestures, typing, or other automated actions. The detected app package is evaluated on this device against your saved reminders and is not sent to a Cue server."

    const val notificationAccess =
        "Cue uses Android notification access only to check notifications against notification reminders you create. Cue may evaluate the sending app, title, and notification text on this device when your rule requires text matching. Cue does not send notification contents to a Cue server and does not use them for advertising."

    const val callScreening =
        "Cue uses Android's call-screening role only to detect the incoming phone number and match it against caller reminders you create. Cue does not block, reject, record, or answer calls, and it does not send call information to a Cue server."

    const val overlay =
        "Cue can display a small reminder banner over the phone call screen so caller reminders are not hidden behind Android's call controls. Cue uses this access only to show reminder content you created; it does not inspect or control other apps."
}
