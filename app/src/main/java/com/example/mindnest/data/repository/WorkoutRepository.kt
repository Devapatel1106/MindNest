package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.WorkoutDao
import com.example.mindnest.data.entity.WorkoutEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getWorkoutsByUser(userId: Long): Flow<List<WorkoutEntity>> {
        return workoutDao.getWorkoutsByUser(userId)
    }

    suspend fun insertWorkout(workout: WorkoutEntity): Long {

        val id = workoutDao.insertWorkout(workout)

        val uid = auth.currentUser?.uid ?: return id

        val workoutMap = hashMapOf(
            "localId" to id,
            "userId" to workout.userId,
            "name" to workout.name,
            "durationMinutes" to workout.durationMinutes,
            "intensity" to workout.intensity,
            "date" to workout.date
        )

        firestore.collection("users")
            .document(uid)
            .collection("workouts")
            .add(workoutMap)

        return id
    }

    suspend fun deleteWorkout(workout: WorkoutEntity) {

        workoutDao.deleteWorkout(workout)

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("workouts")
            .whereEqualTo("localId", workout.id)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    doc.reference.delete()
                }
            }
    }

    fun getWorkoutsByDateRange(
        userId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<WorkoutEntity>> {
        return workoutDao.getWorkoutsByDateRange(userId, startDate, endDate)
    }
}
