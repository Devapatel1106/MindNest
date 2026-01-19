package com.example.mindnest

data class LogSleep(
    val id: Long = 0,
    val sleepTime: String,
    val wakeTime: String,
    val duration: String,
    val date: String = ""
)
