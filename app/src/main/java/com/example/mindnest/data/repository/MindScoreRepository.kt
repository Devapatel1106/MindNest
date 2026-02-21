package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.MindScoreDao
import com.example.mindnest.data.entity.MindScoreEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MindScoreRepository(private val mindScoreDao: MindScoreDao) {

    private val firestore = FirebaseFirestore.getInstance()
    suspend fun getScoresBetween(
        userId: Long,
        startDate: String,
        endDate: String
    ): List<MindScoreEntity> {
        return mindScoreDao.getScoresBetween(userId, startDate, endDate)
    }

    suspend fun getScoreByDate(
        userId: Long,
        date: String
    ): Int? {
        return mindScoreDao.getScoreByDate(userId, date)
    }

    suspend fun insertScore(mindScore: MindScoreEntity) {

        mindScoreDao.insertScore(mindScore)
        val docId = "${mindScore.userId}_${mindScore.date}"

        firestore.collection("mind_scores")
            .document(docId)
            .set(
                mapOf(
                    "userId" to mindScore.userId,
                    "date" to mindScore.date,
                    "score" to mindScore.score
                )
            )
            .await()
    }
}