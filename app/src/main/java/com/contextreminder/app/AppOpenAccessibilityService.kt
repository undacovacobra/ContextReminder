package com.contextreminder.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.contextreminder.core.TriggerEvent

class AppOpenAccessibilityService : AccessibilityService() {
    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType ?: return
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        val packageName = event.packageName?.toString()?.takeIf(String::isNotBlank) ?: return
        if (packageName == this.packageName) {
            lastPackage = packageName
            return
        }
        if (packageName == lastPackage) return

        lastPackage = packageName
        RuleCoordinator(this).handle(TriggerEvent.AppOpened(packageName))
    }

    override fun onInterrupt() = Unit
}
