package com.contextreminder.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.contextreminder.core.GeofenceTransition
import com.contextreminder.core.TriggerEvent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> GeofenceTransition.ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> GeofenceTransition.EXIT
            else -> return
        }

        val coordinator = RuleCoordinator(context)
        event.triggeringGeofences.orEmpty().forEach { geofence ->
            coordinator.handle(TriggerEvent.Geofence(geofence.requestId, transition))
        }
    }
}
