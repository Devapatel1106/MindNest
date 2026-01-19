package com.example.mindnest

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.databinding.ItemLogSleepBinding

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
            binding.txtSleepDuration.text = logSleep.duration

            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SleepViewHolder {
        val binding = ItemLogSleepBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SleepViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SleepViewHolder,
        position: Int
    ) {
        holder.bind(sleepList[position])
    }

    override fun getItemCount(): Int = sleepList.size
}
