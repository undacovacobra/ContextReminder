package com.contextreminder.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.contextreminder.core.TriggerEvent

class ReminderNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            sbn.notification.tickerText?.toString()
        ).joinToString(" ").takeIf(String::isNotBlank)

        RuleCoordinator(this).handle(
            TriggerEvent.NotificationReceived(
                packageName = sbn.packageName,
                title = title,
                text = text
            )
        )
    }
}
