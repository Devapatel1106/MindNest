package com.example.mindnest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_info")
data class UserInfoEntity(
    @PrimaryKey
    val userId: String,
    val weight: Int,
    val height: Int,
    val age: Int,
    val gender: String,
    val targetCalories: Int
)
