package com.example.mindnest.ui.workout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.R
import com.example.mindnest.model.Workout
import java.text.SimpleDateFormat
import java.util.*

sealed class WorkoutListItem {
    data class DateHeader(val date: Long) : WorkoutListItem()
    data class WorkoutItem(val workout: Workout) : WorkoutListItem()
}

class WorkoutAdapter(private val list: MutableList<WorkoutListItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_WORKOUT = 1
    }

    inner class DateVH(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.tvDateHeader)
    }

    inner class WorkoutVH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.txtWorkoutName)
        val intensity: TextView = view.findViewById(R.id.txtWorkoutIntensity)
        val duration: TextView = view.findViewById(R.id.txtWorkoutDuration)
    }

    override fun getItemViewType(position: Int) = when (list[position]) {
        is WorkoutListItem.DateHeader -> TYPE_DATE
        is WorkoutListItem.WorkoutItem -> TYPE_WORKOUT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_DATE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_date_header, parent, false)
            DateVH(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_workout, parent, false)
            WorkoutVH(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = list[position]) {
            is WorkoutListItem.DateHeader -> {
                val vh = holder as DateVH
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                vh.dateText.text = sdf.format(Date(item.date))
            }
            is WorkoutListItem.WorkoutItem -> {
                val vh = holder as WorkoutVH
                val workout = item.workout
                vh.name.text = workout.name
                vh.intensity.text = if (workout.intensity.isNotEmpty()) workout.intensity else "-"
                val hours = workout.durationMinutes / 60
                val minutes = workout.durationMinutes % 60
                vh.duration.text = when {
                    hours > 0 && minutes > 0 -> "${hours} hr ${minutes} min"
                    hours > 0 -> "${hours} hr"
                    else -> "${minutes} min"
                }
            }
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<WorkoutListItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): WorkoutListItem? = list.getOrNull(position)
}
