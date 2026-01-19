package com.example.mindnest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mindnest.MindNestApplication
import com.example.mindnest.data.entity.SleepEntity
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LogSleepViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MindNestApplication
    private val preferenceManager = PreferenceManager(application)

    private val _sleepLogs = MutableLiveData<MutableList<LogSleep>>(mutableListOf())
    val sleepLogs: LiveData<MutableList<LogSleep>> = _sleepLogs

    init {
        loadSleepLogs()
    }

    private fun loadSleepLogs() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            app.sleepRepository.getSleepLogsByUser(userId)
                .map { entities ->
                    entities.map { entity ->
                        val sleepTime = formatTime(entity.startHour, entity.startMinute)
                        val wakeTime = formatTime(entity.endHour, entity.endMinute)
                        val duration = calculateDuration(
                            entity.startHour, entity.startMinute,
                            entity.endHour, entity.endMinute
                        )
                        LogSleep(
                            id = entity.id,
                            sleepTime = sleepTime,
                            wakeTime = wakeTime,
                            duration = duration,
                            date = entity.date
                        )
                    }
                }
                .collect { logList ->
                    _sleepLogs.value = logList.toMutableList()
                }
        }
    }

    fun addSleepLog(sleepTime: String, wakeTime: String, duration: String) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        val date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
        val (startHour, startMinute) = parseTime(sleepTime)
        val (endHour, endMinute) = parseTime(wakeTime)

        viewModelScope.launch {
            val entity = SleepEntity(
                id = 0,
                userId = userId,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                date = date
            )
            app.sleepRepository.insertSleepLog(entity)
        }
    }

    fun removeSleepLog(position: Int) {
        val log = _sleepLogs.value?.getOrNull(position) ?: return
        val userId = preferenceManager.getUserId()
        if (userId <= 0 || log.id == 0L) return

        viewModelScope.launch {
            val (startHour, startMinute) = parseTime(log.sleepTime)
            val (endHour, endMinute) = parseTime(log.wakeTime)
            
            val entity = SleepEntity(
                id = log.id,
                userId = userId,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                date = log.date
            )
            app.sleepRepository.deleteSleepLog(entity)
        }
    }

    private fun parseTime(timeStr: String): Pair<Int, Int> {
        // Format: "HH:MM AM/PM" or "HH:MM"
        try {
            val parts = timeStr.split(" ")
            val timePart = parts[0]
            val isPm = parts.getOrNull(1)?.uppercase() == "PM"
            
            val (hourStr, minuteStr) = timePart.split(":")
            var hour = hourStr.toInt()
            val minute = minuteStr.toInt()
            
            if (isPm && hour != 12) hour += 12
            if (!isPm && hour == 12) hour = 0
            
            return Pair(hour, minute)
        } catch (e: Exception) {
            return Pair(0, 0)
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = hour % 12
        val displayHour = if (h == 0) 12 else h
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    private fun calculateDuration(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): String {
        var startH = startHour
        var endH = endHour
        var startTotal = startH * 60 + startMinute
        var endTotal = endH * 60 + endMinute
        if (endTotal <= startTotal) endTotal += 24 * 60
        val diff = endTotal - startTotal
        val hours = diff / 60
        val minutes = diff % 60
        return "${hours}h ${minutes}m"
    }
}
