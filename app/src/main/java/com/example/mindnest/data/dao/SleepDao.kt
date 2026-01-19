package com.example.mindnest.data.dao

import androidx.room.*
import com.example.mindnest.data.entity.SleepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_logs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSleepLogsByUser(userId: Long): Flow<List<SleepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(sleep: SleepEntity): Long

    @Delete
    suspend fun deleteSleepLog(sleep: SleepEntity)

    @Query("DELETE FROM sleep_logs WHERE id = :sleepId")
    suspend fun deleteSleepLogById(sleepId: Long)
}
