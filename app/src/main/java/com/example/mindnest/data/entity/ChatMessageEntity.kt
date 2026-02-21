package com.example.mindnest.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Long,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
