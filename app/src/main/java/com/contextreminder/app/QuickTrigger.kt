package com.contextreminder.app

enum class QuickTrigger(val wireName: String) {
    PLACE("place"),
    CALL("call"),
    APP("app"),
    NOTIFICATION("notification");

    companion object {
        fun fromWireName(value: String?): QuickTrigger? = entries.firstOrNull { it.wireName == value }
    }
}
