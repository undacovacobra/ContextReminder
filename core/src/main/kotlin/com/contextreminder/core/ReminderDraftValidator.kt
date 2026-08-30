package com.contextreminder.core

enum class ReminderDraftTrigger {
    LOCATION,
    CALLER,
    APP,
    NOTIFICATION
}

data class ReminderDraft(
    val reminderText: String,
    val triggerType: ReminderDraftTrigger,
    val locationQuery: String = "",
    val hasResolvedLocation: Boolean = false,
    val callerNumber: String = "",
    val packageName: String = "",
    val useTimeWindow: Boolean = false,
    val startMinute: Int? = null,
    val endMinute: Int? = null
)

data class ReminderDraftValidation(
    val reminderError: String? = null,
    val triggerError: String? = null,
    val timeError: String? = null
) {
    val isValid: Boolean
        get() = reminderError == null && triggerError == null && timeError == null
}

object ReminderDraftValidator {
    fun validate(draft: ReminderDraft): ReminderDraftValidation {
        val reminderError = if (draft.reminderText.isBlank()) {
            "What should I remind you?"
        } else {
            null
        }

        val triggerError = when (draft.triggerType) {
            ReminderDraftTrigger.LOCATION -> if (
                draft.locationQuery.isBlank() && !draft.hasResolvedLocation
            ) {
                "Enter an address or use your current location."
            } else {
                null
            }

            ReminderDraftTrigger.CALLER -> if (draft.callerNumber.isBlank()) {
                "Choose a person or enter a phone number."
            } else {
                null
            }

            ReminderDraftTrigger.APP,
            ReminderDraftTrigger.NOTIFICATION -> if (draft.packageName.isBlank()) {
                "Choose an app."
            } else {
                null
            }
        }

        val timeError = if (
            draft.useTimeWindow && (draft.startMinute == null || draft.endMinute == null)
        ) {
            "Use valid start and end times."
        } else {
            null
        }

        return ReminderDraftValidation(
            reminderError = reminderError,
            triggerError = triggerError,
            timeError = timeError
        )
    }
}
