package com.example.mindnest.ui.period.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PeriodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(entry: PeriodEntry)

    @Query("SELECT * FROM period_entries ORDER BY startDate ASC")
    suspend fun getAllPeriods(): List<PeriodEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(log: DailyLog)

    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    suspend fun getLogByDate(date: Long): DailyLog?
}
