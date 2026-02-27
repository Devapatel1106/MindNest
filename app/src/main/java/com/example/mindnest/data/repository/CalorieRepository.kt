package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.CalorieDao
import com.example.mindnest.data.entity.FoodItemEntity
import com.example.mindnest.data.entity.UserInfoEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CalorieRepository(private val dao: CalorieDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun saveUser(user: UserInfoEntity) {

        dao.insertUser(user)

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("calorie")
            .document("user_info")
            .set(user)
            .await()
    }

    suspend fun getUser(userId: String) =
        dao.getUser(userId)

    fun getUserFlow(userId: String): Flow<UserInfoEntity?> =
        dao.getUserFlow(userId)

    fun getTodayFood(userId: String, date: String): Flow<List<FoodItemEntity>> =
        dao.getTodayFood(userId, date)

    suspend fun addFood(food: FoodItemEntity) {

        val id = dao.insertFood(food).toInt()

        val uid = auth.currentUser?.uid ?: return

        val finalFood = food.copy(id = id)

        firestore.collection("users")
            .document(uid)
            .collection("calorie")
            .document("data")
            .collection("food_items")
            .document(id.toString())
            .set(finalFood)
            .await()
    }

    suspend fun removeFoodByData(
        userId: String,
        name: String,
        category: String,
        date: String
    ) {

        val foods = dao.getTodayFoodOnce(userId, date)

        val match = foods.find {
            it.name == name && it.category == category
        } ?: return

        dao.deleteFood(match)

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("calorie")
            .document("data")
            .collection("food_items")
            .document(match.id.toString())
            .delete()
            .await()
    }

    suspend fun resetOldFood(userId: String, date: String) =
        dao.deleteOldFood(userId, date)

    suspend fun clearAllFood(userId: String) {

        val uid = auth.currentUser?.uid ?: return


        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("calorie")
            .document("data")
            .collection("food_items")
            .get()
            .await()

        if (!snapshot.isEmpty) {

            val batch = firestore.batch()
            snapshot.documents.forEach {
                batch.delete(it.reference)
            }
            batch.commit().await()
        }

        dao.clearAllFood(userId)
    }

    fun startUserRealtimeSync(userId: String) {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("calorie")
            .document("user_info")
            .addSnapshotListener { doc, _ ->

                if (doc == null || !doc.exists()) return@addSnapshotListener

                CoroutineScope(Dispatchers.IO).launch {

                    val user = UserInfoEntity(
                        userId = userId,
                        weight = (doc.getLong("weight") ?: 0).toInt(),
                        height = (doc.getLong("height") ?: 0).toInt(),
                        age = (doc.getLong("age") ?: 0).toInt(),
                        gender = doc.getString("gender") ?: "",
                        targetCalories = (doc.getLong("targetCalories") ?: 0).toInt()
                    )

                    dao.insertUser(user)
                }
            }
    }

    fun startFoodRealtimeSync(userId: String) {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("calorie")
            .document("data")
            .collection("food_items")
            .addSnapshotListener { snapshot, e ->

                if (e != null || snapshot == null) return@addSnapshotListener

                CoroutineScope(Dispatchers.IO).launch {

                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val foodId = (doc.getLong("id") ?: 0L).toInt()

                        if (change.type == DocumentChange.Type.REMOVED) {
                            // Sync deletions across phones
                            val foodToDelete = FoodItemEntity(
                                id = foodId,
                                userId = userId,
                                name = "",
                                category = "",
                                calories = 0,
                                quantity = 0,
                                date = ""
                            )
                            dao.deleteFood(foodToDelete)
                        } else {

                            val food = FoodItemEntity(
                                id = foodId,
                                userId = userId,
                                name = doc.getString("name") ?: "",
                                category = doc.getString("category") ?: "",
                                calories = (doc.getLong("calories") ?: 0L).toInt(),
                                quantity = (doc.getLong("quantity") ?: 0L).toInt(),
                                date = doc.getString("date") ?: ""
                            )
                            dao.insertFood(food)
                        }
                    }
                }
            }
    }
}