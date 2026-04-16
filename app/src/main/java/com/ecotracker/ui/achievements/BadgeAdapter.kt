package com.ecotracker.ui.achievements

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ecotracker.R
import com.ecotracker.databinding.ItemBadgeBinding
import com.ecotracker.utils.Badge

class BadgeAdapter :
    ListAdapter<Badge, BadgeAdapter.ViewHolder>(DiffCallback) {

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
        holder.bind(getItem(position))
    }

    fun submitBadges(items: List<Badge>) {
        submitList(items)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Badge>() {
            override fun areItemsTheSame(oldItem: Badge, newItem: Badge): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Badge, newItem: Badge): Boolean =
                oldItem == newItem
        }
    }
}
