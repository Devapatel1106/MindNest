package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.WaterDao
import com.example.mindnest.data.entity.WaterEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WaterRepository(private val waterDao: WaterDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getWaterEntriesByUser(userId: Long): Flow<List<WaterEntity>> {
        return waterDao.getWaterEntriesByUser(userId)
    }

    suspend fun insertWaterEntry(entry: WaterEntity): Long {

        val id = waterDao.insertWaterEntry(entry)

        val uid = auth.currentUser?.uid ?: return id

        try {
            firestore.collection("users")
                .document(uid)
                .collection("water")
                .document(id.toString())
                .set(hashMapOf(
                    "id" to id,
                    "userId" to entry.userId,
                    "amountMl" to entry.amountMl,
                    "date" to entry.date,
                    "createdAt" to entry.createdAt
                ))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return id
    }

    fun startRealtimeSync(userId: Long) {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("water")
            .addSnapshotListener { snapshot, error ->
                if (snapshot == null || error != null) return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val entry = WaterEntity(
                        id = doc.getLong("id") ?: 0,
                        userId = userId,
                        amountMl = (doc.getLong("amountMl") ?: 0).toInt(),
                        date = doc.getString("date") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        waterDao.insertWaterEntry(entry)
                    }
                }
            }
    }
}