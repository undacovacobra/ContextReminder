package com.contextreminder.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class QuickAddWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
                setOnClickPendingIntent(R.id.widget_place, quickIntent(context, QuickTrigger.PLACE))
                setOnClickPendingIntent(R.id.widget_call, quickIntent(context, QuickTrigger.CALL))
                setOnClickPendingIntent(R.id.widget_app, quickIntent(context, QuickTrigger.APP))
                setOnClickPendingIntent(R.id.widget_notification, quickIntent(context, QuickTrigger.NOTIFICATION))
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun quickIntent(context: Context, trigger: QuickTrigger): PendingIntent {
        val intent = Intent(context, QuickAddActivity::class.java).apply {
            putExtra(EXTRA_TRIGGER, trigger.wireName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            9100 + trigger.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_TRIGGER = "quick_trigger"
    }
}
