package com.ecotracker.ui.achievements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecotracker.R
import com.ecotracker.databinding.ItemBadgeBinding
import com.ecotracker.utils.Badge

class BadgeAdapter(private val badges: List<Badge>) :
    RecyclerView.Adapter<BadgeAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ItemBadgeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(badge: Badge) {
            val context = binding.root.context
            binding.apply {
                tvBadgeName.text = context.getString(badge.nameResId)
                tvBadgeDesc.text = context.getString(badge.descResId)
                
                if (badge.isUnlocked) {
                    ivBadgeIcon.alpha = 1.0f
                    ivBadgeIcon.setTag(R.id.ivBadgeIcon, true) // For color filtering if needed
                    ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                        root.context.getColor(R.color.eco_green)
                    )
                } else {
                    ivBadgeIcon.alpha = 0.3f
                    ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                        root.context.getColor(R.color.on_surface_variant)
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBadgeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(badges[position])
    }

    override fun getItemCount(): Int = badges.size
}
