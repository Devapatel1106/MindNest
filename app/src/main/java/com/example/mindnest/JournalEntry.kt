package com.example.mindnest.model

data class JournalEntry(
    var id: Long = 0,
    var day: String,
    var weekday: String,
    var text: String,
    var monthYear: String,
    var mood: String
)
