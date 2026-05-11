package com.etrisad.zenith.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.etrisad.zenith.data.local.entity.HourlyUsageEntity
import com.etrisad.zenith.data.preferences.UserPreferencesRepository
import com.etrisad.zenith.data.repository.ShieldRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class UsageSyncManager(
    private val context: Context,
    private val repository: ShieldRepository,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun syncUsageData() {
        val lastSyncTime = preferencesRepository.userPreferencesFlow.first().lastSyncTimestamp
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastSyncTime < 60000) return 

        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launcherPackage = pm.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName

        val launcherApps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.activityInfo.packageName }.toSet()

        val excludePackages = setOfNotNull(context.packageName, launcherPackage)

        val events = usageStatsManager.queryEvents(lastSyncTime, currentTime)
        val event = UsageEvents.Event()
        
        val activeSessions = mutableMapOf<String, Long>()
        val hourlyBuckets = mutableMapOf<String, MutableMap<Int, MutableMap<String, Long>>>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            val time = event.timeStamp

            if (pkg in excludePackages || pkg !in launcherApps) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND, UsageEvents.Event.ACTIVITY_RESUMED -> {
                    activeSessions[pkg] = time
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND, UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val startTime = activeSessions.remove(pkg) ?: continue
                    processSession(pkg, startTime, time, hourlyBuckets)
                }
            }
        }

        activeSessions.forEach { (pkg, startTime) ->
            processSession(pkg, startTime, currentTime, hourlyBuckets)
        }

        saveBucketsToDatabase(hourlyBuckets)
        preferencesRepository.setLastSyncTimestamp(currentTime)
    }

    private fun processSession(
        pkg: String,
        start: Long,
        end: Long,
        buckets: MutableMap<String, MutableMap<Int, MutableMap<String, Long>>>
    ) {
        val cal = Calendar.getInstance()
        var current = start

        while (current < end) {
            cal.timeInMillis = current
            val dateStr = dateFormat.format(cal.time)
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            
            val nextHourStart = (cal.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val chunkEnd = minOf(end, nextHourStart)
            val duration = chunkEnd - current

            if (duration > 0) {
                buckets.getOrPut(dateStr) { mutableMapOf() }
                    .getOrPut(hour) { mutableMapOf() }
                    .let { it[pkg] = (it[pkg] ?: 0L) + duration }
            }
            current = chunkEnd
        }
    }

    private suspend fun saveBucketsToDatabase(
        buckets: MutableMap<String, MutableMap<Int, MutableMap<String, Long>>>
    ) {
        val entities = mutableListOf<HourlyUsageEntity>()
        val now = System.currentTimeMillis()

        buckets.forEach { (date, hours) ->
            val existingRecords = repository.getHourlyUsageForDate(date).first()
            
            hours.forEach { (hour, apps) ->
                var hourTotal = 0L
                apps.forEach { (pkg, duration) ->
                    val existing = existingRecords.find { it.hour == hour && it.packageName == pkg }
                    val totalDuration = (existing?.usageTimeMillis ?: 0L) + duration
                    
                    hourTotal += duration

                    entities.add(
                        HourlyUsageEntity(
                            id = existing?.id ?: 0,
                            date = date,
                            hour = hour,
                            packageName = pkg,
                            usageTimeMillis = totalDuration,
                            lastUpdated = now
                        )
                    )
                }
                
                if (hourTotal > 0) {
                    val existingTotal = existingRecords.find { it.hour == hour && it.packageName == "TOTAL" }
                    val newTotal = (existingTotal?.usageTimeMillis ?: 0L) + hourTotal
                    entities.add(
                        HourlyUsageEntity(
                            id = existingTotal?.id ?: 0,
                            date = date,
                            hour = hour,
                            packageName = "TOTAL",
                            usageTimeMillis = newTotal,
                            lastUpdated = now
                        )
                    )
                }
            }
        }
        
        if (entities.isNotEmpty()) {
            repository.insertHourlyUsage(entities)
        }
    }
}
