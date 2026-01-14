package com.example.mindnest.ui.periodtracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PeriodTrackerViewModel : ViewModel() {

    private val _cycleLength = MutableLiveData(28)
    val cycleLength: LiveData<Int> = _cycleLength
    fun setCycleLength(days: Int) { _cycleLength.value = days }

    private val _startDate = MutableLiveData<LocalDate>()
    val startDate: LiveData<LocalDate> = _startDate
    fun setStartDate(date: LocalDate) { _startDate.value = date; updatePeriodDuration() }

    private val _endDate = MutableLiveData<LocalDate>()
    val endDate: LiveData<LocalDate> = _endDate
    fun setEndDate(date: LocalDate) { _endDate.value = date; updatePeriodDuration() }

    private val _periodDuration = MutableLiveData(0)
    val periodDuration: LiveData<Int> = _periodDuration

    private fun updatePeriodDuration() {
        val start = _startDate.value
        val end = _endDate.value
        _periodDuration.value =
            if (start != null && end != null && !end.isBefore(start)) {
                (ChronoUnit.DAYS.between(start, end) + 1).toInt()
            } else 0
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
}
