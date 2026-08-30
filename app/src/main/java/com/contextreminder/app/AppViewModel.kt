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
import com.contextreminder.core.GeoCandidate
import com.contextreminder.core.GeoRanker
import com.contextreminder.core.ReminderRule
import com.contextreminder.core.Trigger
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
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
    val longitude: Double,
    val distanceMeters: Double? = null
)

private data class SearchBounds(
    val lowerLeftLatitude: Double,
    val lowerLeftLongitude: Double,
    val upperRightLatitude: Double,
    val upperRightLongitude: Double
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val store = RuleStore(appContext)
    private val placeSearchGeneration = AtomicInteger(0)

    private val _rules = MutableStateFlow(store.load())
    val rules: StateFlow<List<ReminderRule>> = _rules.asStateFlow()

    private val _apps = MutableStateFlow(loadInstalledApps())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    fun refresh() {
        _rules.value = store.load()
        _apps.value = loadInstalledApps()
    }

    fun addRule(rule: ReminderRule) {
        addRule(rule) { }
    }

    fun addRule(rule: ReminderRule, onResult: (Result<Unit>) -> Unit) {
        store.upsert(rule)
        _rules.value = store.load()

        if (rule.trigger !is Trigger.Geofence) {
            GeofenceRegistrar(appContext).sync()
            onResult(Result.success(Unit))
            return
        }

        GeofenceRegistrar(appContext).sync { result ->
            if (result.isSuccess) {
                onResult(Result.success(Unit))
            } else {
                store.delete(rule.id)
                _rules.value = store.load()
                GeofenceRegistrar(appContext).sync()
                onResult(Result.failure(
                    result.exceptionOrNull()
                        ?: IllegalStateException("Android could not activate the place reminder.")
                ))
            }
        }
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

    fun searchPlaces(query: String, onResult: (List<ResolvedPlace>) -> Unit) {
        val locationName = query.trim()
        val generation = placeSearchGeneration.incrementAndGet()
        if (locationName.length < 3 || !Geocoder.isPresent()) {
            onResult(emptyList())
            return
        }

        getSearchOrigin { origin ->
            if (generation != placeSearchGeneration.get()) return@getSearchOrigin
            geocodeLocationName(
                locationName = locationName,
                generation = generation,
                origin = origin,
                useNearbyBounds = origin != null,
                onResult = onResult
            )
        }
    }

    fun resolvePlace(query: String, onResult: (ResolvedPlace?) -> Unit) {
        searchPlaces(query) { onResult(it.firstOrNull()) }
    }

    private fun getSearchOrigin(onResult: (Location?) -> Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            onResult(null)
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(appContext)
        try {
            client.lastLocation
                .addOnSuccessListener { onResult(it) }
                .addOnFailureListener { onResult(null) }
        } catch (_: SecurityException) {
            onResult(null)
        }
    }

    private fun geocodeLocationName(
        locationName: String,
        generation: Int,
        origin: Location?,
        useNearbyBounds: Boolean,
        onResult: (List<ResolvedPlace>) -> Unit
    ) {
        if (generation != placeSearchGeneration.get()) return
        val geocoder = Geocoder(appContext, Locale.getDefault())
        val bounds = origin?.takeIf { useNearbyBounds }?.toSearchBounds()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val listener = object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (generation != placeSearchGeneration.get()) return
                        handleGeocodeResults(
                            addresses = addresses,
                            locationName = locationName,
                            generation = generation,
                            origin = origin,
                            usedNearbyBounds = bounds != null,
                            onResult = onResult
                        )
                    }

                    override fun onError(errorMessage: String?) {
                        if (generation != placeSearchGeneration.get()) return
                        handleGeocodeResults(
                            addresses = emptyList(),
                            locationName = locationName,
                            generation = generation,
                            origin = origin,
                            usedNearbyBounds = bounds != null,
                            onResult = onResult
                        )
                    }
                }

                if (bounds != null) {
                    geocoder.getFromLocationName(
                        locationName,
                        10,
                        bounds.lowerLeftLatitude,
                        bounds.lowerLeftLongitude,
                        bounds.upperRightLatitude,
                        bounds.upperRightLongitude,
                        listener
                    )
                } else {
                    geocoder.getFromLocationName(locationName, 10, listener)
                }
            } catch (_: IllegalArgumentException) {
                handleGeocodeResults(
                    addresses = emptyList(),
                    locationName = locationName,
                    generation = generation,
                    origin = origin,
                    usedNearbyBounds = bounds != null,
                    onResult = onResult
                )
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val addresses = try {
                    @Suppress("DEPRECATION")
                    if (bounds != null) {
                        geocoder.getFromLocationName(
                            locationName,
                            10,
                            bounds.lowerLeftLatitude,
                            bounds.lowerLeftLongitude,
                            bounds.upperRightLatitude,
                            bounds.upperRightLongitude
                        ).orEmpty()
                    } else {
                        geocoder.getFromLocationName(locationName, 10).orEmpty()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
                withContext(Dispatchers.Main) {
                    if (generation == placeSearchGeneration.get()) {
                        handleGeocodeResults(
                            addresses = addresses,
                            locationName = locationName,
                            generation = generation,
                            origin = origin,
                            usedNearbyBounds = bounds != null,
                            onResult = onResult
                        )
                    }
                }
            }
        }
    }

    private fun handleGeocodeResults(
        addresses: List<Address>,
        locationName: String,
        generation: Int,
        origin: Location?,
        usedNearbyBounds: Boolean,
        onResult: (List<ResolvedPlace>) -> Unit
    ) {
        if (generation != placeSearchGeneration.get()) return
        if (addresses.isEmpty() && usedNearbyBounds) {
            geocodeLocationName(
                locationName = locationName,
                generation = generation,
                origin = origin,
                useNearbyBounds = false,
                onResult = onResult
            )
            return
        }
        deliverPlaceResults(addresses, origin, onResult)
    }

    private fun deliverPlaceResults(
        addresses: List<Address>,
        origin: Location?,
        onResult: (List<ResolvedPlace>) -> Unit
    ) {
        val places = addresses
            .map(Address::toResolvedPlace)
            .distinctPlaces(maxResults = 10)

        val ranked = if (origin == null) {
            places
        } else {
            GeoRanker.nearestFirst(
                originLatitude = origin.latitude,
                originLongitude = origin.longitude,
                candidates = places.map { place ->
                    GeoCandidate(
                        value = place,
                        latitude = place.latitude,
                        longitude = place.longitude
                    )
                }
            ).map { rankedPlace ->
                rankedPlace.value.copy(distanceMeters = rankedPlace.distanceMeters)
            }
        }

        viewModelScope.launch(Dispatchers.Main) {
            onResult(ranked.take(5))
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

private fun Location.toSearchBounds(): SearchBounds {
    val latitudeDelta = 0.45
    val longitudeScale = cos(Math.toRadians(latitude)).let { value ->
        if (value < 0.2) 0.2 else value
    }
    val longitudeDelta = 0.45 / longitudeScale

    return SearchBounds(
        lowerLeftLatitude = (latitude - latitudeDelta).coerceAtLeast(-90.0),
        lowerLeftLongitude = (longitude - longitudeDelta).coerceAtLeast(-180.0),
        upperRightLatitude = (latitude + latitudeDelta).coerceAtMost(90.0),
        upperRightLongitude = (longitude + longitudeDelta).coerceAtMost(180.0)
    )
}

private fun Address.toResolvedPlace(): ResolvedPlace {
    val addressLine = getAddressLine(0)?.trim().orEmpty()
    val feature = featureName?.trim().orEmpty()
    val featureLooksLikeName = feature.any(Char::isLetter) &&
        !addressLine.equals(feature, ignoreCase = true) &&
        !addressLine.startsWith(feature, ignoreCase = true)

    val displayLabel = when {
        featureLooksLikeName && addressLine.isNotBlank() -> "$feature — $addressLine"
        addressLine.isNotBlank() -> addressLine
        else -> listOfNotNull(featureName, locality, adminArea)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(", ")
            .ifBlank { "Saved place" }
    }

    return ResolvedPlace(
        label = displayLabel,
        latitude = latitude,
        longitude = longitude
    )
}

private fun List<ResolvedPlace>.distinctPlaces(maxResults: Int): List<ResolvedPlace> =
    distinctBy { "${it.latitude.formatCoordinate()},${it.longitude.formatCoordinate()}" }
        .take(maxResults)

private fun Double.formatCoordinate(): String = String.format(Locale.US, "%.5f", this)
