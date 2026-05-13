package com.etrisad.zenith.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import java.util.Calendar

object ScreenUsageHelper {
    fun fetchAppUsageTodayTillNow(usageStatsManager: UsageStatsManager): Map<String, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()

        val usageMap = mutableMapOf<String, Long>()
        val lastResumedEvents = mutableMapOf<String, Long>()
        
        val events = usageStatsManager.queryEvents(start - (3 * 3600000), end)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> lastResumedEvents[event.packageName] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    lastResumedEvents.remove(event.packageName)?.let { resumeTime ->
                        if (event.timeStamp > start) {
                            val duration = event.timeStamp - maxOf(resumeTime, start)
                            usageMap[event.packageName] = usageMap.getOrDefault(event.packageName, 0L) + duration
                        }
                    }
                }
            }
        }
        
        lastResumedEvents.forEach { (packageName, resumeTime) ->
            if (end > start) {
                val duration = end - maxOf(resumeTime, start)
                usageMap[packageName] = usageMap.getOrDefault(packageName, 0L) + duration
            }
        }

        return usageMap.mapValues { it.value / 1000 }
    }
}
