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
    onPermissionsChanged: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
        onPermissionsChanged()
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
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
    val contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val accessibilityEnabled = isAccessibilityEnabled(context)
    val notificationAccessEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    val callScreeningEnabled = isCallScreeningRoleHeld(context)
    val overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    val items = listOf(
        SetupItem(
            "Show reminders",
            "Allows Context Reminder to display the reminder when a rule fires.",
            notificationsGranted,
            if (notificationsGranted) "Enabled" else "Allow"
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        SetupItem(
            "Show reminders over other apps",
            "Lets caller reminders appear as a banner below the phone call controls instead of being hidden behind them.",
            overlayEnabled,
            if (overlayEnabled) "Enabled" else "Open settings"
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        },
        SetupItem(
            "Current location",
            "Needed to capture places and use location reminders.",
            fineLocationGranted,
            if (fineLocationGranted) "Enabled" else "Allow"
        ) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        SetupItem(
            "Background location",
            "Needed so arrival and departure reminders can fire when the app is closed.",
            backgroundLocationGranted,
            if (backgroundLocationGranted) "Enabled" else "Open settings"
        ) {
            openAppDetails(context)
        },
        SetupItem(
            "App-open detection",
            "Accessibility access lets the app detect when one of your selected apps comes to the foreground.",
            accessibilityEnabled,
            if (accessibilityEnabled) "Enabled" else "Open settings"
        ) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        SetupItem(
            "Notification detection",
            "Notification access lets your rules react to notifications from selected apps.",
            notificationAccessEnabled,
            if (notificationAccessEnabled) "Enabled" else "Open settings"
        ) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        },
        SetupItem(
            "Contacts",
            "Lets caller reminders work for numbers already saved in your contacts.",
            contactsGranted,
            if (contactsGranted) "Enabled" else "Allow"
        ) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        },
        SetupItem(
            "Caller detection",
            "Makes Context Reminder the phone's caller-ID/call-screening app so it can react while a selected person is calling. It does not block calls.",
            callScreeningEnabled,
            if (callScreeningEnabled) "Enabled" else "Choose app"
        ) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        }
    )

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Setup", style = MaterialTheme.typography.headlineSmall)
        Text(
            "You only need to enable the access used by the trigger types you want. All reminder rules stay on this phone.",
            style = MaterialTheme.typography.bodyMedium
        )
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
