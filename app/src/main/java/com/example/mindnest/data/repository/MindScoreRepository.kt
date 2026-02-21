package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.MindScoreDao
import com.example.mindnest.data.entity.MindScoreEntity

class MindScoreRepository(private val mindScoreDao: MindScoreDao) {

    suspend fun getScoresBetween(userId: Long, startDate: String, endDate: String): List<MindScoreEntity> {
        return mindScoreDao.getScoresBetween(userId, startDate, endDate)
    }

    suspend fun getScoreByDate(userId: Long, date: String): Int? {
        return mindScoreDao.getScoreByDate(userId, date)
    }

    suspend fun insertScore(mindScore: MindScoreEntity) {
        mindScoreDao.insertScore(mindScore)
    }
}