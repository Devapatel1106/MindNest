package com.example.mindnest.ui.period.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_entries")
data class PeriodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startDate: Long,       // millis
    val periodLength: Int      // days
)
