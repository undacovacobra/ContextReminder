package com.contextreminder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ContextReminderRoot()
            }
        }
    }
}

private enum class RootPage { RULES, SETUP, ADD, PRIVACY }

@Composable
private fun ContextReminderRoot(viewModel: AppViewModel = viewModel()) {
    val rules by viewModel.rules.collectAsState()
    val apps by viewModel.apps.collectAsState()
    var page by remember { mutableStateOf(RootPage.RULES) }

    if (page == RootPage.ADD) {
        AddRuleScreen(
            viewModel = viewModel,
            apps = apps,
            onCancel = { page = RootPage.RULES },
            onSaved = { page = RootPage.RULES }
        )
        return
    }

    if (page == RootPage.PRIVACY) {
        PrivacyScreen(onBack = { page = RootPage.SETUP })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = page == RootPage.RULES,
                    onClick = { page = RootPage.RULES },
                    icon = { Text("◉") },
                    label = { Text("Reminders") }
                )
                NavigationBarItem(
                    selected = page == RootPage.SETUP,
                    onClick = { page = RootPage.SETUP },
                    icon = { Text("⚙") },
                    label = { Text("Setup") }
                )
            }
        }
    ) { padding ->
        when (page) {
            RootPage.RULES -> RulesScreen(
                modifier = Modifier.padding(padding),
                rules = rules,
                apps = apps,
                onAdd = { page = RootPage.ADD },
                onToggle = viewModel::setRuleEnabled,
                onDelete = viewModel::deleteRule
            )
            RootPage.SETUP -> SetupScreen(
                modifier = Modifier.padding(padding),
                onPermissionsChanged = viewModel::refresh,
                onOpenPrivacy = { page = RootPage.PRIVACY }
            )
            RootPage.ADD,
            RootPage.PRIVACY -> Unit
        }
    }
}
