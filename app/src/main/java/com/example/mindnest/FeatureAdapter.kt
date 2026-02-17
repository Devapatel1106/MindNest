package com.example.mindnest.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mindnest.R
import com.example.mindnest.model.Feature

class FeatureAdapter(
    private val features: List<Feature>,
    private val onItemClick: (Feature) -> Unit
) : RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {

    inner class FeatureViewHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_feature_card,
                parent,
                false
            )
        ) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvStat: TextView = itemView.findViewById(R.id.tvStat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FeatureViewHolder(parent)

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val feature = features[position]
        holder.imgIcon.setImageResource(feature.iconRes)
        holder.tvTitle.text = feature.title
        holder.tvDescription.text = feature.description
        holder.tvStat.text = feature.stat
        holder.itemView.setOnClickListener { onItemClick(feature) }
    }

    override fun getItemCount(): Int = features.size
}
