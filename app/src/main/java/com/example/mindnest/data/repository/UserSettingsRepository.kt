package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.UserSettingsDao
import com.example.mindnest.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow

class UserSettingsRepository(private val userSettingsDao: UserSettingsDao) {
    fun getUserSettings(userId: Long): Flow<UserSettings?> {
        return userSettingsDao.getUserSettings(userId)
    }

    suspend fun saveWaterTarget(userId: Long, target: Int) {
        val settings = UserSettings(
            userId = userId,
            waterTargetMl = target
        )
        userSettingsDao.insertOrUpdateSettings(settings)
    }

    suspend fun getWaterTarget(userId: Long): Int {
        // This will be handled via Flow in ViewModel
        return 0 // Default, actual value comes from Flow
    }
}
