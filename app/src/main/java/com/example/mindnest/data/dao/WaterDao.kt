package com.example.mindnest.data.dao

import androidx.room.*
import com.example.mindnest.data.entity.WaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {

    @Query("SELECT * FROM water_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun getWaterEntriesByUser(userId: Long): Flow<List<WaterEntity>>

    @Query("SELECT * FROM water_entries WHERE userId = :userId AND date = :date")
    fun getWaterEntriesByDate(userId: Long, date: String): Flow<List<WaterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterEntry(entry: WaterEntity): Long

    @Query("SELECT SUM(amountMl) FROM water_entries WHERE userId = :userId AND date = :date")
    suspend fun getTotalWaterByDate(userId: Long, date: String): Int?
}