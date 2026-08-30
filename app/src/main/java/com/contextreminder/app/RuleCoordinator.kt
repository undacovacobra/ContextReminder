package com.contextreminder.app

import android.content.Context
import com.contextreminder.core.ReminderRule
import com.contextreminder.core.RuleEngine
import com.contextreminder.core.TriggerEvent
import java.time.Instant
import java.time.ZoneId

class RuleCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val store = RuleStore(appContext)

    @Synchronized
    fun handle(event: TriggerEvent): List<ReminderRule> {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val rules = store.load()
        val firedRules = mutableListOf<ReminderRule>()

        rules.forEach { rule ->
            val evaluation = RuleEngine.evaluate(rule, event, now, zone)
            if (evaluation.shouldFire) {
                ReminderNotifier.show(appContext, rule)
                firedRules += rule
                store.markFired(rule.id, now.toEpochMilli(), evaluation.disableAfterFire)
            }
        }
        return firedRules
    }
}
