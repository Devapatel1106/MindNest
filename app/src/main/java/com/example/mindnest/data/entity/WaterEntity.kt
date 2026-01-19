package com.example.mindnest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_entries")
data class WaterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val amountMl: Int,
    val date: String, // Format: "dd/MM/yy"
    val createdAt: Long = System.currentTimeMillis()
)
