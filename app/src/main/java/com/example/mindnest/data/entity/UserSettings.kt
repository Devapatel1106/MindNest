package com.example.mindnest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val userId: Long = 0,
    val waterTargetMl: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this(0, 0, System.currentTimeMillis())
}