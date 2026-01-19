package com.example.mindnest.model

data class Workout(
    val id: Long = 0,
    val name: String,
    val durationMinutes: Int,
    val intensity: String
)
