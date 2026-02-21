package com.example.mindnest.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mindnest.data.dao.*
import com.example.mindnest.data.entity.*

@Database(
    entities = [
        User::class,
        TaskEntity::class,
        WorkoutEntity::class,
        WaterEntity::class,
        SleepEntity::class,
        JournalEntity::class,
        PeriodEntity::class,
        UserSettings::class,
        FoodItemEntity::class,
        UserInfoEntity::class,
        MindScoreEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {


    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun waterDao(): WaterDao
    abstract fun sleepDao(): SleepDao
    abstract fun journalDao(): JournalDao
    abstract fun periodDao(): PeriodDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun calorieDao(): CalorieDao
    abstract fun mindScoreDao(): MindScoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop old mind_score table and recreate with userId
                database.execSQL("DROP TABLE IF EXISTS mind_score")
                database.execSQL("""
                    CREATE TABLE mind_score (
                        userId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        PRIMARY KEY(userId, date)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindnest_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}