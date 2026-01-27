package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.CalorieDao
import com.example.mindnest.data.entity.FoodItemEntity
import com.example.mindnest.data.entity.UserInfoEntity

class CalorieRepository(private val dao: CalorieDao) {

    suspend fun saveUser(user: UserInfoEntity) =
        dao.insertUser(user)

    suspend fun getUser(userId: String) =
        dao.getUser(userId)

    suspend fun addFood(food: FoodItemEntity) =
        dao.insertFood(food)

    suspend fun removeFood(food: FoodItemEntity) =
        dao.deleteFood(food)

    suspend fun getTodayFood(userId: String, date: String) =
        dao.getTodayFood(userId, date)

    suspend fun resetOldFood(userId: String, date: String) =
        dao.deleteOldFood(userId, date)

    suspend fun clearAllFood(userId: String) =
        dao.clearAllFood(userId)
}
