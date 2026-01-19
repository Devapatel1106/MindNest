package com.example.mindnest.data.dao

import androidx.room.*
import com.example.mindnest.data.entity.PeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {

    @Query("SELECT * FROM period_tracking WHERE userId = :userId LIMIT 1")
    fun getPeriodTrackingByUser(userId: Long): Flow<PeriodEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodTracking(period: PeriodEntity)

    @Update
    suspend fun updatePeriodTracking(period: PeriodEntity)
}
