package com.contextreminder.app

import android.content.Context
import com.contextreminder.core.ReminderRule

class RuleStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun load(): List<ReminderRule> = RuleJsonCodec.decode(preferences.getString(KEY_RULES, null))

    @Synchronized
    fun save(rules: List<ReminderRule>) {
        preferences.edit().putString(KEY_RULES, RuleJsonCodec.encode(rules)).apply()
    }

    @Synchronized
    fun upsert(rule: ReminderRule) {
        val rules = load().toMutableList()
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index >= 0) rules[index] = rule else rules.add(rule)
        save(rules)
    }

    @Synchronized
    fun delete(id: String) {
        save(load().filterNot { it.id == id })
    }

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean) {
        save(load().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    @Synchronized
    fun markFired(id: String, epochMs: Long, disableAfterFire: Boolean) {
        save(load().map { rule ->
            if (rule.id != id) rule
            else rule.copy(
                lastTriggeredAtEpochMs = epochMs,
                enabled = if (disableAfterFire) false else rule.enabled
            )
        })
    }

    private companion object {
        const val PREFS_NAME = "context_reminder_rules"
        const val KEY_RULES = "rules_json"
    }
}
