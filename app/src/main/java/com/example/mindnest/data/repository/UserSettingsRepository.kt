package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.UserSettingsDao
import com.example.mindnest.data.entity.UserSettings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {

    private val firestore = FirebaseFirestore.getInstance()

    fun getUserSettings(userId: Long): Flow<UserSettings?> {
        return userSettingsDao.getUserSettings(userId)
    }

    suspend fun saveWaterTarget(userId: Long, target: Int) {

        val settings = UserSettings(
            userId = userId,
            waterTargetMl = target
        )

        userSettingsDao.insertOrUpdateSettings(settings)

        firestore.collection("user_settings")
            .document(userId.toString())
            .set(settings)
            .await()
    }

    suspend fun getWaterTarget(userId: Long): Int {
        return 0
    }
}