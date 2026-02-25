package com.example.mindnest.data.dao

import androidx.room.*
import com.example.mindnest.data.entity.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    fun getUserSettings(userId: Long): Flow<UserSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: UserSettings)
}