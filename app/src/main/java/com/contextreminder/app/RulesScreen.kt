package com.contextreminder.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.contextreminder.core.ReminderRule
import com.contextreminder.core.RepeatPolicy
import com.contextreminder.core.Trigger

@Composable
fun RulesScreen(
    modifier: Modifier = Modifier,
    rules: List<ReminderRule>,
    apps: List<InstalledApp>,
    onAdd: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Context Reminder", style = MaterialTheme.typography.headlineSmall)
                Text("When something happens, remind me.", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onAdd) { Text("+ New") }
        }

        if (rules.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("No reminders yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Create a reminder for a place, caller, app, or notification.")
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onAdd) { Text("Create reminder") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(rule, apps, onToggle, onDelete)
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: ReminderRule,
    apps: List<InstalledApp>,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.reminderText, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(triggerDescription(rule.trigger, apps), style = MaterialTheme.typography.bodyMedium)
                    Text(repeatDescription(rule.repeatPolicy), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = rule.enabled, onCheckedChange = { onToggle(rule.id, it) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { onDelete(rule.id) }) { Text("Delete") }
            }
        }
    }
}

private fun triggerDescription(trigger: Trigger, apps: List<InstalledApp>): String = when (trigger) {
    is Trigger.AppOpened -> "When ${appLabel(trigger.packageName, apps)} opens"
    is Trigger.IncomingCall -> "When ${trigger.phoneNumber} calls"
    is Trigger.NotificationReceived -> buildString {
        append("Notification from ${appLabel(trigger.packageName, apps)}")
        trigger.textContains?.takeIf(String::isNotBlank)?.let { append(" containing “$it”") }
    }
    is Trigger.Geofence -> "When you ${trigger.transition.name.lowercase()} ${trigger.label}"
}

private fun appLabel(packageName: String, apps: List<InstalledApp>): String =
    apps.firstOrNull { it.packageName == packageName }?.label ?: packageName

private fun repeatDescription(policy: RepeatPolicy): String = when (policy) {
    RepeatPolicy.EveryTime -> "Repeats every time"
    RepeatPolicy.Once -> "One time only"
    is RepeatPolicy.Cooldown -> "Repeats after ${policy.minutes} minute cooldown"
}
