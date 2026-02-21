package com.example.mindnest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "mind_score",
    primaryKeys = ["userId", "date"]
)
data class MindScoreEntity(
    val userId: Long,
    val date: String,
    val score: Int
)