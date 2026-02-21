package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.CalorieDao
import com.example.mindnest.data.entity.FoodItemEntity
import com.example.mindnest.data.entity.UserInfoEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CalorieRepository(private val dao: CalorieDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun saveUser(user: UserInfoEntity) {


        dao.insertUser(user)


        val uid = auth.currentUser?.uid ?: return

        val map = hashMapOf(
            "userId" to user.userId,
            "weight" to user.weight,
            "height" to user.height,
            "age" to user.age,
            "gender" to user.gender,
            "targetCalories" to user.targetCalories
        )

        firestore.collection("users")
            .document(uid)
            .collection("calorie")
            .document("user_info")
            .set(map)
    }

    suspend fun getUser(userId: String) =
        dao.getUser(userId)

    suspend fun addFood(food: FoodItemEntity) {


        dao.insertFood(food)


        val uid = auth.currentUser?.uid ?: return

        val map = hashMapOf(
            "name" to food.name,
            "category" to food.category,
            "calories" to food.calories,
            "quantity" to food.quantity,
            "date" to food.date
        )

        firestore.collection("users")
            .document(uid)
            .collection("calorie")
            .document("data")
            .collection("food_items")
            .add(map)
    }

    suspend fun removeFood(food: FoodItemEntity) {
        dao.deleteFood(food)

    }

    suspend fun getTodayFood(userId: String, date: String) =
        dao.getTodayFood(userId, date)

    suspend fun resetOldFood(userId: String, date: String) =
        dao.deleteOldFood(userId, date)

    suspend fun clearAllFood(userId: String) {
        dao.clearAllFood(userId)

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("calorie")
            .document("data")
            .collection("food_items")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    doc.reference.delete()

                }
            }
    }
}