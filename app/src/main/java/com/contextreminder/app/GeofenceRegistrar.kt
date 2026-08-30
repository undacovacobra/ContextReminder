package com.contextreminder.app

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.contextreminder.core.GeofenceTransition
import com.contextreminder.core.Trigger
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceRegistrar(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getGeofencingClient(appContext)

    fun sync(onResult: ((Result<Unit>) -> Unit)? = null) {
        if (!hasPermissions()) {
            onResult?.invoke(Result.failure(IllegalStateException(
                "Precise location and background location are required for place reminders."
            )))
            return
        }

        val pendingIntent = geofencePendingIntent()
        val locationRules = RuleStore(appContext).load()
            .filter { it.enabled }
            .mapNotNull { it.trigger as? Trigger.Geofence }

        try {
            client.removeGeofences(pendingIntent)
                .addOnSuccessListener {
                    if (locationRules.isEmpty()) {
                        onResult?.invoke(Result.success(Unit))
                        return@addOnSuccessListener
                    }

                    val geofences = locationRules.map { trigger ->
                        Geofence.Builder()
                            .setRequestId(trigger.placeId)
                            .setCircularRegion(
                                trigger.latitude,
                                trigger.longitude,
                                trigger.radiusMeters.coerceIn(50f, 1000f)
                            )
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setTransitionTypes(
                                if (trigger.transition == GeofenceTransition.ENTER) {
                                    Geofence.GEOFENCE_TRANSITION_ENTER
                                } else {
                                    Geofence.GEOFENCE_TRANSITION_EXIT
                                }
                            )
                            .build()
                    }

                    val requestBuilder = GeofencingRequest.Builder().setInitialTrigger(0)
                    geofences.forEach(requestBuilder::addGeofence)

                    try {
                        client.addGeofences(requestBuilder.build(), pendingIntent)
                            .addOnSuccessListener { onResult?.invoke(Result.success(Unit)) }
                            .addOnFailureListener { error ->
                                onResult?.invoke(Result.failure(
                                    IllegalStateException(
                                        error.message ?: "Android could not activate the place reminder.",
                                        error
                                    )
                                ))
                            }
                    } catch (error: SecurityException) {
                        onResult?.invoke(Result.failure(error))
                    }
                }
                .addOnFailureListener { error ->
                    onResult?.invoke(Result.failure(
                        IllegalStateException(
                            error.message ?: "Android could not refresh place reminders.",
                            error
                        )
                    ))
                }
        } catch (error: SecurityException) {
            onResult?.invoke(Result.failure(error))
        }
    }

    private fun hasPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        return fine && background
    }

    private fun geofencePendingIntent(): PendingIntent {
        val intent = Intent(appContext, GeofenceReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(appContext, 7001, intent, flags)
    }
}
