package com.example.mindnest.ui.periodtracker

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.databinding.ItemCycleCardBinding
import java.text.SimpleDateFormat
import java.util.*

class UpcomingCyclesAdapter(
    private var upcomingDates: List<java.time.LocalDate>
) : RecyclerView.Adapter<UpcomingCyclesAdapter.CycleViewHolder>() {

    inner class CycleViewHolder(val binding: ItemCycleCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CycleViewHolder {
        val binding = ItemCycleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CycleViewHolder(binding)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: CycleViewHolder, position: Int) {
        val date = upcomingDates[position]
        val cal = Calendar.getInstance()
        cal.set(date.year, date.monthValue - 1, date.dayOfMonth)

        val dayFormatter = SimpleDateFormat("dd")
        val monthYearFormatter = SimpleDateFormat("MMM yyyy")

        holder.binding.tvDate.text = dayFormatter.format(cal.time)
        holder.binding.tvMonth.text = monthYearFormatter.format(cal.time)
        holder.binding.tvLabel.text = "Expected"
    }

    override fun getItemCount(): Int = upcomingDates.size

    fun updateData(newDates: List<java.time.LocalDate>) {
        upcomingDates = newDates
        notifyDataSetChanged()
    }
}
