package com.example.mindnest.ui.period.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val mood: String,
    val energy: Int,
    val stress: Int
)
