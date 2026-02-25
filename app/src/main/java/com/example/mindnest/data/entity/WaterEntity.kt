package com.example.mindnest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_entries")
data class WaterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val amountMl: Int = 0,
    val date: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(0, 0, 0, "", System.currentTimeMillis())
}