package com.contextreminder.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.contextreminder.core.PlayDisclosureCopy

enum class SensitiveAccessKind(val title: String, val body: String) {
    BACKGROUND_LOCATION(
        title = "Background location",
        body = PlayDisclosureCopy.backgroundLocation
    ),
    ACCESSIBILITY(
        title = "App-open detection",
        body = PlayDisclosureCopy.accessibility
    ),
    NOTIFICATION_ACCESS(
        title = "Notification access",
        body = PlayDisclosureCopy.notificationAccess
    ),
    CALL_SCREENING(
        title = "Caller detection",
        body = PlayDisclosureCopy.callScreening
    ),
    OVERLAY(
        title = "Show caller reminders over apps",
        body = PlayDisclosureCopy.overlay
    )
}

@Composable
fun SensitiveAccessDisclosureDialog(
    kind: SensitiveAccessKind,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(kind.title) },
        text = { Text(kind.body) },
        confirmButton = {
            TextButton(onClick = onContinue) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Not now") }
        }
    )
}
