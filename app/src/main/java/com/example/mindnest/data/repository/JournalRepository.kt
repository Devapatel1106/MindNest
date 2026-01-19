package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.JournalDao
import com.example.mindnest.data.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    fun getJournalEntriesByUser(userId: Long): Flow<List<JournalEntity>> {
        return journalDao.getJournalEntriesByUser(userId)
    }

    fun getJournalEntryByDate(userId: Long, date: String): Flow<JournalEntity?> {
        return journalDao.getJournalEntryByDate(userId, date)
    }

    suspend fun insertJournalEntry(entry: JournalEntity): Long {
        return journalDao.insertJournalEntry(entry)
    }

    suspend fun updateJournalEntry(entry: JournalEntity) {
        journalDao.updateJournalEntry(entry)
    }

    suspend fun deleteJournalEntry(entry: JournalEntity) {
        journalDao.deleteJournalEntry(entry)
    }
}
