package com.example.mindnest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.databinding.ItemDateHeaderBinding
import com.example.mindnest.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

sealed class TaskListItem {
    data class DateHeader(val date: Long) : TaskListItem()
    data class TaskItem(val task: Task) : TaskListItem()
}

class TaskAdapter(
    private val items: MutableList<TaskListItem>,
    private val onEditClick: (task: Task) -> Unit,
    private val onCompletedToggle: (task: Task, completed: Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_TASK = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TaskListItem.DateHeader -> TYPE_DATE
            is TaskListItem.TaskItem -> TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> {
                val binding = ItemDateHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                DateViewHolder(binding)
            }
            TYPE_TASK -> {
                val binding = ItemTaskBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                TaskViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TaskListItem.DateHeader -> (holder as DateViewHolder).bind(item)
            is TaskListItem.TaskItem -> (holder as TaskViewHolder).bind(item.task)
        }
    }

    inner class DateViewHolder(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: TaskListItem.DateHeader) {
            binding.tvDateHeader.text = sdf.format(header.date)
        }
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.txtTaskTitle.text = task.title

            binding.chkTaskDone.setOnCheckedChangeListener(null)
            binding.chkTaskDone.isChecked = task.completed

            binding.chkTaskDone.setOnCheckedChangeListener { _, isChecked ->
                onCompletedToggle(task, isChecked)
            }

            binding.btnEditTask.setOnClickListener {
                onEditClick(task)
            }
        }
    }

    fun submitList(newTasks: List<Task>) {
        items.clear()


        val grouped = newTasks.groupBy { it.createdAt.toDateWithoutTime() }
            .toSortedMap(compareByDescending { it })

        grouped.forEach { (date, tasksForDate) ->
            items.add(TaskListItem.DateHeader(date))
            tasksForDate.forEach { items.add(TaskListItem.TaskItem(it)) }
        }

        notifyDataSetChanged()
    }


    private fun Long.toDateWithoutTime(): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = this@toDateWithoutTime }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
