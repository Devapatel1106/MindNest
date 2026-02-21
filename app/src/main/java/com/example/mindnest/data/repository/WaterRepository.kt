package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.WaterDao
import com.example.mindnest.data.entity.WaterEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

class WaterRepository(private val waterDao: WaterDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getWaterEntriesByUser(userId: Long): Flow<List<WaterEntity>> {
        return waterDao.getWaterEntriesByUser(userId)
    }

    fun getWaterEntriesByDate(userId: Long, date: String): Flow<List<WaterEntity>> {
        return waterDao.getWaterEntriesByDate(userId, date)
    }

    suspend fun insertWaterEntry(entry: WaterEntity): Long {

        val id = waterDao.insertWaterEntry(entry)

        val uid = auth.currentUser?.uid ?: return id

        val waterMap = hashMapOf(
            "localId" to id,
            "userId" to entry.userId,
            "amountMl" to entry.amountMl,
            "date" to entry.date,
            "createdAt" to entry.createdAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("water")
            .add(waterMap)

        return id
    }

    suspend fun getTotalWaterByDate(userId: Long, date: String): Int {
        return waterDao.getTotalWaterByDate(userId, date) ?: 0
    }
}
