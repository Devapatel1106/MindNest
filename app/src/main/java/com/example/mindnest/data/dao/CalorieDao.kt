package com.example.mindnest.data.dao

import androidx.room.*
import com.example.mindnest.data.entity.FoodItemEntity
import com.example.mindnest.data.entity.UserInfoEntity

@Dao
interface CalorieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserInfoEntity)

    @Query("SELECT * FROM user_info WHERE userId = :userId LIMIT 1")
    suspend fun getUser(userId: String): UserInfoEntity?

    @Insert
    suspend fun insertFood(food: FoodItemEntity)

    @Delete
    suspend fun deleteFood(food: FoodItemEntity)

    @Query("SELECT * FROM food_item WHERE userId = :userId AND date = :today")
    suspend fun getTodayFood(userId: String, today: String): List<FoodItemEntity>

    @Query("DELETE FROM food_item WHERE userId = :userId AND date != :today")
    suspend fun deleteOldFood(userId: String, today: String)

    @Query("DELETE FROM food_item WHERE userId = :userId")
    suspend fun clearAllFood(userId: String)
}
