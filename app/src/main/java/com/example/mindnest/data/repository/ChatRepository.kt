package com.example.mindnest.data.repository

import androidx.lifecycle.LiveData
import com.example.mindnest.data.dao.ChatDao
import com.example.mindnest.data.entity.ChatMessageEntity

class ChatRepository(private val chatDao: ChatDao) {

    fun getMessages(userId: Long): LiveData<List<ChatMessageEntity>> {
        return chatDao.getMessages(userId)
    }

    suspend fun insert(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteMessagesOlderThan(userId: Long, olderThan: Long) {
        chatDao.deleteMessagesOlderThan(userId, olderThan)
    }
}