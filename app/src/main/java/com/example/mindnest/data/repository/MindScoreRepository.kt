package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.MindScoreDao
import com.example.mindnest.data.entity.MindScoreEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class MindScoreRepository(private val mindScoreDao: MindScoreDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getScoresBetween(
        userId: Long,
        startDate: String,
        endDate: String
    ): List<MindScoreEntity> {
        return mindScoreDao.getScoresBetween(userId, startDate, endDate)
    }

    fun observeScoresBetween(
        userId: Long,
        startDate: String,
        endDate: String
    ): Flow<List<MindScoreEntity>> {
        return mindScoreDao.observeScoresBetween(userId, startDate, endDate)
    }

    suspend fun getScoreByDate(
        userId: Long,
        date: String
    ): Int? {
        return mindScoreDao.getScoreByDate(userId, date)
    }

    suspend fun insertScore(mindScore: MindScoreEntity) {

        mindScoreDao.insertScore(mindScore)

        syncScoreToFirebase(mindScore)
    }

    private fun syncScoreToFirebase(mindScore: MindScoreEntity) {

        val uid = auth.currentUser?.uid ?: return
        val docId = "${mindScore.userId}_${mindScore.date}"

        val map = hashMapOf(
            "userId" to mindScore.userId,
            "date" to mindScore.date,
            "score" to mindScore.score
        )

        firestore.collection("users")
            .document(uid)
            .collection("mind_scores")
            .document(docId)
            .set(map)
    }

    fun startRealtimeSync(userId: Long) {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("mind_scores")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot == null) return@addSnapshotListener

                CoroutineScope(Dispatchers.IO).launch {

                    for (doc in snapshot.documents) {

                        val score = MindScoreEntity(
                            userId = userId,
                            date = doc.getString("date") ?: "",
                            score = (doc.getLong("score") ?: 0).toInt()
                        )

                        mindScoreDao.insertScore(score)
                    }
                }
            }
    }

    suspend fun syncMindScoresFromFirebase(userId: Long) {

        val uid = auth.currentUser?.uid ?: return

        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("mind_scores")
            .get()
            .await()

        for (doc in snapshot.documents) {

            val score = MindScoreEntity(
                userId = userId,
                date = doc.getString("date") ?: "",
                score = (doc.getLong("score") ?: 0).toInt()
            )

            mindScoreDao.insertScore(score)
        }
    }
}