package com.example.mindnest.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.calorietracker.model.FoodItem
import com.example.calorietracker.model.UserInfo
import com.example.mindnest.data.database.AppDatabase
import com.example.mindnest.data.entity.FoodItemEntity
import com.example.mindnest.data.entity.UserInfoEntity
import com.example.mindnest.data.repository.CalorieRepository
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalorieViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CalorieRepository
    private val today = LocalDate.now().toString()
    private val preferenceManager = PreferenceManager(application)

    private val USER_ID = preferenceManager.getUserId().toString()

    val userInfo = MutableLiveData<UserInfo>()
    val foodList = MutableLiveData<MutableList<FoodItem>>(mutableListOf())
    val consumedCalories = MutableLiveData(0)
    val remainingCalories = MutableLiveData(0)
    val suggestion = MutableLiveData("You can eat this much today!")

    init {
        val dao = AppDatabase.getDatabase(application).calorieDao()
        repository = CalorieRepository(dao)

        viewModelScope.launch {

            val savedUser = repository.getUser(USER_ID)

            if (savedUser != null) {
                userInfo.value = UserInfo(
                    savedUser.weight,
                    savedUser.height,
                    savedUser.age,
                    savedUser.gender,
                    savedUser.targetCalories
                )
            } else {

                val savedGender = preferenceManager.getUserGender() ?: "Male"

                val defaultUser = UserInfo(
                    weight = 0,
                    height = 0,
                    age = 0,
                    gender = savedGender,
                    targetCalories = 2000
                )

                userInfo.value = defaultUser

                repository.saveUser(
                    UserInfoEntity(
                        userId = USER_ID,
                        weight = 60,
                        height = 170,
                        age = 20,
                        gender = savedGender,
                        targetCalories = 2000
                    )
                )
            }

            repository.resetOldFood(USER_ID, today)

            val foodEntities = repository.getTodayFood(USER_ID, today)

            foodList.value = foodEntities.map {
                FoodItem(it.name, it.category, it.calories, it.quantity)
            }.toMutableList()

            recalcCalories()
        }
    }

    fun addFood(food: FoodItem) {
        viewModelScope.launch {

            val entity = FoodItemEntity(
                userId = USER_ID,
                name = food.name,
                category = food.category,
                calories = food.calories,
                quantity = food.quantity,
                date = today
            )

            repository.addFood(entity)

            val list = foodList.value ?: mutableListOf()
            list.add(food)
            foodList.value = list

            recalcCalories()
        }
    }

    fun removeFood(food: FoodItem) {
        viewModelScope.launch {

            val entity = FoodItemEntity(
                userId = USER_ID,
                name = food.name,
                category = food.category,
                calories = food.calories,
                quantity = food.quantity,
                date = today
            )

            repository.removeFood(entity)

            val list = foodList.value ?: mutableListOf()
            list.remove(food)
            foodList.value = list

            recalcCalories()
        }
    }

    fun clearFoodList() {

        foodList.value = mutableListOf()
        consumedCalories.value = 0

        val target = userInfo.value?.targetCalories ?: 2000
        remainingCalories.value = target
        suggestion.value = "You can eat this much today!"

        viewModelScope.launch {
            // ✅ Correct function (no error now)
            repository.clearAllFood(USER_ID)
        }
    }

    fun updateUserInfo(weight: Int, height: Int, age: Int, gender: String) {

        val target = userInfo.value?.targetCalories ?: 2000

        userInfo.value = UserInfo(weight, height, age, gender, target)

        viewModelScope.launch {
            repository.saveUser(
                UserInfoEntity(
                    userId = USER_ID,
                    weight = weight,
                    height = height,
                    age = age,
                    gender = gender,
                    targetCalories = target
                )
            )
        }

        recalcCalories()
    }

    fun increaseTarget(amount: Int = 100) {
        val info = userInfo.value ?: return
        val newTarget = info.targetCalories + amount

        userInfo.value = info.copy(targetCalories = newTarget)

        viewModelScope.launch {
            repository.saveUser(
                UserInfoEntity(
                    USER_ID,
                    info.weight,
                    info.height,
                    info.age,
                    info.gender,
                    newTarget
                )
            )
        }

        recalcCalories()
    }

    fun decreaseTarget(amount: Int = 100) {
        val info = userInfo.value ?: return
        val newTarget = (info.targetCalories - amount).coerceAtLeast(0)

        userInfo.value = info.copy(targetCalories = newTarget)

        viewModelScope.launch {
            repository.saveUser(
                UserInfoEntity(
                    USER_ID,
                    info.weight,
                    info.height,
                    info.age,
                    info.gender,
                    newTarget
                )
            )
        }

        recalcCalories()
    }

    private fun recalcCalories() {

        val list = foodList.value ?: emptyList()
        val consumed = list.sumOf { it.calories * it.quantity }

        consumedCalories.value = consumed

        val target = userInfo.value?.targetCalories ?: 2000
        remainingCalories.value = target - consumed

        suggestion.value = when {
            consumed < target -> "You can still eat ${target - consumed} kcal"
            consumed == target -> "Goal reached!"
            else -> "Exceeded by ${consumed - target} kcal!"
        }
    }
}
