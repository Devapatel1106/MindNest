package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.WorkoutDao
import com.example.mindnest.data.entity.WorkoutEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getWorkoutsByUser(userId: Long) =
        workoutDao.getWorkoutsByUser(userId)

    suspend fun insertWorkout(workout: WorkoutEntity): Long {
        val id = workoutDao.insertWorkout(workout)
        syncWorkoutToFirebase(workout.copy(id = id))
        return id
    }

    suspend fun deleteWorkout(workout: WorkoutEntity) {
        workoutDao.deleteWorkout(workout)
        deleteFromFirebase(workout.id)
    }

    private fun syncWorkoutToFirebase(workout: WorkoutEntity) {
        val uid = auth.currentUser?.uid ?: return
        val workoutMap = hashMapOf(
            "id" to workout.id,
            "userId" to workout.userId,
            "name" to workout.name,
            "durationMinutes" to workout.durationMinutes,
            "intensity" to workout.intensity,
            "date" to workout.date
        )
        firestore.collection("users")
            .document(uid)
            .collection("workouts")
            .document(workout.id.toString())
            .set(workoutMap)
    }

    private fun deleteFromFirebase(workoutId: Long) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("workouts")
            .document(workoutId.toString())
            .delete()
    }

    fun startRealtimeSync(userId: Long) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("workouts")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                CoroutineScope(Dispatchers.IO).launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val id = doc.getLong("id") ?: 0L
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val workout = WorkoutEntity(
                                    id = id,
                                    userId = userId,
                                    name = doc.getString("name") ?: "",
                                    durationMinutes = (doc.getLong("durationMinutes") ?: 0).toInt(),
                                    intensity = doc.getString("intensity") ?: "",
                                    date = doc.getLong("date") ?: System.currentTimeMillis()
                                )
                                workoutDao.insertWorkout(workout)
                            }
                            DocumentChange.Type.REMOVED -> {
                                workoutDao.deleteWorkoutById(id)
                            }
                        }
                    }
                }
            }
    }
}