package com.example.mindnest.ui.period.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PeriodEntry::class,
        DailyLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MindNestDatabase : RoomDatabase() {

    abstract fun periodDao(): PeriodDao

    companion object {
        @Volatile
        private var INSTANCE: MindNestDatabase? = null

        fun getDatabase(context: Context): MindNestDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MindNestDatabase::class.java,
                    "mindnest_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
