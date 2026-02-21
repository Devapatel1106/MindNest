package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.PeriodDao
import com.example.mindnest.data.entity.PeriodEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

class PeriodRepository(private val periodDao: PeriodDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getPeriodTracking(userId: Long): Flow<PeriodEntity?> {
        return periodDao.getPeriodTrackingByUser(userId)
    }

    suspend fun savePeriod(period: PeriodEntity) {


        periodDao.insertPeriodTracking(period)

        val uid = auth.currentUser?.uid ?: return

        val periodMap = hashMapOf(
            "userId" to period.userId,
            "cycleLength" to period.cycleLength,
            "startDate" to period.startDate,
            "endDate" to period.endDate,
            "updatedAt" to period.updatedAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("period_tracking")
            .document("data")
            .set(periodMap)
    }
}