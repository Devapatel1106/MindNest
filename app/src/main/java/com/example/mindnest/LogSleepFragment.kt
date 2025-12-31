package com.example.mindnest

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.databinding.BottomSheetAddLogsleepBinding
import com.example.mindnest.databinding.FragmentLogSleepBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class LogSleepFragment : Fragment(R.layout.fragment_log_sleep) {

    private var _binding: FragmentLogSleepBinding? = null
    private val binding get() = _binding!!

    private val sleepList = mutableListOf<LogSleep>()
    private lateinit var adapter: LogSleepAdapter
    private val sleepViewModel: LogSleepViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLogSleepBinding.bind(view)

        adapter = LogSleepAdapter(sleepList) { /* item click lambda */ }
        binding.sleepRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.sleepRecyclerView.adapter = adapter

        observeSleepLogs()
        setupSwipeToDelete()

        binding.fabAddSleep.setOnClickListener { showAddSleepBottomSheet() }
    }

    private fun observeSleepLogs() {
        sleepViewModel.sleepLogs.observe(viewLifecycleOwner) { logs ->
            sleepList.clear()
            sleepList.addAll(logs)
            adapter.notifyDataSetChanged()
            updateUI()
        }
    }

    private fun showAddSleepBottomSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetDialog)
        val sheetBinding = BottomSheetAddLogsleepBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val fields: List<EditText> = listOf(
            sheetBinding.edtStartHour, sheetBinding.edtStartMinute,
            sheetBinding.edtEndHour, sheetBinding.edtEndMinute
        )

        fun updateSaveButton() {
            sheetBinding.btnSaveSleep.isEnabled = fields.all { it.text?.isNotBlank() == true }
        }

        updateSaveButton()

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateSaveButton() }
            override fun afterTextChanged(s: Editable?) {}
        }
        fields.forEach { it.addTextChangedListener(watcher) }

        fun setupAutoFocus(fromField: EditText, toField: EditText) {
            fromField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 2) toField.requestFocus()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        setupAutoFocus(sheetBinding.edtStartHour, sheetBinding.edtStartMinute)
        setupAutoFocus(sheetBinding.edtEndHour, sheetBinding.edtEndMinute)

        sheetBinding.btnSaveSleep.setOnClickListener {
            val sleepTime = formatTime(
                sheetBinding.edtStartHour.text.toString(),
                sheetBinding.edtStartMinute.text.toString(),
                sheetBinding.rbStartAm.isChecked
            )
            val wakeTime = formatTime(
                sheetBinding.edtEndHour.text.toString(),
                sheetBinding.edtEndMinute.text.toString(),
                sheetBinding.rbEndAm.isChecked
            )
            val duration = calculateDuration(
                sheetBinding.edtStartHour.text.toString().toInt(),
                sheetBinding.edtStartMinute.text.toString().toInt(),
                sheetBinding.rbStartAm.isChecked,
                sheetBinding.edtEndHour.text.toString().toInt(),
                sheetBinding.edtEndMinute.text.toString().toInt(),
                sheetBinding.rbEndAm.isChecked
            )

            sleepViewModel.addSleepLog(sleepTime, wakeTime, duration)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatTime(hourStr: String, minuteStr: String, isAm: Boolean): String {
        val hour = hourStr.padStart(2, '0')
        val minute = minuteStr.padStart(2, '0')
        val amPm = if (isAm) "AM" else "PM"
        return "$hour:$minute $amPm"
    }

    private fun calculateDuration(
        startHour: Int, startMinute: Int, startAm: Boolean,
        endHour: Int, endMinute: Int, endAm: Boolean
    ): String {
        var startH = startHour % 12 + if (!startAm) 12 else 0
        var endH = endHour % 12 + if (!endAm) 12 else 0
        var startTotal = startH * 60 + startMinute
        var endTotal = endH * 60 + endMinute
        if (endTotal <= startTotal) endTotal += 24 * 60
        val diff = endTotal - startTotal
        val hours = diff / 60
        val minutes = diff % 60
        return "${hours}h ${minutes}m"
    }

    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.delete_24px)
            private val background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.lavender_light))
            private val iconMargin = 32

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Sleep Log")
                    .setMessage("Are you sure you want to delete this sleep entry?")
                    .setPositiveButton("Yes") { _, _ -> sleepViewModel.removeSleepLog(position) }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        adapter.notifyItemChanged(position)
                    }.show()
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                deleteIcon?.let { icon ->
                    if (dX > 0) background.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
                    else background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)

                    val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                    val iconBottom = iconTop + icon.intrinsicHeight
                    if (dX > 0) {
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    } else if (dX < 0) {
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    }
                    icon.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.sleepRecyclerView)
    }

    private fun updateUI() {
        binding.layoutEmpty.visibility = if (sleepList.isEmpty()) View.VISIBLE else View.GONE
        binding.sleepRecyclerView.visibility = if (sleepList.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
