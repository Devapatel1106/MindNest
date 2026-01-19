package com.example.mindnest.utils

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mindnest.JournalViewModel
import com.example.mindnest.LogSleepViewModel
import com.example.mindnest.TaskViewModel
import com.example.mindnest.WaterViewModel
import com.example.mindnest.WorkoutViewModel

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TaskViewModel::class.java) -> {
                TaskViewModel(application) as T
            }
            modelClass.isAssignableFrom(WorkoutViewModel::class.java) -> {
                WorkoutViewModel(application) as T
            }
            modelClass.isAssignableFrom(WaterViewModel::class.java) -> {
                WaterViewModel(application) as T
            }
            modelClass.isAssignableFrom(LogSleepViewModel::class.java) -> {
                LogSleepViewModel(application) as T
            }
            modelClass.isAssignableFrom(JournalViewModel::class.java) -> {
                JournalViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
