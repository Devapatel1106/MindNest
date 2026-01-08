package com.example.mindnest.ui.period

import com.example.mindnest.ui.period.data.PeriodDao
import com.example.mindnest.ui.period.data.PeriodEntry
import kotlin.math.roundToInt

class PeriodRepository(private val dao: PeriodDao) {

    suspend fun addPeriod(start: Long, length: Int) {
        dao.insertPeriod(PeriodEntry(startDate = start, periodLength = length))
    }

    suspend fun getAverageCycle(): Int {
        val periods = dao.getAllPeriods()
        if (periods.size < 2) return 28

        val cycles = periods.zipWithNext { a, b ->
            ((b.startDate - a.startDate) / ONE_DAY).toInt()
        }

        return cycles.takeLast(5).average().roundToInt()
    }

    suspend fun getLastPeriod(): PeriodEntry? =
        dao.getAllPeriods().lastOrNull()
}
