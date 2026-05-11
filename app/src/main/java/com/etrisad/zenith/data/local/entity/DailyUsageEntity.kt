package com.etrisad.zenith.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_usage",
    indices = [Index(value = ["date", "packageName"], unique = true)]
)
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val packageName: String,
    val usageTimeMillis: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
