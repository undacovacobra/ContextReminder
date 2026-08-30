package com.contextreminder.app

import android.telecom.Call
import android.telecom.CallScreeningService
import com.contextreminder.core.TriggerEvent

class ReminderCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)

        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        if (number.isNotBlank()) {
            val firedRules = RuleCoordinator(this).handle(TriggerEvent.IncomingCall(number))
            if (firedRules.isNotEmpty()) {
                CallReminderOverlay.show(
                    this,
                    firedRules.joinToString(separator = "\n") { it.reminderText }
                )
            }
        }
    }
}
