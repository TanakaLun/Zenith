package com.etrisad.zenith.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import java.util.Calendar

object ScreenUsageHelper {
    /**
     * Fetches accurate app usage for today by processing UsageEvents.
     * Filters out transient background events and ensures no overlapping sessions.
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
        
        // We only track ONE active app at a time to prevent notification-related "fake" overlaps
        var activePkg: String? = null
        var activeStartTime: Long = 0L
        
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
                    // Terminate current session immediately when screen goes off
                    if (activePkg != null) {
                        val segmentStart = maxOf(activeStartTime, start)
                        val segmentEnd = minOf(time, end)
                        if (segmentStart < segmentEnd) {
                            val duration = segmentEnd - segmentStart
                            if (duration > 1000) { // Ignore segments < 1s
                                usageMap[activePkg!!] = (usageMap[activePkg!!] ?: 0L) + duration
                            }
                        }
                        activePkg = null
                        activeStartTime = 0L
                    }
                }
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (isScreenOn) {
                        // Class name check to filter out known system transients/notifications
                        val className = event.className ?: ""
                        if (className.contains("Notification", ignoreCase = true) || 
                            className.contains("Toast", ignoreCase = true) ||
                            className.contains("ResolverActivity", ignoreCase = true)) {
                            continue
                        }

                        // If a different app takes focus, end the previous one's session
                        if (activePkg != null && activePkg != pkg) {
                            val segmentStart = maxOf(activeStartTime, start)
                            val segmentEnd = minOf(time, end)
                            if (segmentStart < segmentEnd) {
                                val duration = segmentEnd - segmentStart
                                if (duration > 1000) {
                                    usageMap[activePkg!!] = (usageMap[activePkg!!] ?: 0L) + duration
                                }
                            }
                        }
                        
                        activePkg = pkg
                        activeStartTime = time
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (activePkg == pkg) {
                        val segmentStart = maxOf(activeStartTime, start)
                        val segmentEnd = minOf(time, end)
                        if (segmentStart < segmentEnd) {
                            val duration = segmentEnd - segmentStart
                            if (duration > 1000) {
                                usageMap[activePkg!!] = (usageMap[activePkg!!] ?: 0L) + duration
                            }
                        }
                        activePkg = null
                        activeStartTime = 0L
                    }
                }
            }
        }
        
        // Handle app currently in foreground
        if (isScreenOn && activePkg != null) {
            val segmentStart = maxOf(activeStartTime, start)
            val segmentEnd = end
            if (segmentStart < segmentEnd) {
                val duration = segmentEnd - segmentStart
                if (duration > 1000) {
                    usageMap[activePkg!!] = (usageMap[activePkg!!] ?: 0L) + duration
                }
            }
        }

        return usageMap
    }
}
