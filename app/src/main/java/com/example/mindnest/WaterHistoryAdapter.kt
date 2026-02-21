package com.example.mindnest.ui.water

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.databinding.ItemDateHeaderBinding
import com.example.mindnest.databinding.ItemWaterHistoryBinding

class WaterHistoryAdapter(
    private val items: MutableList<WaterListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_DATE = 0
    private val TYPE_WATER = 1

    override fun getItemViewType(position: Int) =
        if (items[position] is WaterListItem.DateHeader) TYPE_DATE else TYPE_WATER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_DATE) {
            val b = ItemDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            DateVH(b)
        } else {
            val b = ItemWaterHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            WaterVH(b)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is WaterListItem.DateHeader -> {
                val h = holder as DateVH
                h.binding.tvDateHeader.text =
                    if (item.achieved) "${item.date}  •  Hydration goal met"
                    else item.date
            }
            is WaterListItem.WaterLog -> {
                val h = holder as WaterVH
                h.binding.tvAmount.text = "Water: ${item.entry.consumedMl} ml"
            }
        }
    }

    override fun getItemCount() = items.size

    class DateVH(val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class WaterVH(val binding: ItemWaterHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}
