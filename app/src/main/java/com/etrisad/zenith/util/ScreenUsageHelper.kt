package com.etrisad.zenith.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import java.util.Calendar

object ScreenUsageHelper {
    /**
     * Fetches accurate app usage for today by processing UsageEvents.
     * Handles screen off events to prevent "hanging" sessions from inflating stats.
     * Returns a map of package names to usage time in MILLISECONDS.
     */
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
        val lastEventTime = mutableMapOf<String, Long>()
        
        // Use a 24-hour buffer to catch sessions starting before midnight
        val events = usageStatsManager.queryEvents(start - (24 * 60 * 60 * 1000L), end)
        val event = UsageEvents.Event()
        
        var isScreenOn = true 

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            val time = event.timeStamp
            
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    isScreenOn = true
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    isScreenOn = false
                    // When screen goes off, end all active sessions
                    lastEventTime.forEach { (p, startTime) ->
                        val segmentStart = maxOf(startTime, start)
                        val segmentEnd = minOf(time, end)
                        if (segmentStart < segmentEnd) {
                            usageMap[p] = (usageMap[p] ?: 0L) + (segmentEnd - segmentStart)
                        }
                    }
                    lastEventTime.clear()
                }
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (isScreenOn) {
                        // Usually only one app can be resumed, but we track per package just in case
                        lastEventTime[pkg] = time
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    lastEventTime.remove(pkg)?.let { startTime ->
                        val segmentStart = maxOf(startTime, start)
                        val segmentEnd = minOf(time, end)
                        if (segmentStart < segmentEnd) {
                            usageMap[pkg] = (usageMap[pkg] ?: 0L) + (segmentEnd - segmentStart)
                        }
                    }
                }
            }
        }
        
        // Handle app currently in foreground if screen is still on
        if (isScreenOn) {
            lastEventTime.forEach { (pkg, startTime) ->
                val segmentStart = maxOf(startTime, start)
                val segmentEnd = end
                if (segmentStart < segmentEnd) {
                    usageMap[pkg] = (usageMap[pkg] ?: 0L) + (segmentEnd - segmentStart)
                }
            }
        }

        return usageMap
    }
}
