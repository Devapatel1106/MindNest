package com.example.mindnest.ui.workout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.R
import com.example.mindnest.model.Workout

class WorkoutAdapter(
    private val list: MutableList<Workout>
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutVH>() {

    inner class WorkoutVH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.txtWorkoutName)
        val intensity: TextView = view.findViewById(R.id.txtWorkoutIntensity)
        val duration: TextView = view.findViewById(R.id.txtWorkoutDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return WorkoutVH(view)
    }

    override fun onBindViewHolder(holder: WorkoutVH, position: Int) {
        val workout = list[position]
        holder.name.text = workout.name
        holder.intensity.text = if (workout.intensity.isNotEmpty()) workout.intensity else "-"

        val hours = workout.durationMinutes / 60
        val minutes = workout.durationMinutes % 60
        holder.duration.text = when {
            hours > 0 && minutes > 0 -> "${hours} hr ${minutes} min"
            hours > 0 -> "${hours} hr"
            else -> "${minutes} min"
        }
    }

    override fun getItemCount(): Int = list.size

    // Update list safely
    fun updateList(newList: List<Workout>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    // Helper to get workout at position safely
    fun getWorkoutAt(position: Int): Workout? = list.getOrNull(position)
}
