package com.example.mindnest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_tracking")
data class PeriodEntity(

    @PrimaryKey
    val userId: Long,
    val cycleLength: Int = 28,
    val startDate: String? = null,
    val endDate: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)