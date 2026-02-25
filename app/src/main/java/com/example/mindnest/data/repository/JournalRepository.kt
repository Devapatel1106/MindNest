package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.JournalDao
import com.example.mindnest.data.entity.JournalEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class JournalRepository(private val journalDao: JournalDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getJournalEntriesByUser(userId: Long) =
        journalDao.getJournalEntriesByUser(userId)

    fun getJournalEntryByDate(userId: Long, date: String) =
        journalDao.getJournalEntryByDate(userId, date)

    suspend fun insertJournalEntry(entry: JournalEntity): Long {

        val id = journalDao.insertJournalEntry(entry)

        val uid = auth.currentUser?.uid ?: return id

        val map = hashMapOf(
            "id" to id,
            "userId" to entry.userId,
            "content" to entry.content,
            "mood" to entry.mood,
            "date" to entry.date,
            "createdAt" to entry.createdAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("journals")
            .document(id.toString())
            .set(map)
            .await()

        return id
    }


    suspend fun updateJournalEntry(entry: JournalEntity) {

        journalDao.updateJournalEntry(entry)

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("journals")
            .document(entry.id.toString())
            .set(entry)
            .await()
    }

    suspend fun deleteJournalEntry(entry: JournalEntity) {

        journalDao.deleteJournalEntry(entry)

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("journals")
            .document(entry.id.toString())
            .delete()
            .await()
    }

    fun startRealtimeSync(userId: Long) {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("journals")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot == null) return@addSnapshotListener

                CoroutineScope(Dispatchers.IO).launch {

                    for (doc in snapshot.documents) {

                        val entry = JournalEntity(
                            id = doc.getLong("id") ?: 0,
                            userId = userId,
                            content = doc.getString("content") ?: "",
                            mood = doc.getString("mood") ?: "",
                            date = doc.getString("date") ?: "",
                            createdAt = doc.getLong("createdAt")
                                ?: System.currentTimeMillis()
                        )

                        journalDao.insertJournalEntry(entry)
                    }
                }
            }
    }
}