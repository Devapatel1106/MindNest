package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.UserSettingsDao
import com.example.mindnest.data.entity.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getUserSettings(userId: Long): Flow<UserSettings?> {
        return userSettingsDao.getUserSettings(userId)
    }

    suspend fun saveWaterTarget(userId: Long, target: Int) {
        if (userId <= 0) return
        val settings = UserSettings(userId = userId, waterTargetMl = target)

        userSettingsDao.insertOrUpdateSettings(settings)

        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .collection("settings")
                .document("water")
                .set(settings)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRealtimeSync(userId: Long) {
        if (userId <= 0) return
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("settings")
            .document("water")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val settings = snapshot.toObject(UserSettings::class.java) ?: return@addSnapshotListener
                CoroutineScope(Dispatchers.IO).launch {
                    userSettingsDao.insertOrUpdateSettings(settings.copy(userId = userId))
                }
            }
    }
}