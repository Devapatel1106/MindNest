package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.PeriodDao
import com.example.mindnest.data.entity.PeriodEntity
import kotlinx.coroutines.flow.Flow

class PeriodRepository(private val periodDao: PeriodDao) {

    fun getPeriodTracking(userId: Long): Flow<PeriodEntity?> {
        return periodDao.getPeriodTrackingByUser(userId)
    }

    suspend fun savePeriod(period: PeriodEntity) {
        periodDao.insertPeriodTracking(period)
    }
}
