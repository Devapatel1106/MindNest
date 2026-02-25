package com.example.mindnest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mindnest.data.entity.WorkoutEntity
import com.example.mindnest.model.Workout
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MindNestApplication
    private val preferenceManager = PreferenceManager(application)

    private val _workouts = MutableLiveData<List<Workout>>(emptyList())
    val workouts: LiveData<List<Workout>> = _workouts

    init {
        startRealtimeSync()
        loadWorkouts()
    }

    private fun startRealtimeSync() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        app.workoutRepository.startRealtimeSync(userId)
    }

    private fun loadWorkouts() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            app.workoutRepository.getWorkoutsByUser(userId)
                .map { entities ->
                    entities.map { entity ->
                        Workout(
                            id = entity.id,
                            name = entity.name,
                            durationMinutes = entity.durationMinutes,
                            intensity = entity.intensity,
                            date = entity.date
                        )
                    }
                }
                .collect { workoutList ->
                    _workouts.value = workoutList
                }
        }
    }

    fun addWorkout(workout: Workout) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            val entity = WorkoutEntity(
                id = 0,
                userId = userId,
                name = workout.name,
                durationMinutes = workout.durationMinutes,
                intensity = workout.intensity,
                date = System.currentTimeMillis()
            )
            app.workoutRepository.insertWorkout(entity)
        }
    }

    fun deleteWorkout(workout: Workout) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0 || workout.id == 0L) return

        viewModelScope.launch {
            val entity = WorkoutEntity(
                id = workout.id,
                userId = userId,
                name = workout.name,
                durationMinutes = workout.durationMinutes,
                intensity = workout.intensity,
                date = workout.date
            )
            app.workoutRepository.deleteWorkout(entity)
        }
    }
}