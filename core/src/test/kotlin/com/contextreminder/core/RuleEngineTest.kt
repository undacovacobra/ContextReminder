package com.contextreminder.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

private fun assertTrue(value: Boolean, message: String) {
    if (!value) error(message)
}

private fun assertFalse(value: Boolean, message: String) = assertTrue(!value, message)

private fun assertEquals(expected: Any?, actual: Any?, message: String) {
    if (expected != actual) error("$message. Expected=$expected actual=$actual")
}

fun main() {
    val zone = ZoneId.of("America/New_York")
    val mondayTenAm = Instant.parse("2026-08-31T14:00:00Z")

    val appRule = ReminderRule(
        id = "1",
        reminderText = "Finish invoices first",
        trigger = Trigger.AppOpened("com.example.video"),
        condition = RuleCondition(days = setOf(DayOfWeek.MONDAY)),
        repeatPolicy = RepeatPolicy.EveryTime
    )

    val match = RuleEngine.evaluate(appRule, TriggerEvent.AppOpened("com.example.video"), mondayTenAm, zone)
    assertTrue(match.shouldFire, "Matching app-open event should fire")

    val wrongApp = RuleEngine.evaluate(appRule, TriggerEvent.AppOpened("com.other"), mondayTenAm, zone)
    assertFalse(wrongApp.shouldFire, "Different app should not fire")

    val wrongDayRule = appRule.copy(condition = RuleCondition(days = setOf(DayOfWeek.TUESDAY)))
    val wrongDay = RuleEngine.evaluate(wrongDayRule, TriggerEvent.AppOpened("com.example.video"), mondayTenAm, zone)
    assertFalse(wrongDay.shouldFire, "Rule should respect selected weekdays")

    val timeRule = appRule.copy(condition = RuleCondition(startMinute = 9 * 60, endMinute = 11 * 60))
    val inWindow = RuleEngine.evaluate(timeRule, TriggerEvent.AppOpened("com.example.video"), mondayTenAm, zone)
    assertTrue(inWindow.shouldFire, "10 AM should be inside 9-11 AM window")

    val overnightRule = appRule.copy(condition = RuleCondition(startMinute = 22 * 60, endMinute = 6 * 60))
    val overnightMiss = RuleEngine.evaluate(overnightRule, TriggerEvent.AppOpened("com.example.video"), mondayTenAm, zone)
    assertFalse(overnightMiss.shouldFire, "10 AM should not be inside overnight window")

    val cooldownRule = appRule.copy(
        repeatPolicy = RepeatPolicy.Cooldown(minutes = 60),
        lastTriggeredAtEpochMs = mondayTenAm.minusSeconds(30 * 60).toEpochMilli()
    )
    val cooldownBlocked = RuleEngine.evaluate(cooldownRule, TriggerEvent.AppOpened("com.example.video"), mondayTenAm, zone)
    assertFalse(cooldownBlocked.shouldFire, "Cooldown should suppress early repeat")

    val cooldownReady = cooldownRule.copy(lastTriggeredAtEpochMs = mondayTenAm.minusSeconds(61 * 60).toEpochMilli())
    val cooldownAllowed = RuleEngine.evaluate(cooldownReady, TriggerEvent.AppOpened("com.example.video"), mondayTenAm, zone)
    assertTrue(cooldownAllowed.shouldFire, "Cooldown should allow firing after expiry")

    val onceRule = appRule.copy(repeatPolicy = RepeatPolicy.Once)
    val onceResult = RuleEngine.evaluate(onceRule, TriggerEvent.AppOpened("com.example.video"), mondayTenAm, zone)
    assertTrue(onceResult.shouldFire, "Once rule should fire when it matches")
    assertTrue(onceResult.disableAfterFire, "Once rule should disable after firing")

    val callRule = appRule.copy(trigger = Trigger.IncomingCall("9415551212"))
    val callMatch = RuleEngine.evaluate(callRule, TriggerEvent.IncomingCall("+1 (941) 555-1212"), mondayTenAm, zone)
    assertTrue(callMatch.shouldFire, "Phone matching should tolerate country code and formatting")

    val notifRule = appRule.copy(trigger = Trigger.NotificationReceived("com.mail", "invoice"))
    val notifMatch = RuleEngine.evaluate(
        notifRule,
        TriggerEvent.NotificationReceived("com.mail", "New message", "Your INVOICE is ready"),
        mondayTenAm,
        zone
    )
    assertTrue(notifMatch.shouldFire, "Notification text matching should be case-insensitive")

    val geoRule = appRule.copy(trigger = Trigger.Geofence("place-1", GeofenceTransition.ENTER))
    val geoMatch = RuleEngine.evaluate(
        geoRule,
        TriggerEvent.Geofence("place-1", GeofenceTransition.ENTER),
        mondayTenAm,
        zone
    )
    assertTrue(geoMatch.shouldFire, "Matching geofence transition should fire")

    val disabled = appRule.copy(enabled = false)
    val disabledResult = RuleEngine.evaluate(disabled, TriggerEvent.AppOpened("com.example.video"), mondayTenAm, zone)
    assertFalse(disabledResult.shouldFire, "Disabled rule should never fire")

    assertEquals("19415551212", RuleEngine.normalizePhone("+1 (941) 555-1212"), "Phone normalization should retain digits")

    val blankReminder = ReminderDraftValidator.validate(
        ReminderDraft(
            reminderText = "",
            triggerType = ReminderDraftTrigger.APP,
            packageName = "com.example.video"
        )
    )
    assertEquals("What should I remind you?", blankReminder.reminderError, "Blank reminder text should show an inline error")

    val blankLocation = ReminderDraftValidator.validate(
        ReminderDraft(
            reminderText = "Get cabinet screws",
            triggerType = ReminderDraftTrigger.LOCATION
        )
    )
    assertEquals("Enter an address and choose the correct place, or use your current location.", blankLocation.triggerError, "Location reminders should require a selected place")

    val unresolvedAddress = ReminderDraftValidator.validate(
        ReminderDraft(
            reminderText = "Get cabinet screws",
            triggerType = ReminderDraftTrigger.LOCATION,
            locationQuery = "Home Depot, North Port FL",
            hasResolvedLocation = false
        )
    )
    assertEquals(
        "Choose the correct place from the suggestions.",
        unresolvedAddress.triggerError,
        "A typed location should not save until a suggestion is selected"
    )

    val selectedAddress = ReminderDraftValidator.validate(
        ReminderDraft(
            reminderText = "Get cabinet screws",
            triggerType = ReminderDraftTrigger.LOCATION,
            locationQuery = "Home Depot, North Port FL",
            hasResolvedLocation = true
        )
    )
    assertTrue(selectedAddress.isValid, "A selected resolved place should be accepted")

    val blankCaller = ReminderDraftValidator.validate(
        ReminderDraft(
            reminderText = "Ask about Sunday",
            triggerType = ReminderDraftTrigger.CALLER
        )
    )
    assertEquals("Choose a person or enter a phone number.", blankCaller.triggerError, "Caller reminders should require a caller")

    val blankApp = ReminderDraftValidator.validate(
        ReminderDraft(
            reminderText = "Finish invoices first",
            triggerType = ReminderDraftTrigger.APP
        )
    )
    assertEquals("Choose an app.", blankApp.triggerError, "App-open reminders should require an app")

    val invalidTime = ReminderDraftValidator.validate(
        ReminderDraft(
            reminderText = "Finish invoices first",
            triggerType = ReminderDraftTrigger.APP,
            packageName = "com.example.video",
            useTimeWindow = true,
            startMinute = null,
            endMinute = 17 * 60
        )
    )
    assertEquals("Use valid start and end times.", invalidTime.timeError, "Invalid optional time windows should be explained inline")

    val rankedPlaces = GeoRanker.nearestFirst(
        originLatitude = 27.0442,
        originLongitude = -82.2359,
        candidates = listOf(
            GeoCandidate("far", 27.3364, -82.5307),
            GeoCandidate("near", 27.0700, -82.2080)
        )
    )
    assertEquals(
        listOf("near", "far"),
        rankedPlaces.map { it.value },
        "Nearby business results should be sorted nearest first"
    )
    assertTrue(
        rankedPlaces.first().distanceMeters < rankedPlaces.last().distanceMeters,
        "Ranked business results should expose increasing distance"
    )

    val locationDisclosure = PlayDisclosureCopy.backgroundLocation.lowercase()
    assertTrue(locationDisclosure.contains("location"), "Background location disclosure must name location data")
    assertTrue(locationDisclosure.contains("background"), "Background location disclosure must name background use")
    assertTrue(locationDisclosure.contains("when the app is closed"), "Background location disclosure must explain closed-app use")
    assertTrue(locationDisclosure.contains("on this device"), "Background location disclosure should state local-only handling")

    val accessibilityDisclosure = PlayDisclosureCopy.accessibility.lowercase()
    assertTrue(accessibilityDisclosure.contains("accessibility"), "Accessibility disclosure must name Accessibility access")
    assertTrue(accessibilityDisclosure.contains("foreground"), "Accessibility disclosure must explain foreground-app detection")
    assertTrue(accessibilityDisclosure.contains("does not read screen text"), "Accessibility disclosure must describe the narrow data scope")
    assertTrue(accessibilityDisclosure.contains("does not perform taps"), "Accessibility disclosure must state that Cue does not automate UI actions")

    println("RuleEngine tests passed")
}