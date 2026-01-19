package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.WorkoutDao
import com.example.mindnest.data.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {
    fun getWorkoutsByUser(userId: Long): Flow<List<WorkoutEntity>> {
        return workoutDao.getWorkoutsByUser(userId)
    }

    suspend fun insertWorkout(workout: WorkoutEntity): Long {
        return workoutDao.insertWorkout(workout)
    }

    suspend fun deleteWorkout(workout: WorkoutEntity) {
        workoutDao.deleteWorkout(workout)
    }

    fun getWorkoutsByDateRange(userId: Long, startDate: Long, endDate: Long): Flow<List<WorkoutEntity>> {
        return workoutDao.getWorkoutsByDateRange(userId, startDate, endDate)
    }
}
