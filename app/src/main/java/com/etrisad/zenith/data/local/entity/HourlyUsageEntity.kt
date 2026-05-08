package com.etrisad.zenith.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hourly_usage",
    indices = [Index(value = ["date", "hour", "packageName"], unique = true)]
)
data class HourlyUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: YYYY-MM-DD
    val hour: Int, // 0-23
    val packageName: String, // Use "TOTAL" for global hourly usage
    val usageTimeMillis: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
