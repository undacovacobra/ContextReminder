package com.contextreminder.core

import java.time.Instant
import java.time.ZoneId

object RuleEngine {
    fun evaluate(
        rule: ReminderRule,
        event: TriggerEvent,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): RuleEvaluation {
        if (!rule.enabled) return RuleEvaluation(false, reason = "disabled")
        if (!triggerMatches(rule.trigger, event)) return RuleEvaluation(false, reason = "trigger_mismatch")
        if (!conditionMatches(rule.condition, now, zoneId)) return RuleEvaluation(false, reason = "condition_mismatch")
        if (!repeatAllows(rule, now)) return RuleEvaluation(false, reason = "repeat_blocked")

        return RuleEvaluation(
            shouldFire = true,
            disableAfterFire = rule.repeatPolicy is RepeatPolicy.Once
        )
    }

    fun normalizePhone(value: String): String = value.filter(Char::isDigit)

    private fun triggerMatches(trigger: Trigger, event: TriggerEvent): Boolean {
        return when {
        trigger is Trigger.AppOpened && event is TriggerEvent.AppOpened ->
            trigger.packageName == event.packageName

        trigger is Trigger.IncomingCall && event is TriggerEvent.IncomingCall ->
            phoneMatches(trigger.phoneNumber, event.phoneNumber)

        trigger is Trigger.NotificationReceived && event is TriggerEvent.NotificationReceived -> {
            if (trigger.packageName != event.packageName) return false
            val needle = trigger.textContains?.trim().orEmpty()
            if (needle.isEmpty()) return true
            val haystack = listOfNotNull(event.title, event.text).joinToString(" ")
            haystack.contains(needle, ignoreCase = true)
        }

        trigger is Trigger.Geofence && event is TriggerEvent.Geofence ->
            trigger.placeId == event.placeId && trigger.transition == event.transition

        else -> false
        }
    }

    private fun phoneMatches(expected: String, actual: String): Boolean {
        val a = normalizePhone(expected)
        val b = normalizePhone(actual)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        val suffixLength = minOf(10, a.length, b.length)
        return suffixLength >= 7 && a.takeLast(suffixLength) == b.takeLast(suffixLength)
    }

    private fun conditionMatches(condition: RuleCondition, now: Instant, zoneId: ZoneId): Boolean {
        val local = now.atZone(zoneId)
        if (condition.days.isNotEmpty() && local.dayOfWeek !in condition.days) return false

        val start = condition.startMinute
        val end = condition.endMinute
        if (start == null || end == null) return true

        val minute = local.hour * 60 + local.minute
        return if (start == end) {
            true
        } else if (start < end) {
            minute in start until end
        } else {
            minute >= start || minute < end
        }
    }

    private fun repeatAllows(rule: ReminderRule, now: Instant): Boolean {
        return when (val policy = rule.repeatPolicy) {
        RepeatPolicy.EveryTime -> true
        RepeatPolicy.Once -> true
        is RepeatPolicy.Cooldown -> {
            val last = rule.lastTriggeredAtEpochMs ?: return true
            val cooldownMs = policy.minutes.coerceAtLeast(1) * 60_000L
            now.toEpochMilli() - last >= cooldownMs
            }
        }
    }
}
