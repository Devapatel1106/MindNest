package com.example.mindnest.data.repository

import androidx.lifecycle.LiveData
import com.example.mindnest.data.dao.ChatDao
import com.example.mindnest.data.entity.ChatMessageEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChatRepository(private val chatDao: ChatDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getMessages(userId: Long): LiveData<List<ChatMessageEntity>> {
        return chatDao.getMessages(userId)
    }

    suspend fun insert(message: ChatMessageEntity) {

        chatDao.insertMessage(message)

        val uid = auth.currentUser?.uid ?: return

        val map = hashMapOf(
            "id" to message.id,
            "userId" to message.userId,
            "message" to message.message,
            "isUser" to message.isUser,
            "timestamp" to message.timestamp
        )

        firestore.collection("users")
            .document(uid)
            .collection("chat")
            .document(message.timestamp.toString())
            .set(map)
    }

    suspend fun deleteMessagesOlderThan(userId: Long, olderThan: Long) {

        chatDao.deleteMessagesOlderThan(userId, olderThan)

        val uid = auth.currentUser?.uid ?: return

        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("chat")
            .whereLessThan("timestamp", olderThan)
            .get()
            .await()

        for (doc in snapshot.documents) {
            doc.reference.delete()
        }
    }

    suspend fun syncChatFromFirebase(userId: Long) {
        chatDao.clearChat(userId)
        val uid = auth.currentUser?.uid ?: return

        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("chat")
            .get()
            .await()

        for (doc in snapshot.documents) {

            val msg = ChatMessageEntity(
                id = 0,
                userId = userId,
                message = doc.getString("message") ?: "",
                isUser = doc.getBoolean("isUser") ?: false,
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            )

            chatDao.insertMessage(msg)
        }
    }
}