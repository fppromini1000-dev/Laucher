package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_habits",
    indices = [
        Index(value = ["packageName", "hourOfDay", "dayOfWeek"], unique = true)
    ]
)
data class UsageHabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val launchCount: Int = 1,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)
