package com.contextreminder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class QuickAddActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trigger = QuickTrigger.fromWireName(intent.getStringExtra(QuickAddWidgetProvider.EXTRA_TRIGGER))
            ?: QuickTrigger.PLACE

        setContent {
            MaterialTheme {
                val apps by viewModel.apps.collectAsState()
                AddRuleScreen(
                    viewModel = viewModel,
                    apps = apps,
                    onCancel = ::finish,
                    onSaved = ::finish,
                    initialTrigger = trigger,
                    quickMode = true
                )
            }
        }
    }
}
