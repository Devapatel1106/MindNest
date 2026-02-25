// WaterViewModel.kt
package com.example.mindnest

import android.app.Application
import androidx.lifecycle.*
import com.example.mindnest.data.entity.WaterEntity
import com.example.mindnest.ui.water.WaterEntry
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MindNestApplication
    private val preferenceManager = PreferenceManager(application)
    private var userId: Long = 0
    private var realtimeStarted = false

    private val _entries = MutableLiveData<MutableList<WaterEntry>>(mutableListOf())
    val entries: LiveData<MutableList<WaterEntry>> = _entries

    private val _dailyTarget = MutableLiveData(0)
    val dailyTarget: LiveData<Int> = _dailyTarget

    init {
        viewModelScope.launch {
            userId = preferenceManager.getUserId()
            if (userId > 0) reloadData()
        }
    }

    fun reloadData() {
        startRealtimeSync()
        observeDailyTarget()
        observeWaterEntries()
    }

    private fun startRealtimeSync() {
        if (realtimeStarted || userId <= 0) return
        realtimeStarted = true
        app.waterRepository.startRealtimeSync(userId)
        app.userSettingsRepository.startRealtimeSync(userId)
    }

    private fun observeWaterEntries() {
        if (userId <= 0) return
        viewModelScope.launch {
            app.waterRepository.getWaterEntriesByUser(userId).collect { entities ->
                val target = _dailyTarget.value ?: 0
                val list = entities.map {
                    WaterEntry(it.date, it.amountMl, target)
                }
                _entries.postValue(list.toMutableList())
            }
        }
    }

    private fun observeDailyTarget() {
        if (userId <= 0) return
        viewModelScope.launch {
            app.userSettingsRepository.getUserSettings(userId).collect { settings ->
                val target = settings?.waterTargetMl ?: 0
                _dailyTarget.postValue(target)
                _entries.value?.let { list ->
                    _entries.postValue(list.map { it.copy(targetMl = target) }.toMutableList())
                }
            }
        }
    }

    fun addWater(amount: Int, onInserted: (() -> Unit)? = null) {
        if (userId <= 0) return
        val today = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            val entity = WaterEntity(userId = userId, amountMl = amount, date = today)
            app.waterRepository.insertWaterEntry(entity)
            onInserted?.invoke()
        }
    }

    fun setTarget(target: Int) {
        if (userId <= 0) return
        viewModelScope.launch {
            app.userSettingsRepository.saveWaterTarget(userId, target)
            _dailyTarget.postValue(target)
            _entries.value?.let { list ->
                _entries.postValue(list.map { it.copy(targetMl = target) }.toMutableList())
            }
        }
    }

    fun todayTotal(): Int {
        val today = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
        return _entries.value?.filter { it.date == today }?.sumOf { it.consumedMl } ?: 0
    }

    fun isTargetAchieved(date: String): Boolean {
        val target = _dailyTarget.value ?: return false
        if (target == 0) return false
        val total = _entries.value?.filter { it.date == date }?.sumOf { it.consumedMl } ?: 0
        return total >= target
    }
}