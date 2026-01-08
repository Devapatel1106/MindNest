package com.example.mindnest

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.databinding.BottomSheetAddLogsleepBinding
import com.example.mindnest.databinding.FragmentLogSleepBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class LogSleepFragment : Fragment(R.layout.fragment_log_sleep) {

    private var _binding: FragmentLogSleepBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LogSleepAdapter
    private val sleepList = mutableListOf<LogSleep>()

    private val sleepViewModel: LogSleepViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLogSleepBinding.bind(view)

        adapter = LogSleepAdapter(sleepList) { }

        binding.sleepRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())
        binding.sleepRecyclerView.adapter = adapter

        setupSwipeToDelete()
        updateUI()

        sleepViewModel.sleepLogs.observe(viewLifecycleOwner) {
            sleepList.clear()
            sleepList.addAll(it)
            adapter.notifyDataSetChanged()
            updateUI()
        }

        binding.fabAddSleep.setOnClickListener {
            showAddSleepBottomSheet()
        }
    }

    // 🔹 Convert 24h → 12h AM/PM (UI helper only)
    private fun to12HourFormat(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        return SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(calendar.time)
    }

    // ✅ Bottom sheet: ONLY time input
    private fun showAddSleepBottomSheet() {
        val dialog = BottomSheetDialog(
            requireContext(),
            R.style.TransparentBottomSheetDialog
        )

        val sheetBinding =
            BottomSheetAddLogsleepBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        var startTime: String? = null
        var endTime: String? = null

        fun updateSaveButton() {
            sheetBinding.btnSaveSleep.isEnabled =
                !startTime.isNullOrBlank() && !endTime.isNullOrBlank()
        }

        // 🌙 Sleep Time
        sheetBinding.txtSleepStart.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, h, m ->
                    startTime = to12HourFormat(h, m)
                    sheetBinding.txtSleepStart.text = startTime
                    updateSaveButton()
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false // 12-hour
            ).show()
        }

        // ☀️ Wake Time
        sheetBinding.txtSleepEnd.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, h, m ->
                    endTime = to12HourFormat(h, m)
                    sheetBinding.txtSleepEnd.text = endTime
                    updateSaveButton()
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false
            ).show()
        }

        // ✅ Only pass times — backend handles date & duration
        sheetBinding.btnSaveSleep.setOnClickListener {
            sleepViewModel.addSleepLog(
                sleepTime = startTime!!,
                wakeTime = endTime!!
            )
            dialog.dismiss()
        }


        dialog.show()
    }

    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                val position = viewHolder.adapterPosition
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Sleep Log")
                    .setMessage("Are you sure you want to delete this sleep entry?")
                    .setPositiveButton("Yes") { _, _ ->
                        sleepViewModel.removeSleepLog(position)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val background = ColorDrawable(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.lavender_light
                    )
                )
                val itemView = viewHolder.itemView

                if (dX > 0) {
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        dX.toInt(),
                        itemView.bottom
                    )
                } else {
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                }
                background.draw(c)

                super.onChildDraw(
                    c, recyclerView, viewHolder,
                    dX, dY, actionState, isCurrentlyActive
                )
            }
        }

        ItemTouchHelper(callback)
            .attachToRecyclerView(binding.sleepRecyclerView)
    }

    private fun updateUI() {
        binding.layoutEmpty.visibility =
            if (sleepList.isEmpty()) View.VISIBLE else View.GONE
        binding.sleepRecyclerView.visibility =
            if (sleepList.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
