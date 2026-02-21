package com.example.mindnest.ui.chat

import android.app.Application
import androidx.lifecycle.*
import com.example.mindnest.MindNestApplication
import com.example.mindnest.data.entity.ChatMessageEntity
import com.example.mindnest.data.repository.ChatRepository
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager(application)
    private val userId = prefs.getUserId()

    private val repository: ChatRepository

    val messages: LiveData<List<ChatMessageEntity>>

    init {
        val dao = (application as MindNestApplication).database.chatDao()
        repository = ChatRepository(dao)
        messages = repository.getMessages(userId)

        viewModelScope.launch {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            repository.deleteMessagesOlderThan(userId, sevenDaysAgo)
        }
    }

    fun sendMessage(text: String, isUser: Boolean) {
        viewModelScope.launch {

            repository.insert(
                ChatMessageEntity(
                    userId = userId,
                    message = text,
                    isUser = isUser
                )
            )
        }
    }
}

