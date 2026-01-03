package com.example.mindnest.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mindnest.model.Workout

class WorkoutViewModel : ViewModel() {

    private val _workouts = MutableLiveData<List<Workout>>(emptyList())
    val workouts: LiveData<List<Workout>> = _workouts

    fun addWorkout(workout: Workout) {
        val list = _workouts.value!!.toMutableList()
        list.add(workout)
        _workouts.value = list
    }

    fun deleteWorkout(workout: Workout) {
        val list = _workouts.value!!.toMutableList()
        list.remove(workout)
        _workouts.value = list
    }
}
