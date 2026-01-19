package com.example.mindnest

import android.app.Application
import com.example.mindnest.data.database.AppDatabase
import com.example.mindnest.data.repository.*

class MindNestApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    val userRepository by lazy { UserRepository(database.userDao()) }
    val taskRepository by lazy { TaskRepository(database.taskDao()) }
    val workoutRepository by lazy { WorkoutRepository(database.workoutDao()) }
    val waterRepository by lazy { WaterRepository(database.waterDao()) }
    val sleepRepository by lazy { SleepRepository(database.sleepDao()) }
    val journalRepository by lazy { JournalRepository(database.journalDao()) }
    val periodRepository by lazy { PeriodRepository(database.periodDao()) }
    val userSettingsRepository by lazy { UserSettingsRepository(database.userSettingsDao()) }
}
