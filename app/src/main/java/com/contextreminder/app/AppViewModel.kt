package com.contextreminder.app

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contextreminder.core.ReminderRule
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class InstalledApp(
    val label: String,
    val packageName: String
)

data class ResolvedPlace(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val store = RuleStore(appContext)

    private val _rules = MutableStateFlow(store.load())
    val rules: StateFlow<List<ReminderRule>> = _rules.asStateFlow()

    private val _apps = MutableStateFlow(loadInstalledApps())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    fun refresh() {
        _rules.value = store.load()
        _apps.value = loadInstalledApps()
    }

    fun addRule(rule: ReminderRule) {
        store.upsert(rule)
        _rules.value = store.load()
        GeofenceRegistrar(appContext).sync()
    }

    fun deleteRule(id: String) {
        store.delete(id)
        _rules.value = store.load()
        GeofenceRegistrar(appContext).sync()
    }

    fun setRuleEnabled(id: String, enabled: Boolean) {
        store.setEnabled(id, enabled)
        _rules.value = store.load()
        GeofenceRegistrar(appContext).sync()
    }

    fun captureCurrentLocation(onResult: (Location?) -> Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            onResult(null)
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(appContext)
        val tokenSource = CancellationTokenSource()
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { onResult(it) }
                .addOnFailureListener { onResult(null) }
        } catch (_: SecurityException) {
            onResult(null)
        }
    }

    fun resolvePlace(query: String, onResult: (ResolvedPlace?) -> Unit) {
        val locationName = query.trim()
        if (locationName.isBlank() || !Geocoder.isPresent()) {
            onResult(null)
            return
        }

        val geocoder = Geocoder(appContext, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                geocoder.getFromLocationName(
                    locationName,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            deliverResolvedPlace(addresses.firstOrNull(), onResult)
                        }

                        override fun onError(errorMessage: String?) {
                            deliverResolvedPlace(null, onResult)
                        }
                    }
                )
            } catch (_: IllegalArgumentException) {
                deliverResolvedPlace(null, onResult)
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val address = try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(locationName, 1)?.firstOrNull()
                } catch (_: Exception) {
                    null
                }
                withContext(Dispatchers.Main) {
                    onResult(address?.toResolvedPlace())
                }
            }
        }
    }

    private fun deliverResolvedPlace(address: Address?, onResult: (ResolvedPlace?) -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            onResult(address?.toResolvedPlace())
        }
    }

    private fun loadInstalledApps(): List<InstalledApp> {
        val packageManager = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        return results.mapNotNull { info ->
            val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
            InstalledApp(
                label = info.loadLabel(packageManager)?.toString()?.ifBlank { packageName } ?: packageName,
                packageName = packageName
            )
        }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}

private fun Address.toResolvedPlace(): ResolvedPlace {
    val displayLabel = getAddressLine(0)
        ?.takeIf { it.isNotBlank() }
        ?: listOfNotNull(featureName, locality, adminArea)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(", ")
            .ifBlank { "Saved place" }

    return ResolvedPlace(
        label = displayLabel,
        latitude = latitude,
        longitude = longitude
    )
}
