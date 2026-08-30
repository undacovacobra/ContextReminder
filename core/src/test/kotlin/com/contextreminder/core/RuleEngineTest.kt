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

    println("RuleEngine tests passed")
}
