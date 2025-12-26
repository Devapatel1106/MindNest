package com.example.mindnest

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.databinding.ItemLogSleepBinding
import java.text.SimpleDateFormat
import java.util.Locale

class LogSleepAdapter(
    private val sleepList: List<LogSleep>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<LogSleepAdapter.SleepViewHolder>() {

    inner class SleepViewHolder(
        private val binding: ItemLogSleepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(logSleep: LogSleep) {
            binding.txtSleepDate.text = logSleep.date
            binding.txtSleepTime.text =
                "${logSleep.sleepTime} - ${logSleep.wakeTime}"

            binding.txtSleepDuration.text =
                calculateDuration(logSleep.sleepTime, logSleep.wakeTime)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SleepViewHolder {
        val binding = ItemLogSleepBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SleepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SleepViewHolder, position: Int) {
        holder.bind(sleepList[position])
    }

    override fun getItemCount(): Int = sleepList.size

    // ✅ 12-hour duration calculation (handles overnight sleep)
    private fun calculateDuration(start: String, end: String): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val startTime = sdf.parse(start)!!
        val endTime = sdf.parse(end)!!

        var diff = endTime.time - startTime.time
        if (diff < 0) diff += 24 * 60 * 60 * 1000 // overnight sleep

        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60

        return "${hours}h ${minutes}m"
    }
}
