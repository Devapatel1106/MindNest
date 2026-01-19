package com.example.mindnest

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.databinding.BottomSheetAddTaskBinding
import com.example.mindnest.databinding.FragmentTaskBinding
import com.example.mindnest.utils.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog

class FragmentTask : Fragment(R.layout.fragment_task) {

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

    private val taskViewModel: TaskViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskBinding.bind(view)

        adapter = TaskAdapter(taskList) { position ->
            showEditTaskBottomSheet(position)
        }

        binding.taskRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FragmentTask.adapter
        }

        setupSwipeToDelete()
        updateUI()

        taskViewModel.tasks.observe(viewLifecycleOwner) { list ->
            taskList.clear()
            taskList.addAll(list)
            adapter.notifyDataSetChanged()
            updateUI()
        }

        binding.fabAddTask.setOnClickListener {
            showAddTaskBottomSheet()
        }
    }

    private fun showAddTaskBottomSheet() {
        val dialog = BottomSheetDialog(
            requireContext(),
            R.style.TransparentBottomSheetDialog
        )

        val sheetBinding = BottomSheetAddTaskBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        sheetBinding.btnSaveTask.isEnabled = false

        sheetBinding.edtTaskTitle.addTextChangedListener { text ->
            sheetBinding.btnSaveTask.isEnabled = !text.isNullOrBlank()
        }

        sheetBinding.btnSaveTask.setOnClickListener {
            val title = sheetBinding.edtTaskTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                taskViewModel.addTask(Task(title = title))
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditTaskBottomSheet(position: Int) {
        val task = taskList[position]

        val dialog = BottomSheetDialog(
            requireContext(),
            R.style.TransparentBottomSheetDialog
        )

        val sheetBinding = BottomSheetAddTaskBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        sheetBinding.edtTaskTitle.setText(task.title)
        sheetBinding.btnSaveTask.isEnabled = task.title.isNotBlank()

        sheetBinding.edtTaskTitle.addTextChangedListener { text ->
            sheetBinding.btnSaveTask.isEnabled = !text.isNullOrBlank()
        }

        sheetBinding.btnSaveTask.setOnClickListener {
            val updatedTitle = sheetBinding.edtTaskTitle.text.toString().trim()
            if (updatedTitle.isNotEmpty()) {
                taskViewModel.updateTask(position, updatedTitle)
                dialog.dismiss()
            }
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
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition

                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Yes") { _, _ ->
                        taskViewModel.removeTask(position)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        adapter.notifyItemChanged(position)
                    }
                    .setCancelable(false)
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
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val background = ColorDrawable(
                        ContextCompat.getColor(requireContext(), R.color.lavender_light)
                    )

                    if (dX > 0) {
                        background.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt(),
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

                    val icon = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.delete_24px
                    ) ?: return

                    val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + icon.intrinsicHeight

                    if (dX > 0) {
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    } else {
                        val iconLeft =
                            itemView.right - iconMargin - icon.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    }
                    icon.draw(c)
                }
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.taskRecyclerView)
    }

    private fun updateUI() {
        if (taskList.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.taskRecyclerView.visibility = View.INVISIBLE
        } else {
            binding.layoutEmpty.visibility = View.INVISIBLE
            binding.taskRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
