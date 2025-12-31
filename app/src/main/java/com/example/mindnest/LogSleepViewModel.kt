package com.example.mindnest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

class LogSleepViewModel : ViewModel() {

    private val _sleepLogs = MutableLiveData<MutableList<LogSleep>>(mutableListOf())
    val sleepLogs: LiveData<MutableList<LogSleep>> = _sleepLogs

    fun addSleepLog(sleepTime: String, wakeTime: String, duration: String) {
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val newLog = LogSleep(
            sleepTime = sleepTime,
            wakeTime = wakeTime,
            duration = duration,
            date = date
        )
        val updatedList = _sleepLogs.value ?: mutableListOf()
        updatedList.add(0, newLog)
        _sleepLogs.value = updatedList
    }

    fun removeSleepLog(position: Int) {
        val updatedList = _sleepLogs.value ?: return
        if (position in updatedList.indices) {
            updatedList.removeAt(position)
            _sleepLogs.value = updatedList
        }
    }
}
