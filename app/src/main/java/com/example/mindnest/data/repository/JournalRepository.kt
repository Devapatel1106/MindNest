package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.JournalDao
import com.example.mindnest.data.entity.JournalEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getJournalEntriesByUser(userId: Long): Flow<List<JournalEntity>> {
        return journalDao.getJournalEntriesByUser(userId)
    }

    fun getJournalEntryByDate(userId: Long, date: String): Flow<JournalEntity?> {
        return journalDao.getJournalEntryByDate(userId, date)
    }

    suspend fun insertJournalEntry(entry: JournalEntity): Long {

        // ✅ Save in Room
        val id = journalDao.insertJournalEntry(entry)

        // ✅ Save in Firebase
        val uid = auth.currentUser?.uid ?: return id

        val journalMap = hashMapOf(
            "localId" to id,
            "userId" to entry.userId,
            "content" to entry.content,
            "mood" to entry.mood,
            "date" to entry.date,
            "createdAt" to entry.createdAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("journals")
            .add(journalMap)

        return id
    }

    suspend fun updateJournalEntry(entry: JournalEntity) {

        // ✅ Update Room
        journalDao.updateJournalEntry(entry)

        // ✅ Update Firebase
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("journals")
            .whereEqualTo("localId", entry.id)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    doc.reference.update(
                        mapOf(
                            "content" to entry.content,
                            "mood" to entry.mood,
                            "date" to entry.date
                        )
                    )
                }
            }
    }

    suspend fun deleteJournalEntry(entry: JournalEntity) {

        journalDao.deleteJournalEntry(entry)

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("journals")
            .whereEqualTo("localId", entry.id)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    doc.reference.delete()
                }
            }
    }
}
