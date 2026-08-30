package com.contextreminder.app

import com.contextreminder.core.GeofenceTransition
import com.contextreminder.core.ReminderRule
import com.contextreminder.core.RepeatPolicy
import com.contextreminder.core.RuleCondition
import com.contextreminder.core.Trigger
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

object RuleJsonCodec {
    fun encode(rules: List<ReminderRule>): String {
        val array = JSONArray()
        rules.forEach { array.put(encodeRule(it)) }
        return array.toString()
    }

    fun decode(value: String?): List<ReminderRule> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    runCatching { decodeRule(array.getJSONObject(index)) }
                        .getOrNull()
                        ?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeRule(rule: ReminderRule): JSONObject = JSONObject().apply {
        put("id", rule.id)
        put("title", rule.title)
        put("reminderText", rule.reminderText)
        put("enabled", rule.enabled)
        put("createdAt", rule.createdAtEpochMs)
        if (rule.lastTriggeredAtEpochMs == null) put("lastTriggeredAt", JSONObject.NULL)
        else put("lastTriggeredAt", rule.lastTriggeredAtEpochMs)
        put("trigger", encodeTrigger(rule.trigger))
        put("condition", encodeCondition(rule.condition))
        put("repeat", encodeRepeat(rule.repeatPolicy))
    }

    private fun decodeRule(json: JSONObject): ReminderRule = ReminderRule(
        id = json.getString("id"),
        title = json.optString("title", "Reminder"),
        reminderText = json.getString("reminderText"),
        enabled = json.optBoolean("enabled", true),
        trigger = decodeTrigger(json.getJSONObject("trigger")),
        condition = decodeCondition(json.optJSONObject("condition")),
        repeatPolicy = decodeRepeat(json.optJSONObject("repeat")),
        lastTriggeredAtEpochMs = json.optLongOrNull("lastTriggeredAt"),
        createdAtEpochMs = json.optLong("createdAt", 0L)
    )

    private fun encodeTrigger(trigger: Trigger): JSONObject = JSONObject().apply {
        when (trigger) {
            is Trigger.AppOpened -> {
                put("type", "app_opened")
                put("packageName", trigger.packageName)
            }
            is Trigger.IncomingCall -> {
                put("type", "incoming_call")
                put("phoneNumber", trigger.phoneNumber)
            }
            is Trigger.NotificationReceived -> {
                put("type", "notification")
                put("packageName", trigger.packageName)
                put("textContains", trigger.textContains ?: "")
            }
            is Trigger.Geofence -> {
                put("type", "geofence")
                put("placeId", trigger.placeId)
                put("transition", trigger.transition.name)
                put("latitude", trigger.latitude)
                put("longitude", trigger.longitude)
                put("radiusMeters", trigger.radiusMeters.toDouble())
                put("label", trigger.label)
            }
        }
    }

    private fun decodeTrigger(json: JSONObject): Trigger = when (json.getString("type")) {
        "app_opened" -> Trigger.AppOpened(json.getString("packageName"))
        "incoming_call" -> Trigger.IncomingCall(json.getString("phoneNumber"))
        "notification" -> Trigger.NotificationReceived(
            packageName = json.getString("packageName"),
            textContains = json.optString("textContains").takeIf(String::isNotBlank)
        )
        "geofence" -> Trigger.Geofence(
            placeId = json.getString("placeId"),
            transition = GeofenceTransition.valueOf(json.getString("transition")),
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
            radiusMeters = json.optDouble("radiusMeters", 150.0).toFloat(),
            label = json.optString("label", "Saved place")
        )
        else -> error("Unknown trigger type")
    }

    private fun encodeCondition(condition: RuleCondition): JSONObject = JSONObject().apply {
        put("days", JSONArray(condition.days.map { it.name }))
        if (condition.startMinute == null) put("startMinute", JSONObject.NULL) else put("startMinute", condition.startMinute)
        if (condition.endMinute == null) put("endMinute", JSONObject.NULL) else put("endMinute", condition.endMinute)
    }

    private fun decodeCondition(json: JSONObject?): RuleCondition {
        if (json == null) return RuleCondition()
        val dayArray = json.optJSONArray("days") ?: JSONArray()
        val days = buildSet {
            for (index in 0 until dayArray.length()) {
                runCatching { DayOfWeek.valueOf(dayArray.getString(index)) }.getOrNull()?.let(::add)
            }
        }
        return RuleCondition(
            days = days,
            startMinute = json.optIntOrNull("startMinute"),
            endMinute = json.optIntOrNull("endMinute")
        )
    }

    private fun encodeRepeat(policy: RepeatPolicy): JSONObject = JSONObject().apply {
        when (policy) {
            RepeatPolicy.EveryTime -> put("type", "every_time")
            RepeatPolicy.Once -> put("type", "once")
            is RepeatPolicy.Cooldown -> {
                put("type", "cooldown")
                put("minutes", policy.minutes)
            }
        }
    }

    private fun decodeRepeat(json: JSONObject?): RepeatPolicy = when (json?.optString("type")) {
        "once" -> RepeatPolicy.Once
        "cooldown" -> RepeatPolicy.Cooldown(json.optInt("minutes", 60).coerceAtLeast(1))
        else -> RepeatPolicy.EveryTime
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)
}
