package com.contextreminder.core

import java.time.DayOfWeek

sealed interface Trigger {
    data class AppOpened(val packageName: String) : Trigger
    data class IncomingCall(val phoneNumber: String) : Trigger
    data class NotificationReceived(val packageName: String, val textContains: String? = null) : Trigger
    data class Geofence(
        val placeId: String,
        val transition: GeofenceTransition,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val radiusMeters: Float = 150f,
        val label: String = "Saved place"
    ) : Trigger
}

enum class GeofenceTransition { ENTER, EXIT }

sealed interface TriggerEvent {
    data class AppOpened(val packageName: String) : TriggerEvent
    data class IncomingCall(val phoneNumber: String) : TriggerEvent
    data class NotificationReceived(
        val packageName: String,
        val title: String?,
        val text: String?
    ) : TriggerEvent
    data class Geofence(
        val placeId: String,
        val transition: GeofenceTransition,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val radiusMeters: Float = 150f,
        val label: String = "Saved place"
    ) : TriggerEvent
}

data class RuleCondition(
    val days: Set<DayOfWeek> = emptySet(),
    val startMinute: Int? = null,
    val endMinute: Int? = null
)

sealed interface RepeatPolicy {
    data object EveryTime : RepeatPolicy
    data object Once : RepeatPolicy
    data class Cooldown(val minutes: Int) : RepeatPolicy
}

data class ReminderRule(
    val id: String,
    val title: String = "Reminder",
    val reminderText: String,
    val enabled: Boolean = true,
    val trigger: Trigger,
    val condition: RuleCondition = RuleCondition(),
    val repeatPolicy: RepeatPolicy = RepeatPolicy.EveryTime,
    val lastTriggeredAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = 0L
)

data class RuleEvaluation(
    val shouldFire: Boolean,
    val disableAfterFire: Boolean = false,
    val reason: String? = null
)
