package com.etrisad.zenith.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.etrisad.zenith.data.local.database.ZenithDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupUtils {
    private const val DATABASE_NAME = "zenith_database"
    private const val PREFS_FILE_NAME = "settings.preferences_pb"

    data class BackupMetadata(
        val hasDatabase: Boolean,
        val hasPreferences: Boolean,
        val fileSize: Long,
        val latestUsageDate: String? = null,
        val latestUsageMillis: Long? = null,
        val hasHourly: Boolean = false,
        val hasPiechart: Boolean = false,
        val last7DaysSnapshot: List<Pair<String, Long>> = emptyList(),
        val last7DaysTopApps: List<Triple<String, String, Long>> = emptyList(),
        val last7DaysDbStatus: List<Pair<String, Boolean>> = emptyList(),
        val latestHourlyData: Map<Int, Long> = emptyMap(),
        val latestPiechartData: Map<String, Long> = emptyMap(),
        val latestShieldUsage: Long = 0L,
        val latestGoalUsage: Long = 0L,
        val latestOtherUsage: Long = 0L
    )

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    suspend fun backupDatabase(context: Context, targetUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val prefsFile = File(context.filesDir, "datastore/$PREFS_FILE_NAME")

            ZenithDatabase.closeDatabase()

            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    if (dbFile.exists()) {
                        addToZip(zipOut, dbFile, DATABASE_NAME)
                    }
                    if (prefsFile.exists()) {
                        addToZip(zipOut, prefsFile, PREFS_FILE_NAME)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreDatabase(context: Context, sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ZenithDatabase.closeDatabase()

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        when (entry.name) {
                            DATABASE_NAME -> {
                                val dbFile = context.getDatabasePath(DATABASE_NAME)
                                FileOutputStream(dbFile).use { zipIn.copyTo(it) }
                            }
                            PREFS_FILE_NAME -> {
                                val prefsFile = File(context.filesDir, "datastore/$PREFS_FILE_NAME")
                                prefsFile.parentFile?.mkdirs()
                                FileOutputStream(prefsFile).use { zipIn.copyTo(it) }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            val dbPath = context.getDatabasePath(DATABASE_NAME).path
            File("$dbPath-wal").delete()
            File("$dbPath-shm").delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBackupMetadata(context: Context, uri: Uri): BackupMetadata? = withContext(Dispatchers.IO) {
        try {
            var hasDb = false
            var hasPrefs = false
            val fileSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    try {
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            val entryName = entry.name.lowercase()
                            if (entryName.contains(DATABASE_NAME.lowercase())) hasDb = true
                            if (entryName.contains(PREFS_FILE_NAME.lowercase())) hasPrefs = true

                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    } catch (e: Exception) {
                    }
                }
            }

            if (!hasDb && !hasPrefs) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val header = ByteArray(16)
                    if (input.read(header) >= 16) {
                        val headerString = String(header)
                        if (headerString.contains("SQLite format 3")) {
                            hasDb = true
                        }
                    }
                }
            }

            if (hasDb || hasPrefs) {
                var latestDate: String? = null
                var latestMillis: Long? = null
                var hasHourly = false
                var hasPiechart = false
                var last7DaysSnapshot = listOf<Pair<String, Long>>()
                var last7DaysTopApps = listOf<Triple<String, String, Long>>()
                var latestHourlyData = mapOf<Int, Long>()
                var latestPiechartData = mapOf<String, Long>()
                var shieldU = 0L
                var goalU = 0L
                var otherU = 0L

                if (hasDb) {
                    try {
                        val tempDbFile = File(context.cacheDir, "temp_restore.db")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            ZipInputStream(input).use { zipIn ->
                                var entry = zipIn.nextEntry
                                while (entry != null) {
                                    if (entry.name.lowercase().contains(DATABASE_NAME.lowercase())) {
                                        FileOutputStream(tempDbFile).use { zipIn.copyTo(it) }
                                        break
                                    }
                                    zipIn.closeEntry()
                                    entry = zipIn.nextEntry
                                }
                            }

                            if (!tempDbFile.exists() || tempDbFile.length() == 0L) {
                                context.contentResolver.openInputStream(uri)?.use { rawInput ->
                                    FileOutputStream(tempDbFile).use { rawInput.copyTo(it) }
                                }
                            }
                        }

                        if (tempDbFile.exists() && tempDbFile.length() > 0) {
                            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                                tempDbFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                            )
                            
                            db.rawQuery(
                                "SELECT date, usageTimeMillis FROM daily_usage WHERE packageName = 'TOTAL' ORDER BY date DESC LIMIT 1",
                                null
                            ).use { cursor ->
                                if (cursor.moveToFirst()) {
                                    latestDate = cursor.getString(0)
                                    latestMillis = cursor.getLong(1)
                                }
                            }

                            try {
                                db.rawQuery("SELECT 1 FROM hourly_usage LIMIT 1", null).use { 
                                    hasHourly = it.moveToFirst() 
                                }
                            } catch (_: Exception) {}

                            try {
                                db.rawQuery(
                                    "SELECT date, usageTimeMillis FROM daily_usage WHERE packageName = 'TOTAL' ORDER BY date DESC LIMIT 7",
                                    null
                                ).use { cursor ->
                                    val snapshot = mutableListOf<Pair<String, Long>>()
                                    while (cursor.moveToNext()) {
                                        snapshot.add(cursor.getString(0) to cursor.getLong(1))
                                    }
                                    last7DaysSnapshot = snapshot
                                }
                            } catch (_: Exception) {}

                            try {
                                db.rawQuery(
                                    "SELECT date, packageName, usageTimeMillis FROM daily_usage WHERE packageName != 'TOTAL' AND packageName NOT LIKE '%_TOTAL' ORDER BY date DESC, usageTimeMillis DESC",
                                    null
                                ).use { cursor ->
                                    val topAppsMap = mutableMapOf<String, Pair<String, Long>>()
                                    while (cursor.moveToNext()) {
                                        val date = cursor.getString(0)
                                        if (!topAppsMap.containsKey(date)) {
                                            topAppsMap[date] = cursor.getString(1) to cursor.getLong(2)
                                        }
                                    }
                                    last7DaysTopApps = topAppsMap.map { Triple(it.key, it.value.first, it.value.second) }
                                        .sortedByDescending { it.first }
                                        .take(7)
                                }
                            } catch (_: Exception) {}

                            if (hasHourly) {
                                try {
                                    db.rawQuery(
                                        "SELECT hour, SUM(usageTimeMillis) FROM hourly_usage WHERE date = (SELECT MAX(date) FROM daily_usage) GROUP BY hour",
                                        null
                                    ).use { cursor ->
                                        val hourly = mutableMapOf<Int, Long>()
                                        while (cursor.moveToNext()) {
                                            hourly[cursor.getInt(0)] = cursor.getLong(1)
                                        }
                                        latestHourlyData = hourly
                                    }
                                } catch (_: Exception) {}
                            }

                            try {
                                db.rawQuery(
                                    "SELECT packageName, usageTimeMillis FROM daily_usage WHERE date = (SELECT MAX(date) FROM daily_usage) AND packageName != 'TOTAL' AND packageName NOT LIKE '%_TOTAL' ORDER BY usageTimeMillis DESC LIMIT 5",
                                    null
                                ).use { cursor ->
                                    val pie = mutableMapOf<String, Long>()
                                    while (cursor.moveToNext()) {
                                        pie[cursor.getString(0)] = cursor.getLong(1)
                                    }
                                    latestPiechartData = pie
                                    hasPiechart = pie.isNotEmpty()
                                }
                            } catch (_: Exception) {}

                            try {
                                db.rawQuery(
                                    "SELECT packageName, usageTimeMillis FROM daily_usage WHERE date = (SELECT MAX(date) FROM daily_usage) AND packageName LIKE '%_TOTAL'",
                                    null
                                ).use { cursor ->
                                    while (cursor.moveToNext()) {
                                        val pkg = cursor.getString(0)
                                        val valU = cursor.getLong(1)
                                        when (pkg) {
                                            "SHIELD_TOTAL" -> shieldU = valU
                                            "GOAL_TOTAL" -> goalU = valU
                                            "OTHER_TOTAL" -> otherU = valU
                                        }
                                    }
                                }
                            } catch (_: Exception) {}

                            db.close()
                        }
                        tempDbFile.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                BackupMetadata(
                    hasDatabase = hasDb,
                    hasPreferences = hasPrefs,
                    fileSize = fileSize,
                    latestUsageDate = latestDate,
                    latestUsageMillis = latestMillis,
                    hasHourly = hasHourly,
                    hasPiechart = hasPiechart,
                    last7DaysSnapshot = last7DaysSnapshot,
                    last7DaysTopApps = last7DaysTopApps,
                    latestHourlyData = latestHourlyData,
                    latestPiechartData = latestPiechartData,
                    latestShieldUsage = shieldU,
                    latestGoalUsage = goalU,
                    latestOtherUsage = otherU
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun addToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { input ->
            zipOut.putNextEntry(ZipEntry(entryName))
            input.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
}
