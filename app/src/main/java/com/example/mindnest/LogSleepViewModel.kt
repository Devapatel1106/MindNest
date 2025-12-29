package com.example.mindnest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LogSleepViewModel : ViewModel() {

    private val _sleepLogs = MutableLiveData<MutableList<LogSleep>>(mutableListOf())
    val sleepLogs: LiveData<MutableList<LogSleep>> = _sleepLogs

    fun addSleepLog(log: LogSleep) {
        val currentList = _sleepLogs.value ?: mutableListOf()
        currentList.add(log)
        _sleepLogs.value = currentList
    }

    fun updateSleepLog(
        position: Int,
        updatedDate: String,
        updatedSleepTime: String,
        updatedWakeTime: String
    ) {
        val currentList = _sleepLogs.value ?: return
        if (position in currentList.indices) {
            currentList[position] = LogSleep(
                date = updatedDate,
                sleepTime = updatedSleepTime,
                wakeTime = updatedWakeTime
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
}
