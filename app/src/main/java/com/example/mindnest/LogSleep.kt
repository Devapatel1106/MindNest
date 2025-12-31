package com.example.mindnest

data class LogSleep(
    val sleepTime: String,
    val wakeTime: String,
    val duration: String,
    val date: String = ""
)
