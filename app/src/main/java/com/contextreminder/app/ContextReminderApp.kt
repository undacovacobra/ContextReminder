package com.contextreminder.app

import android.app.Application

class ContextReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.ensureChannel(this)
        GeofenceRegistrar(this).sync()
    }
}
