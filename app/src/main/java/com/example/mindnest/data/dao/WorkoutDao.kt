package com.example.mindnest.data.dao

import androidx.room.*
import com.example.mindnest.data.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY date DESC")
    fun getWorkoutsByUser(userId: Long): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    fun getWorkoutsByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<WorkoutEntity>>
}
