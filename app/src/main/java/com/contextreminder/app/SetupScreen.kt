package com.contextreminder.app

import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private data class SetupItem(
    val title: String,
    val description: String,
    val enabled: Boolean,
    val actionLabel: String,
    val onAction: () -> Unit
)

@Composable
fun SetupScreen(
    modifier: Modifier = Modifier,
    onPermissionsChanged: () -> Unit,
    onOpenPrivacy: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    var pendingDisclosure by remember { mutableStateOf<SensitiveAccessKind?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
        onPermissionsChanged()
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
        onPermissionsChanged()
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresh++
        onPermissionsChanged()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh++
                onPermissionsChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    @Suppress("UNUSED_VARIABLE")
    val token = refresh

    val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val backgroundLocationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    val accessibilityEnabled = isAccessibilityEnabled(context)
    val notificationAccessEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    val callScreeningEnabled = isCallScreeningRoleHeld(context)
    val overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun continueSensitiveAccess(kind: SensitiveAccessKind) {
        when (kind) {
            SensitiveAccessKind.BACKGROUND_LOCATION -> openAppDetails(context)
            SensitiveAccessKind.ACCESSIBILITY -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            SensitiveAccessKind.NOTIFICATION_ACCESS -> context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            SensitiveAccessKind.OVERLAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            }
            SensitiveAccessKind.CALL_SCREENING -> {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                }
            }
        }
    }

    pendingDisclosure?.let { kind ->
        SensitiveAccessDisclosureDialog(
            kind = kind,
            onContinue = {
                pendingDisclosure = null
                continueSensitiveAccess(kind)
            },
            onCancel = { pendingDisclosure = null }
        )
    }

    val items = listOf(
        SetupItem(
            "Show reminders",
            "Allows Cue to display a notification when one of your reminder rules fires.",
            notificationsGranted,
            if (notificationsGranted) "Enabled" else "Allow"
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        SetupItem(
            "Show caller reminders over other apps",
            "Lets caller reminders appear below the phone call controls instead of being hidden behind them.",
            overlayEnabled,
            if (overlayEnabled) "Enabled" else "Review access"
        ) {
            pendingDisclosure = SensitiveAccessKind.OVERLAY
        },
        SetupItem(
            "Current location",
            "Used when you choose your current position or create an arrival/departure reminder.",
            fineLocationGranted,
            if (fineLocationGranted) "Enabled" else "Allow"
        ) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        SetupItem(
            "Background location",
            "Needed so arrival and departure reminders can fire when Cue is closed.",
            backgroundLocationGranted,
            if (backgroundLocationGranted) "Enabled" else "Review access"
        ) {
            pendingDisclosure = SensitiveAccessKind.BACKGROUND_LOCATION
        },
        SetupItem(
            "App-open detection",
            "Accessibility access detects only which selected app comes to the foreground; Cue does not retrieve window content.",
            accessibilityEnabled,
            if (accessibilityEnabled) "Enabled" else "Review access"
        ) {
            pendingDisclosure = SensitiveAccessKind.ACCESSIBILITY
        },
        SetupItem(
            "Notification detection",
            "Notification access lets rules react to notifications from apps you select, including optional text matching.",
            notificationAccessEnabled,
            if (notificationAccessEnabled) "Enabled" else "Review access"
        ) {
            pendingDisclosure = SensitiveAccessKind.NOTIFICATION_ACCESS
        },
        SetupItem(
            "Caller detection",
            "Makes Cue the phone's call-screening app so it can match incoming numbers to caller reminders. Cue does not block calls.",
            callScreeningEnabled,
            if (callScreeningEnabled) "Enabled" else "Review access"
        ) {
            pendingDisclosure = SensitiveAccessKind.CALL_SCREENING
        }
    )

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Setup", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Only enable the access used by the reminder types you want. Reminder rules are stored locally on this device.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = onOpenPrivacy) {
            Text("Privacy & data access")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.title }) { item ->
                SetupCard(item)
            }
        }
    }
}

@Composable
private fun SetupCard(item: SetupItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (item.enabled) "Ready" else "Needs setup", style = MaterialTheme.typography.labelMedium)
                }
                if (item.enabled) {
                    OutlinedButton(onClick = item.onAction, enabled = false) { Text("Enabled") }
                } else {
                    Button(onClick = item.onAction) { Text(item.actionLabel) }
                }
            }
            Text(item.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val component = ComponentName(context, AppOpenAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabled.split(':').any { it.equals(component, ignoreCase = true) }
}

private fun isCallScreeningRoleHeld(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val roleManager = context.getSystemService(RoleManager::class.java)
    return roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
        roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
}

private fun openAppDetails(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    )
}
