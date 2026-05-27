package com.etrisad.zenith.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.util.Log
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ScreenUsageHelper {
    data class UsageResult(
        val appUsageMap: Map<String, Long>,
        val hourlyUsageMap: Map<Int, Map<String, Long>>,
        val sessionCounts: Map<String, Int>,
        val lastUsedMap: Map<String, Long> = emptyMap()
    )

    private var lastResult: UsageResult? = null
    private var lastQueryTime = 0L
    private const val CACHE_DURATION = 15000L
    
    private val persistentUsageMap = mutableMapOf<String, Long>()
    private val persistentSessionCounts = mutableMapOf<String, Int>()
    private var lastEventProcessedTime = 0L
    private var activePkg: String? = null
    private var activeStartTime = 0L
    private var isScreenOnState = true
    private var lastProcessedDate: LocalDate? = null

    private const val MIDNIGHT_LOOKBACK_MS = 1800000L
    private const val SESSION_MIN_DURATION = 4000L
    private const val MIN_SEGMENT_DURATION = 100L
    private const val SESSION_TIMEOUT_MS = 60000L

    @Synchronized
    fun fetchDetailedUsageToday(
        usageStatsManager: UsageStatsManager,
        includeHourly: Boolean = false
    ): UsageResult {
        val currentTime = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)

        if (lastResult != null && currentTime - lastQueryTime < CACHE_DURATION &&
            (!includeHourly || lastResult!!.hourlyUsageMap.isNotEmpty())) {
            return lastResult!!
        }

        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        if (lastProcessedDate != today) {
            persistentUsageMap.clear()
            persistentSessionCounts.clear()
            lastEventProcessedTime = startOfDay - MIDNIGHT_LOOKBACK_MS
            activePkg = null
            activeStartTime = 0L
            isScreenOnState = true
            lastProcessedDate = today
        }

        val queryStart = if (lastEventProcessedTime > startOfDay) lastEventProcessedTime - 1000L else lastEventProcessedTime
        val events = try {
            usageStatsManager.queryEvents(queryStart.coerceAtLeast(startOfDay - MIDNIGHT_LOOKBACK_MS), currentTime)
        } catch (e: Exception) {
            null
        }

        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName
                val time = event.timeStamp
                
                if (time <= lastEventProcessedTime && lastEventProcessedTime > startOfDay) continue

                when (event.eventType) {
                    UsageEvents.Event.SCREEN_INTERACTIVE -> isScreenOnState = true
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        isScreenOnState = false
                        activePkg?.let { p ->
                            val segmentStart = maxOf(activeStartTime, startOfDay)
                            if (segmentStart < time) {
                                val duration = time - segmentStart
                                if (duration > MIN_SEGMENT_DURATION) {
                                    persistentUsageMap[p] = (persistentUsageMap[p] ?: 0L) + duration
                                    if (duration > SESSION_MIN_DURATION) {
                                        persistentSessionCounts[p] = (persistentSessionCounts[p] ?: 0) + 1
                                    }
                                }
                            }
                        }
                        activePkg = null
                        activeStartTime = 0L
                    }
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (isScreenOnState) {
                            val className = event.className
                            if (className != null && (className.contains("Notification") ||
                                className.contains("Toast") || 
                                className.contains("toast"))) {
                                continue
                            }

                            if (activePkg != null && activePkg != pkg) {
                                val segmentStart = maxOf(activeStartTime, startOfDay)
                                if (segmentStart < time) {
                                    val duration = time - segmentStart
                                    if (duration > 0) {
                                        persistentUsageMap[activePkg!!] = (persistentUsageMap[activePkg!!] ?: 0L) + duration
                                        if (duration > SESSION_MIN_DURATION) {
                                            persistentSessionCounts[activePkg!!] = (persistentSessionCounts[activePkg!!] ?: 0) + 1
                                        }
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
                            val segmentStart = maxOf(activeStartTime, startOfDay)
                            if (segmentStart < time) {
                                val duration = time - segmentStart
                                if (duration > MIN_SEGMENT_DURATION) {
                                    persistentUsageMap[pkg] = (persistentUsageMap[pkg] ?: 0L) + duration
                                    if (duration > SESSION_MIN_DURATION) {
                                        persistentSessionCounts[pkg] = (persistentSessionCounts[pkg] ?: 0) + 1
                                    }
                                }
                            }
                            activePkg = null
                            activeStartTime = 0L
                        }
                    }
                }
                lastEventProcessedTime = time
            }
        }

        val finalUsageMap = persistentUsageMap.toMutableMap()
        val finalSessionCounts = persistentSessionCounts.toMutableMap()
        
        if (isScreenOnState && activePkg != null) {
            val segmentStart = maxOf(activeStartTime, startOfDay)
            if (segmentStart < currentTime) {
                val duration = currentTime - segmentStart
                finalUsageMap[activePkg!!] = (finalUsageMap[activePkg!!] ?: 0L) + duration
                if (duration > SESSION_MIN_DURATION) {
                    finalSessionCounts[activePkg!!] = (finalSessionCounts[activePkg!!] ?: 0) + 1
                }
            }
        }

        val lastUsedMap = mutableMapOf<String, Long>()
        try {
            val aggregatedStats = usageStatsManager.queryAndAggregateUsageStats(startOfDay, currentTime)
            aggregatedStats?.forEach { (pkg, stats) ->
                lastUsedMap[pkg] = stats.lastTimeUsed
                val aggUsage = stats.totalTimeInForeground.coerceAtLeast(stats.totalTimeVisible)
                if (aggUsage > (finalUsageMap[pkg] ?: 0L)) {
                    finalUsageMap[pkg] = aggUsage
                }
            }
        } catch (e: Exception) {}

        val hourlyMap = if (includeHourly) {
            calculateHourlyUsage(usageStatsManager, startOfDay, currentTime, zoneId)
        } else {
            emptyMap()
        }

        val result = UsageResult(finalUsageMap, hourlyMap, finalSessionCounts, lastUsedMap)
        lastResult = result
        lastQueryTime = currentTime
        return result
    }

    private fun calculateHourlyUsage(
        usageStatsManager: UsageStatsManager,
        start: Long,
        end: Long,
        zoneId: ZoneId
    ): Map<Int, Map<String, Long>> {
        val hourlyMap = mutableMapOf<Int, MutableMap<String, Long>>()
        val events = try {
            usageStatsManager.queryEvents(start - MIDNIGHT_LOOKBACK_MS, end)
        } catch (e: Exception) { null } ?: return emptyMap()
        
        val event = UsageEvents.Event()
        var activePkg: String? = null
        var activeStartTime = 0L
        var isScreenOn = true

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            val time = event.timeStamp
            
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> isScreenOn = true
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    isScreenOn = false
                    activePkg?.let { p ->
                        addHourlySegments(hourlyMap, p, maxOf(activeStartTime, start), minOf(time, end), zoneId)
                    }
                    activePkg = null
                }
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (isScreenOn) {
                        activePkg?.let { p ->
                            addHourlySegments(hourlyMap, p, maxOf(activeStartTime, start), minOf(time, end), zoneId)
                        }
                        activePkg = pkg
                        activeStartTime = time
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (activePkg == pkg) {
                        addHourlySegments(hourlyMap, pkg, maxOf(activeStartTime, start), minOf(time, end), zoneId)
                        activePkg = null
                    }
                }
            }
        }
        
        if (isScreenOn && activePkg != null) {
            addHourlySegments(hourlyMap, activePkg!!, maxOf(activeStartTime, start), end, zoneId)
        }

        return hourlyMap
    }

    private fun addHourlySegments(
        hourlyMap: MutableMap<Int, MutableMap<String, Long>>,
        packageName: String,
        start: Long,
        end: Long,
        zoneId: ZoneId
    ) {
        if (start >= end) return
        
        var currentMillis = start
        while (currentMillis < end) {
            val currentZdt = Instant.ofEpochMilli(currentMillis).atZone(zoneId)
            val hour = currentZdt.hour
            val nextHourMillis = currentZdt.plusHours(1).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()
            
            val segmentEnd = minOf(nextHourMillis, end)
            val duration = segmentEnd - currentMillis
            
            if (duration > 0) {
                val pkgMap = hourlyMap.getOrPut(hour) { mutableMapOf() }
                pkgMap[packageName] = (pkgMap[packageName] ?: 0L) + duration
            }
            currentMillis = segmentEnd
        }
    }

    fun fetchAppUsageTodayTillNow(usageStatsManager: UsageStatsManager): Map<String, Long> {
        return fetchDetailedUsageToday(usageStatsManager).appUsageMap
    }

    @Synchronized
    fun clearCache() {
        lastResult = null
        lastQueryTime = 0L
        persistentUsageMap.clear()
        persistentSessionCounts.clear()
        lastEventProcessedTime = 0L
        activePkg = null
        activeStartTime = 0L
        lastProcessedDate = null
    }
}
