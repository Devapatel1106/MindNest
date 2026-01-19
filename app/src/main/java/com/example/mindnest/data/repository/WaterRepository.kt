package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.WaterDao
import com.example.mindnest.data.entity.WaterEntity
import kotlinx.coroutines.flow.Flow

class WaterRepository(private val waterDao: WaterDao) {
    fun getWaterEntriesByUser(userId: Long): Flow<List<WaterEntity>> {
        return waterDao.getWaterEntriesByUser(userId)
    }

    fun getWaterEntriesByDate(userId: Long, date: String): Flow<List<WaterEntity>> {
        return waterDao.getWaterEntriesByDate(userId, date)
    }

    suspend fun insertWaterEntry(entry: WaterEntity): Long {
        return waterDao.insertWaterEntry(entry)
    }

    suspend fun getTotalWaterByDate(userId: Long, date: String): Int {
        return waterDao.getTotalWaterByDate(userId, date) ?: 0
    }
}
