package com.example.mindnest.ui.periodtracker

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.mindnest.R
import com.example.mindnest.databinding.FragmentPeriodTrackerBinding
import com.example.mindnest.databinding.ItemCycleCardBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class PeriodTrackerFragment : Fragment() {

    private val viewModel: PeriodTrackerViewModel by activityViewModels()
    private var _binding: FragmentPeriodTrackerBinding? = null
    private val binding get() = _binding!!

    private val textFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val numericFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeriodTrackerBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
        updateUpcomingCycles()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setupObservers() {
        viewModel.startDate.observe(viewLifecycleOwner) { updateUpcomingCycles() }
        viewModel.endDate.observe(viewLifecycleOwner) { updateUpcomingCycles() }
        viewModel.cycleLength.observe(viewLifecycleOwner) { updateUpcomingCycles() }
        viewModel.periodDuration.observe(viewLifecycleOwner) {
            binding.tvFlow.text = "Your period lasted for: $it days"
        }
    }


    private fun setupClickListeners() {
        binding.tvStartDate.setOnClickListener { openDatePicker(true) }
        binding.ivStartDate.setOnClickListener { openDatePicker(true) }
        binding.tvEndDate.setOnClickListener { openDatePicker(false) }
        binding.ivEndDate.setOnClickListener { openDatePicker(false) }
        binding.tvCycleDays.setOnClickListener { openCyclePicker() }
    }

    private fun openDatePicker(isStart: Boolean) {
        val date = if (isStart) viewModel.startDate.value else viewModel.endDate.value
        val defaultDate = date ?: LocalDate.now()

        DatePickerDialog(
            requireActivity(),
            { _, year, month, day ->
                val pickedDate = LocalDate.of(year, month + 1, day)
                if (isStart) viewModel.setStartDate(pickedDate)
                else viewModel.setEndDate(pickedDate)
            },
            defaultDate.year,
            defaultDate.monthValue - 1,
            defaultDate.dayOfMonth
        ).show()
    }

    private fun openCyclePicker() {
        Dialog(requireActivity(), R.style.TransparentBottomSheetDialog).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_cycle_picker)

            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.CENTER)
                setDimAmount(0.5f)
            }

            val picker = findViewById<NumberPicker>(R.id.npCycle)
            val tvFocus = findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.tvCycleFocus)

            picker.minValue = 20
            picker.maxValue = 40
            picker.value = viewModel.cycleLength.value?.coerceIn(20, 40) ?: 28
            tvFocus.text = picker.value.toString()

            picker.setOnValueChangedListener { _, _, newVal ->
                tvFocus.text = newVal.toString()
            }

            findViewById<View>(R.id.btnDone)?.setOnClickListener {
                viewModel.setCycleLength(picker.value)
                dismiss()
            }

            show()
        }
    }


    private fun updateUpcomingCycles() {
        // Update the start & end date TextViews
        binding.tvStartDate.text = viewModel.startDate.value?.format(textFormatter) ?: "Select date"
        binding.tvEndDate.text = viewModel.endDate.value?.format(textFormatter) ?: "Select date"

        // Update upcoming cycle cards
        val dates = viewModel.getUpcomingCycles(8)
        val container = binding.llUpcomingCyclesContainer
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        for (date in dates) {
            val cardBinding = ItemCycleCardBinding.inflate(inflater, container, false)

            val cal = Calendar.getInstance()
            cal.set(date.year, date.monthValue - 1, date.dayOfMonth)

            val dayFormatter = java.text.SimpleDateFormat("dd")
            val monthYearFormatter = java.text.SimpleDateFormat("MMM yyyy")

            cardBinding.tvDate.text = dayFormatter.format(cal.time)
            cardBinding.tvMonth.text = monthYearFormatter.format(cal.time)
            cardBinding.tvLabel.text = "Expected"

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.marginEnd = 8.dpToPx()
            cardBinding.root.layoutParams = layoutParams

            container.addView(cardBinding.root)
        }

        binding.tvNextPeriod.text = viewModel.nextPeriod()?.format(textFormatter) ?: "-"
        viewModel.fertileWindow()?.let {
            binding.tvOvulation.text =
                "Fertile window: ${it.first.format(numericFormatter)} – ${it.second.format(numericFormatter)}"
        }
        binding.tvInsight.text = viewModel.cycleInsight()
    }


    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
