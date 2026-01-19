package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.SleepDao
import com.example.mindnest.data.entity.SleepEntity
import kotlinx.coroutines.flow.Flow

class SleepRepository(private val sleepDao: SleepDao) {
    fun getSleepLogsByUser(userId: Long): Flow<List<SleepEntity>> {
        return sleepDao.getSleepLogsByUser(userId)
    }

    suspend fun insertSleepLog(sleep: SleepEntity): Long {
        return sleepDao.insertSleepLog(sleep)
    }

    suspend fun deleteSleepLog(sleep: SleepEntity) {
        sleepDao.deleteSleepLog(sleep)
    }

    suspend fun deleteSleepLogById(sleepId: Long) {
        sleepDao.deleteSleepLogById(sleepId)
    }
}
