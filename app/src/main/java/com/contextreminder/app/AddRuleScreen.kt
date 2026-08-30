package com.contextreminder.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.contextreminder.core.GeofenceTransition
import com.contextreminder.core.ReminderRule
import com.contextreminder.core.RepeatPolicy
import com.contextreminder.core.RuleCondition
import com.contextreminder.core.Trigger
import java.time.DayOfWeek
import java.util.UUID

private enum class TriggerChoice(val label: String) {
    LOCATION("Location"), CALLER("Caller"), APP("Open app"), NOTIFICATION("Notification")
}

private enum class RepeatChoice(val label: String) {
    EVERY_TIME("Every time"), ONCE("Once"), COOLDOWN("Cooldown")
}

@Composable
fun AddRuleScreen(
    viewModel: AppViewModel,
    apps: List<InstalledApp>,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var triggerChoice by remember { mutableStateOf(TriggerChoice.LOCATION) }
    var title by remember { mutableStateOf("Reminder") }
    var reminderText by remember { mutableStateOf("") }
    var repeatChoice by remember { mutableStateOf(RepeatChoice.EVERY_TIME) }
    var cooldownMinutes by remember { mutableStateOf("60") }
    var useTimeWindow by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("17:00") }
    var selectedDays by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    var error by remember { mutableStateOf<String?>(null) }

    var showAppPicker by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var notificationContains by remember { mutableStateOf("") }

    var contactName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    var locationLabel by remember { mutableStateOf("Saved place") }
    var locationRadius by remember { mutableStateOf("150") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationTransition by remember { mutableStateOf(GeofenceTransition.ENTER) }
    var locating by remember { mutableStateOf(false) }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(0).orEmpty()
                contactNumber = cursor.getString(1).orEmpty()
            }
        }
    }

    fun captureLocation() {
        locating = true
        viewModel.captureCurrentLocation { location ->
            locating = false
            if (location == null) {
                error = "Could not get your current location. Make sure Location is turned on."
            } else {
                latitude = location.latitude
                longitude = location.longitude
                error = null
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) captureLocation() else error = "Location permission is required to capture this place."
    }

    fun requestCurrentLocation() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) captureLocation()
        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("New reminder", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onCancel) { Text("Cancel") }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionCard("1. When should it happen?") {
                    TriggerChoice.entries.chunked(2).forEach { rowChoices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowChoices.forEach { choice ->
                                FilterChip(
                                    selected = triggerChoice == choice,
                                    onClick = {
                                        triggerChoice = choice
                                        if (choice != TriggerChoice.APP && choice != TriggerChoice.NOTIFICATION) selectedApp = null
                                    },
                                    label = { Text(choice.label) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when (triggerChoice) {
                        TriggerChoice.LOCATION -> {
                            OutlinedTextField(
                                value = locationLabel,
                                onValueChange = { locationLabel = it },
                                label = { Text("Place name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = locationTransition == GeofenceTransition.ENTER,
                                    onClick = { locationTransition = GeofenceTransition.ENTER },
                                    label = { Text("Arrive") }
                                )
                                FilterChip(
                                    selected = locationTransition == GeofenceTransition.EXIT,
                                    onClick = { locationTransition = GeofenceTransition.EXIT },
                                    label = { Text("Leave") }
                                )
                            }
                            OutlinedTextField(
                                value = locationRadius,
                                onValueChange = { locationRadius = it.filter(Char::isDigit).take(4) },
                                label = { Text("Radius in meters (50–1000)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(onClick = ::requestCurrentLocation, enabled = !locating) {
                                Text(if (locating) "Getting location…" else if (latitude == null) "Use my current location" else "Update current location")
                            }
                            latitude?.let { lat ->
                                Text("Saved: ${"%.5f".format(lat)}, ${"%.5f".format(longitude ?: 0.0)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TriggerChoice.CALLER -> {
                            Button(onClick = {
                                contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                            }) {
                                Text(if (contactNumber.isBlank()) "Choose a contact" else "Choose another contact")
                            }
                            if (contactNumber.isNotBlank()) {
                                Text("${contactName.ifBlank { "Selected caller" }} — $contactNumber")
                            }
                        }
                        TriggerChoice.APP -> {
                            AppSelectionButton(selectedApp, "Choose an app") { showAppPicker = true }
                        }
                        TriggerChoice.NOTIFICATION -> {
                            AppSelectionButton(selectedApp, "Choose notification app") { showAppPicker = true }
                            OutlinedTextField(
                                value = notificationContains,
                                onValueChange = { notificationContains = it },
                                label = { Text("Only if notification contains (optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                SectionCard("2. What should I remind you?") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reminderText,
                        onValueChange = { reminderText = it },
                        label = { Text("Reminder") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            item {
                SectionCard("3. Conditions") {
                    Text("Days (none selected means every day)", style = MaterialTheme.typography.bodyMedium)
                    DayOfWeek.entries.chunked(4).forEach { days ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            days.forEach { day ->
                                FilterChip(
                                    selected = day in selectedDays,
                                    onClick = {
                                        selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                                    },
                                    label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = useTimeWindow,
                            onClick = { useTimeWindow = !useTimeWindow },
                            label = { Text("Use time window") }
                        )
                    }
                    if (useTimeWindow) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it.take(5) },
                                label = { Text("Start HH:mm") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = { endTime = it.take(5) },
                                label = { Text("End HH:mm") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                SectionCard("4. Repeat") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatChoice.entries.forEach { choice ->
                            FilterChip(
                                selected = repeatChoice == choice,
                                onClick = { repeatChoice = choice },
                                label = { Text(choice.label) }
                            )
                        }
                    }
                    if (repeatChoice == RepeatChoice.COOLDOWN) {
                        OutlinedTextField(
                            value = cooldownMinutes,
                            onValueChange = { cooldownMinutes = it.filter(Char::isDigit).take(5) },
                            label = { Text("Cooldown minutes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            error?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(message, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val id = UUID.randomUUID().toString()
                val startMinute = if (useTimeWindow) parseTime(startTime) else null
                val endMinute = if (useTimeWindow) parseTime(endTime) else null
                val radius = locationRadius.toFloatOrNull()?.coerceIn(50f, 1000f) ?: 150f

                val trigger = when (triggerChoice) {
                    TriggerChoice.LOCATION -> {
                        val lat = latitude
                        val lon = longitude
                        if (lat == null || lon == null) {
                            error = "Capture the location before saving."
                            return@Button
                        }
                        Trigger.Geofence(id, locationTransition, lat, lon, radius, locationLabel.ifBlank { "Saved place" })
                    }
                    TriggerChoice.CALLER -> {
                        if (contactNumber.isBlank()) {
                            error = "Choose a caller before saving."
                            return@Button
                        }
                        Trigger.IncomingCall(contactNumber)
                    }
                    TriggerChoice.APP -> {
                        val app = selectedApp
                        if (app == null) {
                            error = "Choose an app before saving."
                            return@Button
                        }
                        Trigger.AppOpened(app.packageName)
                    }
                    TriggerChoice.NOTIFICATION -> {
                        val app = selectedApp
                        if (app == null) {
                            error = "Choose a notification app before saving."
                            return@Button
                        }
                        Trigger.NotificationReceived(app.packageName, notificationContains.trim().takeIf(String::isNotBlank))
                    }
                }

                if (reminderText.isBlank()) {
                    error = "Enter what you want to be reminded about."
                    return@Button
                }
                if (useTimeWindow && (startMinute == null || endMinute == null)) {
                    error = "Use time values like 09:00 or 17:30."
                    return@Button
                }

                val repeatPolicy = when (repeatChoice) {
                    RepeatChoice.EVERY_TIME -> RepeatPolicy.EveryTime
                    RepeatChoice.ONCE -> RepeatPolicy.Once
                    RepeatChoice.COOLDOWN -> RepeatPolicy.Cooldown(cooldownMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 60)
                }

                viewModel.addRule(
                    ReminderRule(
                        id = id,
                        title = title.ifBlank { "Reminder" },
                        reminderText = reminderText.trim(),
                        trigger = trigger,
                        condition = RuleCondition(selectedDays, startMinute, endMinute),
                        repeatPolicy = repeatPolicy,
                        createdAtEpochMs = System.currentTimeMillis()
                    )
                )
                onSaved()
            }
        ) {
            Text("Save reminder")
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = apps,
            onDismiss = { showAppPicker = false },
            onPick = {
                selectedApp = it
                showAppPicker = false
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun AppSelectionButton(selected: InstalledApp?, emptyLabel: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text(selected?.let { "${it.label} (${it.packageName})" } ?: emptyLabel)
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search, apps) {
        apps.filter { it.label.contains(search, true) || it.packageName.contains(search, true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Choose app", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable { onPick(app) }.padding(vertical = 10.dp)
                        ) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}

private fun parseTime(value: String): Int? {
    val parts = value.trim().split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}
