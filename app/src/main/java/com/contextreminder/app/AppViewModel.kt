package com.contextreminder.app

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.contextreminder.core.ReminderRule
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class InstalledApp(
    val label: String,
    val packageName: String
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
