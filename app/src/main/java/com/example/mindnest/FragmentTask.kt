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

    private val taskViewModel: TaskViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application)
    }

    private val displayList = mutableListOf<TaskListItem>()
    private lateinit var adapter: TaskAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskBinding.bind(view)

        adapter = TaskAdapter(
            items = displayList,
            onEditClick = { task -> showEditTaskBottomSheet(task) },
            onCompletedToggle = { task, completed -> taskViewModel.setTaskCompletion(task, completed) }
        )

        binding.taskRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FragmentTask.adapter
        }

        setupSwipeToDelete()
        observeTasks()

        binding.fabAddTask.setOnClickListener {
            showAddTaskBottomSheet()
        }
    }

    private fun observeTasks() {
        taskViewModel.tasks.observe(viewLifecycleOwner) { list ->
            rebuildList(list)
            updateUI()
        }
    }

    private fun rebuildList(tasks: List<Task>) {
        displayList.clear()
        val grouped = tasks.groupBy { it.createdAt.toDateWithoutTime() }
            .toSortedMap(compareByDescending { it }) // newest date first

        grouped.forEach { (date, tasksForDate) ->
            displayList.add(TaskListItem.DateHeader(date))
            tasksForDate.forEach { displayList.add(TaskListItem.TaskItem(it)) }
        }

        adapter.notifyDataSetChanged()
    }

    private fun showAddTaskBottomSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetDialog)
        val sheetBinding = BottomSheetAddTaskBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        sheetBinding.btnSaveTask.isEnabled = false

        sheetBinding.edtTaskTitle.addTextChangedListener {
            sheetBinding.btnSaveTask.isEnabled = !it.isNullOrBlank()
        }

        sheetBinding.btnSaveTask.setOnClickListener {
            val title = sheetBinding.edtTaskTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                taskViewModel.addTask(title)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditTaskBottomSheet(task: Task) {
        val dialog = BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetDialog)
        val sheetBinding = BottomSheetAddTaskBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        sheetBinding.edtTaskTitle.setText(task.title)
        sheetBinding.btnSaveTask.isEnabled = task.title.isNotBlank()

        sheetBinding.edtTaskTitle.addTextChangedListener {
            sheetBinding.btnSaveTask.isEnabled = !it.isNullOrBlank()
        }

        sheetBinding.btnSaveTask.setOnClickListener {
            val updatedTitle = sheetBinding.edtTaskTitle.text.toString().trim()
            if (updatedTitle.isNotEmpty()) {
                taskViewModel.updateTask(task, updatedTitle)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = displayList[position]
                if (item is TaskListItem.TaskItem) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Task")
                        .setMessage("Are you sure you want to delete this task?")
                        .setPositiveButton("Yes") { _, _ -> taskViewModel.deleteTask(item.task) }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                            adapter.notifyItemChanged(position)
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    adapter.notifyItemChanged(position)
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.lavender_light))
                    if (dX > 0) background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                    else background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)

                    val icon = ContextCompat.getDrawable(requireContext(), R.drawable.delete_24px) ?: return
                    val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + icon.intrinsicHeight
                    val iconLeft = if (dX > 0) itemView.left + iconMargin else itemView.right - iconMargin - icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconLeft + icon.intrinsicWidth, iconBottom)
                    icon.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.taskRecyclerView)
    }

    private fun updateUI() {
        val isEmpty = displayList.none { it is TaskListItem.TaskItem }
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.INVISIBLE
        binding.taskRecyclerView.visibility = if (isEmpty) View.INVISIBLE else View.VISIBLE
    }

    private fun Long.toDateWithoutTime(): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = this@toDateWithoutTime }
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
