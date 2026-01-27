package com.example.calorietracker.model

data class UserInfo(
    var weight: Int = 65,
    var height: Int = 165,
    var age: Int = 25,
    var gender: String = "Male",
    var targetCalories: Int = 2000
)
