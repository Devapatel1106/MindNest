package com.example.mindnest.ui.period

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.R

class CalendarAdapter(
    private var days: List<Int?>,
    private var periodDays: List<Int>,
    private var fertileDays: List<Int>,
    private var ovulationDay: Int?,
    private val onDaySelected: (Int) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position], position)
    }

    override fun getItemCount(): Int = days.size

    // 🔄 Used when month changes
    fun updateDays(
        newDays: List<Int?>,
        newPeriodDays: List<Int>,
        newFertileDays: List<Int>,
        newOvulationDay: Int?
    ) {
        days = newDays
        periodDays = newPeriodDays
        fertileDays = newFertileDays
        ovulationDay = newOvulationDay
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)

        fun bind(day: Int?, position: Int) {

            // Empty cell
            if (day == null) {
                tvDay.text = ""
                tvDay.background = null
                tvDay.isClickable = false
                return
            }

            tvDay.text = day.toString()
            tvDay.isClickable = true

            // ---------------- BACKGROUND LOGIC ----------------

            val context = itemView.context

            tvDay.background = when {
                position == selectedPosition ->
                    ContextCompat.getDrawable(context, R.drawable.bg_selected_day)

                day == ovulationDay ->
                    ContextCompat.getDrawable(context, R.drawable.bg_ovulation)

                day in periodDays ->
                    ContextCompat.getDrawable(context, R.drawable.bg_period)

                day in fertileDays ->
                    ContextCompat.getDrawable(context, R.drawable.bg_fertile)

                else -> null
            }

            // ---------------- CLICK ----------------

            itemView.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = adapterPosition

                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)

                onDaySelected(day)
            }
        }
    }
}
