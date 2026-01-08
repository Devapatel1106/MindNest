package com.example.mindnest.ui.period

import android.app.Application
import androidx.lifecycle.*
import com.example.mindnest.ui.period.data.MindNestDatabase
import kotlinx.coroutines.launch
import java.util.*

class PeriodViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = MindNestDatabase
        .getDatabase(app.applicationContext)
        .periodDao()

    private val repo = PeriodRepository(dao)

    companion object {
        const val ONE_DAY = 24 * 60 * 60 * 1000L
        const val DEFAULT_CYCLE_LENGTH = 28
        const val DEFAULT_PERIOD_LENGTH = 5
    }

    // ---------------- LIVE DATA ----------------

    val nextPeriod = MutableLiveData<Long>()
    val ovulationDay = MutableLiveData<Int?>()
    val phase = MutableLiveData<CyclePhase>()
    val selfCareTip = MutableLiveData<String>()

    // Calendar coloring
    val periodDays = mutableListOf<Int>()
    val fertileDays = mutableListOf<Int>()

    // ---------------- CORE CALCULATION ----------------

    fun calculate(today: Long = System.currentTimeMillis()) {
        viewModelScope.launch {

            val lastPeriod = repo.getLastPeriod() ?: return@launch

            val cycleLength = repo.getAverageCycle() ?: DEFAULT_CYCLE_LENGTH
            val periodLength = lastPeriod.periodLength

            // Next period prediction
            val next = lastPeriod.startDate + cycleLength * ONE_DAY
            nextPeriod.postValue(next)

            // Ovulation day
            val ovulationOffset = cycleLength - 14
            ovulationDay.postValue(ovulationOffset)

            buildCalendarDays(periodLength, ovulationOffset)
            updatePhase(today, lastPeriod.startDate, cycleLength)
        }
    }

    // ---------------- CALENDAR DAYS ----------------

    private fun buildCalendarDays(periodLength: Int, ovulationOffset: Int) {
        periodDays.clear()
        fertileDays.clear()

        // Period days (Day 1..n)
        for (i in 1..periodLength) {
            periodDays.add(i)
        }

        // Fertile window (5 days before ovulation)
        for (i in (ovulationOffset - 5)..(ovulationOffset - 1)) {
            fertileDays.add(i)
        }
    }

    // ---------------- PHASE LOGIC ----------------

    private fun updatePhase(today: Long, startDate: Long, cycleLength: Int) {
        val dayOfCycle =
            ((today - startDate) / ONE_DAY).toInt() + 1

        when {
            dayOfCycle in periodDays -> {
                phase.postValue(CyclePhase.PERIOD)
                selfCareTip.postValue("Rest, hydrate, and be kind to yourself 💖")
            }

            dayOfCycle == ovulationDay.value -> {
                phase.postValue(CyclePhase.OVULATION)
                selfCareTip.postValue("High energy day! Great for workouts ✨")
            }

            dayOfCycle in fertileDays -> {
                phase.postValue(CyclePhase.FERTILE)
                selfCareTip.postValue("Your body is in sync — eat well 🥗")
            }

            else -> {
                phase.postValue(CyclePhase.NORMAL)
                selfCareTip.postValue("Maintain balance & self-care 🌿")
            }
        }
    }

    // ---------------- DATE SELECTION ----------------

    fun onDateSelected(monthMillis: Long, day: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = monthMillis
        cal.set(Calendar.DAY_OF_MONTH, day)

        updatePhase(cal.timeInMillis, cal.timeInMillis, DEFAULT_CYCLE_LENGTH)
    }

    fun getDayStatus(day: Int): String {
        return when {
            day in periodDays -> "Period"
            day == ovulationDay.value -> "Ovulation"
            day in fertileDays -> "Fertile"
            else -> "Normal"
        }
    }
}
