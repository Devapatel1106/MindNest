package com.example.mindnest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

class LogSleepViewModel : ViewModel() {

    private val _sleepLogs =
        MutableLiveData<MutableList<LogSleep>>(mutableListOf())
    val sleepLogs: LiveData<MutableList<LogSleep>> = _sleepLogs

    // ✅ Only time inputs — date & duration handled here
    fun addSleepLog(sleepTime: String, wakeTime: String) {
        val currentList = _sleepLogs.value ?: mutableListOf()

        val date = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        ).format(Date())

        val duration = calculateDuration(sleepTime, wakeTime)

        currentList.add(
            LogSleep(
                date = date,
                sleepTime = sleepTime,
                wakeTime = wakeTime,
                duration = duration
            )
        )

        _sleepLogs.value = currentList
    }

    // ❌ No date update allowed
    fun updateSleepLog(
        position: Int,
        updatedSleepTime: String,
        updatedWakeTime: String
    ) {
        val currentList = _sleepLogs.value ?: return
        if (position in currentList.indices) {

            val oldLog = currentList[position]
            val duration =
                calculateDuration(updatedSleepTime, updatedWakeTime)

            currentList[position] = oldLog.copy(
                sleepTime = updatedSleepTime,
                wakeTime = updatedWakeTime,
                duration = duration
            )

            _sleepLogs.value = currentList
        }
    }

    fun removeSleepLog(position: Int) {
        val currentList = _sleepLogs.value ?: return
        if (position in currentList.indices) {
            currentList.removeAt(position)
            _sleepLogs.value = currentList
        }
    }

    // 🧮 12-hour duration calculation (overnight handled)
    private fun calculateDuration(
        start: String,
        end: String
    ): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val startTime = sdf.parse(start)!!
        val endTime = sdf.parse(end)!!

        var diff = endTime.time - startTime.time
        if (diff < 0) diff += 24 * 60 * 60 * 1000

        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60

        return "${hours}h ${minutes}m"
    }
}
