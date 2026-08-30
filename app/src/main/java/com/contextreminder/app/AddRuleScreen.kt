package com.contextreminder.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.contextreminder.core.GeofenceTransition
import com.contextreminder.core.ReminderDraft
import com.contextreminder.core.ReminderDraftTrigger
import com.contextreminder.core.ReminderDraftValidator
import com.contextreminder.core.ReminderRule
import com.contextreminder.core.RepeatPolicy
import com.contextreminder.core.RuleCondition
import com.contextreminder.core.Trigger
import java.time.DayOfWeek
import java.util.UUID
import kotlinx.coroutines.delay

private enum class TriggerChoice(
    val label: String,
    val draftTrigger: ReminderDraftTrigger
) {
    LOCATION("Place", ReminderDraftTrigger.LOCATION),
    CALLER("Person calls", ReminderDraftTrigger.CALLER),
    APP("Open app", ReminderDraftTrigger.APP),
    NOTIFICATION("Notification", ReminderDraftTrigger.NOTIFICATION)
}

private fun QuickTrigger.toTriggerChoice(): TriggerChoice = when (this) {
    QuickTrigger.PLACE -> TriggerChoice.LOCATION
    QuickTrigger.CALL -> TriggerChoice.CALLER
    QuickTrigger.APP -> TriggerChoice.APP
    QuickTrigger.NOTIFICATION -> TriggerChoice.NOTIFICATION
}

private enum class RepeatChoice(val label: String) {
    EVERY_TIME("Every time"),
    ONCE("Once"),
    COOLDOWN("Cooldown")
}

@Composable
fun AddRuleScreen(
    viewModel: AppViewModel,
    apps: List<InstalledApp>,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    initialTrigger: QuickTrigger? = null,
    quickMode: Boolean = false
) {
    val context = LocalContext.current
    val reminderFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var reminderText by remember { mutableStateOf("") }
    var triggerChoice by remember(initialTrigger) {
        mutableStateOf(initialTrigger?.toTriggerChoice() ?: TriggerChoice.LOCATION)
    }
    var reminderError by remember { mutableStateOf<String?>(null) }
    var triggerError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }

    var showOptions by remember { mutableStateOf(false) }
    var repeatChoice by remember { mutableStateOf(RepeatChoice.EVERY_TIME) }
    var cooldownMinutes by remember { mutableStateOf("60") }
    var selectedDays by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    var useTimeWindow by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("17:00") }

    var showAppPicker by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var notificationContains by remember { mutableStateOf("") }

    var contactName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    var locationQuery by remember { mutableStateOf("") }
    var locationRadius by remember { mutableStateOf("150") }
    var selectedPlace by remember { mutableStateOf<ResolvedPlace?>(null) }
    var placeResults by remember { mutableStateOf(emptyList<ResolvedPlace>()) }
    var searchingPlaces by remember { mutableStateOf(false) }
    var searchAttempted by remember { mutableStateOf(false) }
    var locationTransition by remember { mutableStateOf(GeofenceTransition.ENTER) }
    var locating by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(quickMode) {
        if (quickMode) {
            delay(150)
            reminderFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(locationQuery, triggerChoice, selectedPlace) {
        if (triggerChoice != TriggerChoice.LOCATION || selectedPlace != null) {
            searchingPlaces = false
            return@LaunchedEffect
        }
        val query = locationQuery.trim()
        if (query.length < 3) {
            searchingPlaces = false
            placeResults = emptyList()
            searchAttempted = false
            return@LaunchedEffect
        }
        delay(500)
        searchingPlaces = true
        searchAttempted = false
        viewModel.searchPlaces(query) { results ->
            placeResults = results
            searchingPlaces = false
            searchAttempted = true
        }
    }

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
                triggerError = null
            }
        }
    }

    fun captureLocation() {
        locating = true
        viewModel.captureCurrentLocation { location ->
            locating = false
            if (location == null) {
                triggerError = "Could not get your current location. Make sure Location is turned on."
            } else {
                selectedPlace = ResolvedPlace(
                    label = "Current location",
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                locationQuery = "Current location"
                placeResults = emptyList()
                searchAttempted = false
                triggerError = null
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            captureLocation()
        } else {
            triggerError = "Location permission is required to use your current location."
        }
    }

    fun requestCurrentLocation() {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            captureLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun currentRepeatPolicy(): RepeatPolicy = when (repeatChoice) {
        RepeatChoice.EVERY_TIME -> RepeatPolicy.EveryTime
        RepeatChoice.ONCE -> RepeatPolicy.Once
        RepeatChoice.COOLDOWN -> RepeatPolicy.Cooldown(
            cooldownMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 60
        )
    }

    fun persistRule(
        startMinute: Int?,
        endMinute: Int?,
        triggerBuilder: (String) -> Trigger
    ) {
        val id = UUID.randomUUID().toString()
        val rule = ReminderRule(
            id = id,
            reminderText = reminderText.trim(),
            trigger = triggerBuilder(id),
            condition = RuleCondition(selectedDays, startMinute, endMinute),
            repeatPolicy = currentRepeatPolicy(),
            createdAtEpochMs = System.currentTimeMillis()
        )
        saving = true
        viewModel.addRule(rule) { result ->
            saving = false
            result.onSuccess {
                Toast.makeText(context, "Reminder saved", Toast.LENGTH_SHORT).show()
                onSaved()
            }.onFailure { error ->
                triggerError = error.message ?: "Android could not activate this reminder."
            }
        }
    }

    fun submitReminder() {
        val startMinute = if (useTimeWindow) parseTime(startTime) else null
        val endMinute = if (useTimeWindow) parseTime(endTime) else null

        val validation = ReminderDraftValidator.validate(
            ReminderDraft(
                reminderText = reminderText,
                triggerType = triggerChoice.draftTrigger,
                locationQuery = locationQuery,
                hasResolvedLocation = selectedPlace != null,
                callerNumber = contactNumber,
                packageName = selectedApp?.packageName.orEmpty(),
                useTimeWindow = useTimeWindow,
                startMinute = startMinute,
                endMinute = endMinute
            )
        )

        reminderError = validation.reminderError
        triggerError = validation.triggerError
        timeError = validation.timeError
        if (!validation.isValid) return

        when (triggerChoice) {
            TriggerChoice.LOCATION -> {
                val place = selectedPlace ?: return
                val radius = locationRadius.toFloatOrNull()?.coerceIn(50f, 1000f) ?: 150f
                persistRule(startMinute, endMinute) { id ->
                    Trigger.Geofence(
                        placeId = id,
                        transition = locationTransition,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        radiusMeters = radius,
                        label = place.label
                    )
                }
            }

            TriggerChoice.CALLER -> {
                persistRule(startMinute, endMinute) {
                    Trigger.IncomingCall(contactNumber.trim())
                }
            }

            TriggerChoice.APP -> {
                val app = selectedApp ?: return
                persistRule(startMinute, endMinute) {
                    Trigger.AppOpened(app.packageName)
                }
            }

            TriggerChoice.NOTIFICATION -> {
                val app = selectedApp ?: return
                persistRule(startMinute, endMinute) {
                    Trigger.NotificationReceived(
                        packageName = app.packageName,
                        textContains = notificationContains.trim().takeIf(String::isNotBlank)
                    )
                }
            }
        }
    }

    val busy = locating || searchingPlaces || saving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (quickMode) "Quick reminder" else "New reminder",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onCancel, enabled = !saving) { Text("Cancel") }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = reminderText,
                        onValueChange = {
                            reminderText = it
                            reminderError = null
                        },
                        label = { Text("Remind me…") },
                        placeholder = { Text("What do you want to remember?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(reminderFocusRequester),
                        minLines = if (quickMode) 1 else 2,
                        isError = reminderError != null
                    )
                    reminderError?.let { ErrorText(it) }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (quickMode) {
                            Text("When: ${triggerChoice.label}", style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text("When…", style = MaterialTheme.typography.titleMedium)
                            TriggerChoice.entries.chunked(2).forEach { choices ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    choices.forEach { choice ->
                                        FilterChip(
                                            selected = triggerChoice == choice,
                                            onClick = {
                                                triggerChoice = choice
                                                triggerError = null
                                                if (
                                                    choice != TriggerChoice.APP &&
                                                    choice != TriggerChoice.NOTIFICATION
                                                ) {
                                                    selectedApp = null
                                                }
                                            },
                                            label = { Text(choice.label) }
                                        )
                                    }
                                }
                            }
                        }

                        when (triggerChoice) {
                            TriggerChoice.LOCATION -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = locationTransition == GeofenceTransition.ENTER,
                                        onClick = { locationTransition = GeofenceTransition.ENTER },
                                        label = { Text("I arrive") }
                                    )
                                    FilterChip(
                                        selected = locationTransition == GeofenceTransition.EXIT,
                                        onClick = { locationTransition = GeofenceTransition.EXIT },
                                        label = { Text("I leave") }
                                    )
                                }
                                OutlinedTextField(
                                    value = locationQuery,
                                    onValueChange = {
                                        locationQuery = it
                                        selectedPlace = null
                                        placeResults = emptyList()
                                        searchAttempted = false
                                        triggerError = null
                                    },
                                    label = { Text("Address or place") },
                                    placeholder = { Text("123 Main St, North Port, FL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = triggerError != null
                                )

                                if (searchingPlaces) {
                                    Text("Searching for places…", style = MaterialTheme.typography.bodySmall)
                                }

                                if (selectedPlace == null && placeResults.isNotEmpty()) {
                                    Text("Choose the correct place", style = MaterialTheme.typography.labelLarge)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        placeResults.forEach { place ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedPlace = place
                                                        locationQuery = place.label
                                                        placeResults = emptyList()
                                                        searchAttempted = false
                                                        triggerError = null
                                                    },
                                                shape = MaterialTheme.shapes.medium,
                                                tonalElevation = 2.dp
                                            ) {
                                                Text(
                                                    place.label,
                                                    modifier = Modifier.padding(12.dp),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                } else if (
                                    selectedPlace == null &&
                                    searchAttempted &&
                                    locationQuery.trim().length >= 3
                                ) {
                                    Text(
                                        "No matching places found. Try a fuller address, city, and state.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                selectedPlace?.let { place ->
                                    Text(
                                        "✓ Using: ${place.label}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                OutlinedButton(
                                    onClick = ::requestCurrentLocation,
                                    enabled = !saving
                                ) {
                                    Text(if (locating) "Getting location…" else "Use my current location")
                                }
                            }

                            TriggerChoice.CALLER -> {
                                OutlinedButton(
                                    onClick = {
                                        contactPicker.launch(
                                            Intent(
                                                Intent.ACTION_PICK,
                                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                            )
                                        )
                                    }
                                ) {
                                    Text(if (contactName.isBlank()) "Choose a contact" else contactName)
                                }
                                OutlinedTextField(
                                    value = contactNumber,
                                    onValueChange = {
                                        contactNumber = it
                                        triggerError = null
                                    },
                                    label = { Text("Phone number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = triggerError != null
                                )
                            }

                            TriggerChoice.APP -> {
                                AppSelectionButton(selectedApp, "Choose an app") {
                                    showAppPicker = true
                                }
                            }

                            TriggerChoice.NOTIFICATION -> {
                                AppSelectionButton(selectedApp, "Choose an app") {
                                    showAppPicker = true
                                }
                                OutlinedTextField(
                                    value = notificationContains,
                                    onValueChange = { notificationContains = it },
                                    label = { Text("Notification contains (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        triggerError?.let { ErrorText(it) }
                    }
                }
            }

            if (!quickMode) {
                item {
                    TextButton(onClick = { showOptions = !showOptions }) {
                        Text(if (showOptions) "Hide options" else "Options")
                    }
                }
            }

            if (!quickMode && showOptions) {
                item {
                    SectionCard("Options") {
                        Text("Repeat", style = MaterialTheme.typography.bodyMedium)
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
                                onValueChange = {
                                    cooldownMinutes = it.filter(Char::isDigit).take(5)
                                },
                                label = { Text("Cooldown minutes") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Text("Days (none selected = every day)")
                        DayOfWeek.entries.chunked(4).forEach { days ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                days.forEach { day ->
                                    FilterChip(
                                        selected = day in selectedDays,
                                        onClick = {
                                            selectedDays = if (day in selectedDays) {
                                                selectedDays - day
                                            } else {
                                                selectedDays + day
                                            }
                                        },
                                        label = {
                                            Text(
                                                day.name
                                                    .take(3)
                                                    .lowercase()
                                                    .replaceFirstChar(Char::uppercase)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        FilterChip(
                            selected = useTimeWindow,
                            onClick = {
                                useTimeWindow = !useTimeWindow
                                timeError = null
                            },
                            label = { Text("Only during certain hours") }
                        )
                        if (useTimeWindow) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = {
                                        startTime = it.take(5)
                                        timeError = null
                                    },
                                    label = { Text("Start") },
                                    placeholder = { Text("09:00") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    isError = timeError != null
                                )
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = {
                                        endTime = it.take(5)
                                        timeError = null
                                    },
                                    label = { Text("End") },
                                    placeholder = { Text("17:00") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    isError = timeError != null
                                )
                            }
                            timeError?.let { ErrorText(it) }
                        }

                        if (triggerChoice == TriggerChoice.LOCATION) {
                            OutlinedTextField(
                                value = locationRadius,
                                onValueChange = {
                                    locationRadius = it.filter(Char::isDigit).take(4)
                                },
                                label = { Text("Location radius in meters") },
                                supportingText = { Text("Default 150 m; allowed 50–1000 m") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            shadowElevation = 6.dp
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(52.dp),
                onClick = ::submitReminder,
                enabled = !busy
            ) {
                Text(
                    when {
                        saving -> "Saving…"
                        locating -> "Getting location…"
                        else -> "Save reminder"
                    }
                )
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = apps,
            onDismiss = { showAppPicker = false },
            onPick = {
                selectedApp = it
                triggerError = null
                showAppPicker = false
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun AppSelectionButton(
    selected: InstalledApp?,
    emptyLabel: String,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick) {
        Text(selected?.label ?: emptyLabel)
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
        apps.filter {
            it.label.contains(search, true) || it.packageName.contains(search, true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Choose app", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
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
