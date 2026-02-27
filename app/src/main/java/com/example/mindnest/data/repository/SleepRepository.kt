package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.SleepDao
import com.example.mindnest.data.entity.SleepEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SleepRepository(private val sleepDao: SleepDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getSleepLogsByUser(userId: Long) =
        sleepDao.getSleepLogsByUser(userId)

    suspend fun insertSleepLog(sleep: SleepEntity): Long {
        val id = sleepDao.insertSleepLog(sleep)
        val uid = auth.currentUser?.uid ?: return id

        val sleepMap = hashMapOf(
            "id" to id,
            "userId" to sleep.userId,
            "startHour" to sleep.startHour,
            "startMinute" to sleep.startMinute,
            "endHour" to sleep.endHour,
            "endMinute" to sleep.endMinute,
            "date" to sleep.date,
            "createdAt" to sleep.createdAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("sleep_logs")
            .document(id.toString())
            .set(sleepMap)
            .await()

        return id
    }

    suspend fun deleteSleepLog(sleep: SleepEntity) {
        sleepDao.deleteSleepLog(sleep)
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("sleep_logs")
            .document(sleep.id.toString())
            .delete()
            .await()
    }

    suspend fun deleteSleepLogById(sleepId: Long) {
        sleepDao.deleteSleepLogById(sleepId)
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("sleep_logs")
            .document(sleepId.toString())
            .delete()
            .await()
    }

    fun startRealtimeSync(userId: Long) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("sleep_logs")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                CoroutineScope(Dispatchers.IO).launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val id = doc.getLong("id") ?: 0L
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val sleep = SleepEntity(
                                    id = id,
                                    userId = userId,
                                    startHour = (doc.getLong("startHour") ?: 0).toInt(),
                                    startMinute = (doc.getLong("startMinute") ?: 0).toInt(),
                                    endHour = (doc.getLong("endHour") ?: 0).toInt(),
                                    endMinute = (doc.getLong("endMinute") ?: 0).toInt(),
                                    date = doc.getString("date") ?: "",
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                )
                                sleepDao.insertSleepLog(sleep)
                            }
                            DocumentChange.Type.REMOVED -> {
                                sleepDao.deleteSleepLogById(id)
                            }
                        }
                    }
                }
            }
    }
}