package com.example.mindnest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val userId: Long,
    val waterTargetMl: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
