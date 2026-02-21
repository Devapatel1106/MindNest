package com.example.mindnest.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.mindnest.data.entity.ChatMessageEntity

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun getMessages(userId: Long): LiveData<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearChat(userId: Long)

    @Query("DELETE FROM chat_messages WHERE userId = :userId AND timestamp < :olderThan")
    suspend fun deleteMessagesOlderThan(userId: Long, olderThan: Long)
}