package com.contextreminder.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.contextreminder.core.PlayDisclosureCopy

@Composable
fun PrivacyScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Privacy & data access", style = MaterialTheme.typography.headlineSmall)
            }
            item {
                Text(
                    "Cue is designed as a local-first reminder app. It has no Cue account, advertising SDK, analytics SDK, or Cue-operated cloud server. Reminder text, selected phone numbers, app package names, notification matching text, and saved place coordinates are stored locally on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item { PrivacyCard("Location", PlayDisclosureCopy.backgroundLocation) }
            item { PrivacyCard("App-open detection", PlayDisclosureCopy.accessibility) }
            item { PrivacyCard("Notification access", PlayDisclosureCopy.notificationAccess) }
            item { PrivacyCard("Caller detection", PlayDisclosureCopy.callScreening) }
            item { PrivacyCard("Display over other apps", PlayDisclosureCopy.overlay) }
            item {
                PrivacyCard(
                    "Contacts",
                    "Cue does not request broad access to your contacts. When you choose a person for a caller reminder, Android's contact picker gives Cue temporary access only to the contact entry you selected so Cue can save the selected name and phone number locally."
                )
            }
            item {
                PrivacyCard(
                    "System services",
                    "Android and Google system services may process device location, geofencing, or place-search requests according to your device settings and their own privacy terms. Cue does not receive that information on a Cue-operated server."
                )
            }
            item {
                PrivacyCard(
                    "Deleting your data",
                    "Delete individual reminders inside Cue, or clear/uninstall the app to remove Cue's locally stored reminder data. Cue does not create an online account, so there is no remote account data to delete."
                )
            }
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Back to setup")
        }
    }
}

@Composable
private fun PrivacyCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}
