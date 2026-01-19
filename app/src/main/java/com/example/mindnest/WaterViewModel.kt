package com.example.mindnest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mindnest.data.entity.WaterEntity
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WaterViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MindNestApplication
    private val preferenceManager = PreferenceManager(application)

    private val _entries = MutableLiveData<MutableList<com.example.mindnest.ui.water.WaterEntry>>(mutableListOf())
    val entries: LiveData<MutableList<com.example.mindnest.ui.water.WaterEntry>> = _entries

    private val _dailyTarget = MutableLiveData<Int>(0)
    val dailyTarget: LiveData<Int> = _dailyTarget

    init {
        loadWaterEntries()
        loadDailyTarget()
    }

    fun reloadData() {
        loadWaterEntries()
        loadDailyTarget()
    }

    private fun loadWaterEntries() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) {
            _entries.value = mutableListOf()
            return
        }

        viewModelScope.launch {
            // Load both entries and target, then combine them
            app.waterRepository.getWaterEntriesByUser(userId)
                .collect { entities ->
                    val target = _dailyTarget.value ?: 0
                    val entryList = entities.map { entity ->
                        com.example.mindnest.ui.water.WaterEntry(
                            date = entity.date,
                            consumedMl = entity.amountMl,
                            targetMl = target
                        )
                    }
                    _entries.value = entryList.toMutableList()
                }
        }
    }

    private fun loadDailyTarget() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) {
            _dailyTarget.value = 0
            return
        }

        viewModelScope.launch {
            app.userSettingsRepository.getUserSettings(userId)
                .collect { settings ->
                    val target = settings?.waterTargetMl ?: 0
                    _dailyTarget.value = target
                    // Update entries with the new target
                    updateEntriesWithTarget(target)
                }
        }
    }

    private fun updateEntriesWithTarget(target: Int) {
        val currentEntries = _entries.value ?: mutableListOf()
        val updatedEntries = currentEntries.map { entry ->
            entry.copy(targetMl = target)
        }
        _entries.value = updatedEntries.toMutableList()
    }

    fun addWater(amount: Int) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        val today = getToday()
        viewModelScope.launch {
            val entity = WaterEntity(
                id = 0,
                userId = userId,
                amountMl = amount,
                date = today
            )
            app.waterRepository.insertWaterEntry(entity)
        }
    }

    fun setTarget(target: Int) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            app.userSettingsRepository.saveWaterTarget(userId, target)
            // The target will be updated via Flow in loadDailyTarget()
        }
    }

    fun todayTotal(): Int {
        val today = getToday()
        return _entries.value?.filter { it.date == today }?.sumOf { it.consumedMl } ?: 0
    }

    fun isTargetAchieved(date: String): Boolean {
        val target = _dailyTarget.value ?: 0
        if (target == 0) return false
        val total = _entries.value?.filter { it.date == date }?.sumOf { it.consumedMl } ?: 0
        return total >= target
    }

    private fun getToday(): String = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
}
