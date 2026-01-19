package com.example.mindnest.data.dao

import androidx.room.*
import com.example.mindnest.data.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun getJournalEntriesByUser(userId: Long): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND date = :date")
    fun getJournalEntryByDate(userId: Long, date: String): Flow<JournalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntity): Long

    @Update
    suspend fun updateJournalEntry(entry: JournalEntity)

    @Delete
    suspend fun deleteJournalEntry(entry: JournalEntity)
}
