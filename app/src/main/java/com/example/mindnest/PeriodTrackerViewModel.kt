package com.example.mindnest.ui.periodtracker

import androidx.lifecycle.*
import com.example.mindnest.data.entity.PeriodEntity
import com.example.mindnest.data.repository.PeriodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class PeriodCycle(val startDate: LocalDate, val endDate: LocalDate)

class PeriodTrackerViewModel(private val repository: PeriodRepository) : ViewModel() {

    private val _cycleLength = MutableLiveData(28)
    val cycleLength: LiveData<Int> = _cycleLength

    private val _startDate = MutableLiveData<LocalDate?>()
    val startDate: LiveData<LocalDate?> = _startDate

    private val _endDate = MutableLiveData<LocalDate?>()
    val endDate: LiveData<LocalDate?> = _endDate

    private val _periodDuration = MutableLiveData(0)
    val periodDuration: LiveData<Int> = _periodDuration

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun loadPeriod(userId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPeriodTracking(userId).collect { period ->
                period?.let { updateFromPeriodEntity(it) }
            }
        }
        repository.startRealtimeSync(userId)
    }

    private fun updateFromPeriodEntity(period: PeriodEntity) {
        val start = period.startDate?.let { runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull() }
        val end = period.endDate?.let { runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull() }

        _cycleLength.postValue(period.cycleLength)
        _startDate.postValue(start)
        _endDate.postValue(end)

        val duration = if (start != null && end != null && !end.isBefore(start)) {
            (ChronoUnit.DAYS.between(start, end) + 1).toInt()
        } else 0
        _periodDuration.postValue(duration)
    }

    fun savePeriod(userId: Long) {
        val start = _startDate.value?.format(dateFormatter)
        val end = _endDate.value?.format(dateFormatter)
        val cycle = _cycleLength.value ?: 28

        val period = PeriodEntity(
            userId = userId,
            cycleLength = cycle,
            startDate = start,
            endDate = end
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.savePeriod(period)
        }
    }

    fun setCycleLength(days: Int) {
        _cycleLength.value = days
    }

    fun setStartDate(date: LocalDate) {
        _startDate.value = date
        recalculateDuration(date, _endDate.value)
    }

    fun setEndDate(date: LocalDate) {
        _endDate.value = date
        recalculateDuration(_startDate.value, date)
    }

    private fun recalculateDuration(start: LocalDate?, end: LocalDate?) {
        val duration = if (start != null && end != null && !end.isBefore(start)) {
            (ChronoUnit.DAYS.between(start, end) + 1).toInt()
        } else 0
        _periodDuration.value = duration
    }

    fun nextPeriod(): LocalDate? {
        val start = _startDate.value ?: return null
        val cycle = _cycleLength.value ?: 28
        return start.plusDays(cycle.toLong())
    }

    fun fertileWindow(): Pair<LocalDate, LocalDate>? {
        val start = _startDate.value ?: return null
        val cycle = _cycleLength.value ?: 28
        val ovulation = start.plusDays((cycle / 2).toLong())
        return Pair(ovulation.minusDays(5), ovulation.plusDays(1))
    }

    fun cycleInsight(): String {
        val cycle = _cycleLength.value ?: return ""
        return when {
            cycle < 21 -> "Short cycle • Stress or sleep may affect it"
            cycle in 21..35 -> "Healthy & regular cycle"
            else -> "Long cycle • Monitor consistency"
        }
    }

    fun getUpcomingCycles(count: Int = 12): List<LocalDate> {
        val start = _startDate.value ?: return emptyList()
        val cycle = _cycleLength.value ?: 28
        val result = mutableListOf<LocalDate>()
        var next = start.plusDays(cycle.toLong())
        repeat(count) {
            result.add(next)
            next = next.plusDays(cycle.toLong())
        }
        return result
    }

    fun getCycleLengthText(): String = "${_cycleLength.value ?: 28} days"

    fun getCurrentCycle(): PeriodCycle? {
        val start = _startDate.value
        val end = _endDate.value
        val today = LocalDate.now()
        if (start != null && end != null && !today.isBefore(start) && !today.isAfter(end)) {
            return PeriodCycle(start, end)
        }
        return null
    }

    fun getLastCycle(): PeriodCycle? {
        val start = _startDate.value
        val end = _endDate.value
        if (start != null && end != null) return PeriodCycle(start, end)
        return null
    }
}

class PeriodTrackerViewModelFactory(private val repository: PeriodRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PeriodTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PeriodTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}