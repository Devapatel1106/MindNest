package com.example.mindnest.ui.journal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindnest.R
import com.example.mindnest.JournalViewModel
import com.example.mindnest.adapter.JournalAdapter
import com.example.mindnest.adapter.MonthAdapter
import com.example.mindnest.databinding.BottomSheetJournalBinding
import com.example.mindnest.databinding.FragmentJournalMoodBinding
import com.example.mindnest.model.JournalEntry
import com.example.mindnest.utils.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class JournalMoodFragment : Fragment() {

    private lateinit var binding: FragmentJournalMoodBinding
    private lateinit var journalAdapter: JournalAdapter
    private lateinit var monthAdapter: MonthAdapter

    private val monthList = mutableListOf<String>()
    private val filteredJournals = mutableListOf<JournalEntry>()
    private var selectedMonth: String? = null

    private val viewModel: JournalViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentJournalMoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupMonthRecycler()
        setupJournalRecycler()
        observeJournals()

        binding.fabAddItem.setOnClickListener {
            openBottomSheet()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data when fragment becomes visible (e.g., after login)
        viewModel.reloadJournals()
    }

    private fun observeJournals() {
        viewModel.allJournals.observe(viewLifecycleOwner) { journals ->
            monthList.clear()
            journals.map { it.monthYear }.distinct().forEach { monthList.add(it) }
            monthAdapter.notifyDataSetChanged()

            selectedMonth?.let { filterByMonth(it) } ?: run {
                if (monthList.isNotEmpty()) {
                    selectedMonth = monthList.last()
                    monthAdapter.setSelected(selectedMonth!!)
                    filterByMonth(selectedMonth!!)
                } else {
                    filteredJournals.clear()
                    journalAdapter.notifyDataSetChanged()
                    updateEmptyState()
                }
            }
        }
    }

    private fun setupMonthRecycler() {
        monthAdapter = MonthAdapter(monthList) { month ->
            selectedMonth = month
            filterByMonth(month)
        }

        binding.rvMonths.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMonths.adapter = monthAdapter
    }

    private fun setupJournalRecycler() {
        journalAdapter = JournalAdapter(filteredJournals) { journalEntry ->
            openBottomSheet(journalEntry)
        }

        binding.itemRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.itemRecyclerView.adapter = journalAdapter
    }

    private fun filterByMonth(month: String) {
        filteredJournals.clear()
        filteredJournals.addAll(viewModel.allJournals.value?.filter { it.monthYear == month } ?: emptyList())
        journalAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (filteredJournals.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.itemRecyclerView.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.itemRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun openBottomSheet(editEntry: JournalEntry? = null) {
        val dialog = BottomSheetDialog(
            requireContext(),
            R.style.TransparentBottomSheetDialog
        )
        val sheetBinding = BottomSheetJournalBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val moodMap = mapOf(
            "Happy" to "😊",
            "Neutral" to "🙂",
            "Sad" to "😔"
        )
        val moodNames = moodMap.keys.toList()

        val initialMoodName = moodMap.entries.find { it.value == editEntry?.mood }?.key ?: "Neutral"
        var selectedMood = editEntry?.mood ?: "🙂"

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            moodNames
        )
        sheetBinding.moodDropdown.setAdapter(adapter)

        sheetBinding.edtJournal.setText(editEntry?.text ?: "")
        sheetBinding.moodDropdown.setText(initialMoodName, false)

        sheetBinding.moodDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedName = moodNames[position]
            selectedMood = moodMap[selectedName] ?: "🙂"
        }

        sheetBinding.btnSaveJournal.setOnClickListener {
            val text = sheetBinding.edtJournal.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val date = Date()
            val day = SimpleDateFormat("dd", Locale.getDefault()).format(date)
            val weekday = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
            val monthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date).uppercase()

            if (editEntry != null) {
                editEntry.text = text
                editEntry.mood = selectedMood
                editEntry.day = day
                editEntry.weekday = weekday
                editEntry.monthYear = monthYear
                viewModel.updateJournal(editEntry)
            } else {
                val entry = JournalEntry(id = 0, day = day, weekday = weekday, text = text, monthYear = monthYear, mood = selectedMood)
                viewModel.addJournal(entry)
            }

            selectedMonth = monthYear
            monthAdapter.setSelected(monthYear)
            filterByMonth(monthYear)
            binding.rvMonths.scrollToPosition(monthList.indexOf(monthYear))

            dialog.dismiss()
        }

        dialog.show()
    }
}
