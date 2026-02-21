package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.SleepDao
import com.example.mindnest.data.entity.SleepEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

class SleepRepository(private val sleepDao: SleepDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getSleepLogsByUser(userId: Long): Flow<List<SleepEntity>> {
        return sleepDao.getSleepLogsByUser(userId)
    }

    suspend fun insertSleepLog(sleep: SleepEntity): Long {

        val id = sleepDao.insertSleepLog(sleep)

        val uid = auth.currentUser?.uid ?: return id

        val sleepMap = hashMapOf(
            "localId" to id,
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
            .add(sleepMap)

        return id
    }

    suspend fun deleteSleepLog(sleep: SleepEntity) {

        sleepDao.deleteSleepLog(sleep)

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("sleep_logs")
            .whereEqualTo("localId", sleep.id)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    doc.reference.delete()
                }
            }
    }

    suspend fun deleteSleepLogById(sleepId: Long) {


        sleepDao.deleteSleepLogById(sleepId)


        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("sleep_logs")
            .whereEqualTo("localId", sleepId)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    doc.reference.delete()
                }
            }
    }
}