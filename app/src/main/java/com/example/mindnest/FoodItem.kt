package com.example.calorietracker.model

data class FoodItem(
    val name: String,
    val category: String,
    val calories: Int,
    var quantity: Int = 1
)
