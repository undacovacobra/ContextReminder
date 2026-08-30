package com.contextreminder.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier

fun Modifier.statusBarsPadding(): Modifier = windowInsetsPadding(WindowInsets.statusBars)
