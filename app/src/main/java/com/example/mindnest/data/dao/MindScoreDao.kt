package com.example.mindnest.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.mindnest.data.entity.MindScoreEntity

@Dao
interface MindScoreDao {

    @Query("SELECT * FROM mind_score WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getScoresBetween(userId: Long, startDate: String, endDate: String): List<MindScoreEntity>

    @Query("SELECT score FROM mind_score WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getScoreByDate(userId: Long, date: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(mindScore: MindScoreEntity)

    @Query("SELECT * FROM mind_score WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeScoresBetween(
        userId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<MindScoreEntity>>
}