package com.example.mindnest.ui.period

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mindnest.R
import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator
import com.example.mindnest.databinding.FragmentPeriodTrackerBinding
import java.util.*

class PeriodTrackerFragment :
    Fragment(R.layout.fragment_period_tracker) {

    private val vm: PeriodViewModel by viewModels()

    private var _binding: FragmentPeriodTrackerBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarAdapter
    private val calendar = Calendar.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentPeriodTrackerBinding.bind(view)

        setupCalendar()
        setupMonthNavigation()
        observeViewModel()

        vm.calculate()
    }

    // ---------------- CALENDAR ----------------

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter(
            days = generateCalendarDays(),
            periodDays = vm.periodDays,
            fertileDays = vm.fertileDays,
            ovulationDay = vm.ovulationDay.value
        ) { selectedDay ->

            vm.onDateSelected(calendar.timeInMillis, selectedDay)
            updateDonut(selectedDay)
        }

        binding.recyclerCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
            isNestedScrollingEnabled = false
        }

        updateMonthTitle()
    }

    private fun generateCalendarDays(): List<Int?> {
        val list = mutableListOf<Int?>()

        val temp = calendar.clone() as Calendar
        temp.set(Calendar.DAY_OF_MONTH, 1)

        val firstDay = temp.get(Calendar.DAY_OF_WEEK)
        val emptyDays = (firstDay + 5) % 7 // Monday start

        repeat(emptyDays) { list.add(null) }

        val maxDays = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..maxDays) {
            list.add(day)
        }

        return list
    }

    // ---------------- MONTH NAVIGATION ----------------

    private fun setupMonthNavigation() {
        binding.btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            refreshCalendar()
        }

        binding.btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            refreshCalendar()
        }
    }

    private fun refreshCalendar() {
        calendarAdapter.updateDays(
            generateCalendarDays(),
            vm.periodDays,
            vm.fertileDays,
            vm.ovulationDay.value
        )
        updateMonthTitle()
    }

    private fun updateMonthTitle() {
        val month = calendar.getDisplayName(
            Calendar.MONTH,
            Calendar.LONG,
            Locale.getDefault()
        )
        binding.tvMonth.text = "$month ${calendar.get(Calendar.YEAR)}"
    }

    // ---------------- DONUT ----------------

    private fun updateDonut(day: Int) {
        val status = vm.getDayStatus(day)

        // include binding already available
        "Day $day".also { binding.viewDonut.tvDay.text = it }
        status.also { binding.viewDonut.tvStatus.text = it }
    }



    // ---------------- VIEWMODEL ----------------

    private fun observeViewModel() {
        vm.phase.observe(viewLifecycleOwner) {
            binding.tvPhase.text = it.name
        }

        vm.selfCareTip.observe(viewLifecycleOwner) {
            binding.tvTip.text = it
        }

        vm.nextPeriod.observe(viewLifecycleOwner) {
            binding.tvNextPeriod.text = "Next period: ${formatDate(it)}"
        }
    }

    private fun formatDate(time: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        return "${cal.get(Calendar.DAY_OF_MONTH)} " +
                cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
