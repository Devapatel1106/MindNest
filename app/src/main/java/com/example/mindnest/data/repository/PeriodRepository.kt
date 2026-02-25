package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.PeriodDao
import com.example.mindnest.data.entity.PeriodEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PeriodRepository(private val periodDao: PeriodDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getPeriodTracking(userId: Long) =
        periodDao.getPeriodTrackingByUser(userId)

    suspend fun savePeriod(period: PeriodEntity) {

        periodDao.insertPeriodTracking(period)

        val uid = auth.currentUser?.uid ?: return

        val periodMap = hashMapOf(
            "userId" to period.userId,
            "cycleLength" to period.cycleLength,
            "startDate" to period.startDate,
            "endDate" to period.endDate,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(uid)
            .collection("period_tracking")
            .document("data")
            .set(periodMap)
    }

    suspend fun syncPeriodFromFirebase(userId: Long) {

        val uid = auth.currentUser?.uid ?: return

        val doc = firestore.collection("users")
            .document(uid)
            .collection("period_tracking")
            .document("data")
            .get()
            .await()

        if (!doc.exists()) return

        val period = PeriodEntity(
            userId = userId,
            cycleLength = (doc.getLong("cycleLength") ?: 28).toInt(),
            startDate = doc.getString("startDate"),
            endDate = doc.getString("endDate"),
            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
        )

        periodDao.insertPeriodTracking(period)
    }

    fun startRealtimeSync(userId: Long) {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("period_tracking")
            .document("data")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val period = PeriodEntity(
                    userId = userId,
                    cycleLength = (snapshot.getLong("cycleLength") ?: 28).toInt(),
                    startDate = snapshot.getString("startDate"),
                    endDate = snapshot.getString("endDate"),
                    updatedAt = snapshot.getLong("updatedAt")
                        ?: System.currentTimeMillis()
                )

                CoroutineScope(Dispatchers.IO).launch {
                    periodDao.insertPeriodTracking(period)
                }
            }
    }
}