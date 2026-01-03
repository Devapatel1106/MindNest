package com.example.mindnest.ui.workout

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.R
import com.example.mindnest.databinding.BottomSheetAddWorkoutBinding
import com.example.mindnest.databinding.FragmentWorkoutTrackingBinding
import com.example.mindnest.model.Workout
import com.example.mindnest.viewmodel.WorkoutViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class WorkoutTrackingFragment :
    Fragment(R.layout.fragment_workout_tracking) {

    private var _binding: FragmentWorkoutTrackingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutViewModel by activityViewModels()
    private val workoutList = mutableListOf<Workout>()
    private lateinit var adapter: WorkoutAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentWorkoutTrackingBinding.bind(view)

        adapter = WorkoutAdapter(workoutList)
        binding.workoutRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.workoutRecyclerView.adapter = adapter

        binding.fabAddWorkout.setOnClickListener {
            showAddWorkoutBottomSheet()
        }

        observeWorkouts()
        setupSwipeToDelete()
    }

    private fun observeWorkouts() {
        viewModel.workouts.observe(viewLifecycleOwner) { list ->
            adapter.updateList(list)
            updateUI()
        }
    }

    private fun showAddWorkoutBottomSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetDialog)
        val sheetBinding = BottomSheetAddWorkoutBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val workoutTypes = listOf("Cardio", "HIIT", "Walking", "Jogging", "Core Strength", "Yoga")
        val intensityLevels = listOf("Low", "Medium", "High")

        sheetBinding.actWorkoutType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, workoutTypes)
        )
        sheetBinding.actIntensity.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, intensityLevels)
        )

        sheetBinding.btnSaveWorkout.setOnClickListener {
            val workoutName = sheetBinding.actWorkoutType.text.toString().trim()
            val intensity = sheetBinding.actIntensity.text.toString().trim()
            val durationMinutes = sheetBinding.edtDuration.text.toString().toIntOrNull() ?: 0

            if (workoutName.isEmpty() || workoutName == "Select workout" || durationMinutes <= 0 || intensity.isEmpty()) {
                return@setOnClickListener
            }

            viewModel.addWorkout(Workout(workoutName, durationMinutes, intensity))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateUI() {
        if (workoutList.isEmpty()) {
            binding.layoutWorkoutEmpty.visibility = View.VISIBLE
            binding.workoutRecyclerView.visibility = View.GONE
        } else {
            binding.layoutWorkoutEmpty.visibility = View.GONE
            binding.workoutRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupSwipeToDelete() {
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.delete_24px)
        val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
        val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.lavender_light)

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val workoutToDelete = adapter.getWorkoutAt(position) ?: return

                AlertDialog.Builder(requireContext())
                    .setTitle("Delete workout?")
                    .setMessage("Are you sure you want to delete this workout?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.deleteWorkout(workoutToDelete)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    // Draw background
                    val background = ColorDrawable(backgroundColor)
                    if (dX > 0) {
                        background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                    } else {
                        background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    }
                    background.draw(c)

                    // Draw delete icon
                    deleteIcon?.let {
                        val iconMargin = (itemView.height - intrinsicHeight) / 2
                        val top = itemView.top + iconMargin
                        val bottom = top + intrinsicHeight

                        if (dX > 0) {
                            val left = itemView.left + iconMargin
                            val right = left + intrinsicWidth
                            it.setBounds(left, top, right, bottom)
                        } else if (dX < 0) {
                            val right = itemView.right - iconMargin
                            val left = right - intrinsicWidth
                            it.setBounds(left, top, right, bottom)
                        }
                        it.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.workoutRecyclerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
