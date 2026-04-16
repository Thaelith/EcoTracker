package com.ecotracker.ui.leaderboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ecotracker.R
import com.ecotracker.data.model.LeaderboardUser
import com.ecotracker.databinding.ItemLeaderboardBinding
import com.ecotracker.utils.GamificationEngine
import com.google.firebase.auth.FirebaseAuth

class LeaderboardAdapter : ListAdapter<LeaderboardUser, LeaderboardAdapter.LeaderboardViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LeaderboardViewHolder(private val binding: ItemLeaderboardBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(user: LeaderboardUser) {
            val context = binding.root.context
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            binding.tvRank.text = user.rank.toString()
            binding.tvUsername.text = user.username
            binding.tvScanCount.text = context.getString(R.string.products_scanned_format, user.scanCount)

            // Nature Rank Title from GamificationEngine
            val rankInfo = GamificationEngine.calculateRank(user.scanCount)
            binding.tvRankTitle.text = context.getString(rankInfo.nameResId)

            // Styling for current user
            if (user.uid == currentUserId) {
                binding.cardUser.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.active_user_stroke_width)
                binding.cardUser.setStrokeColor(ContextCompat.getColorStateList(context, R.color.md_theme_primary))
                binding.tvCurrentUser.visibility = View.VISIBLE
            } else {
                binding.cardUser.strokeWidth = 1
                binding.cardUser.setStrokeColor(ContextCompat.getColorStateList(context, R.color.md_theme_surface_variant))
                binding.tvCurrentUser.visibility = View.GONE
            }

            // Top 3 Highlights
            when (user.rank) {
                1 -> {
                    binding.rankBackground.setBackgroundResource(R.drawable.bg_rank_number)
                    binding.rankBackground.backgroundTintList = ContextCompat.getColorStateList(context, R.color.rank_first)
                    binding.ivTrophy.apply {
                        visibility = View.VISIBLE
                        imageTintList = ContextCompat.getColorStateList(context, R.color.rank_first)
                    }
                }
                2 -> {
                    binding.rankBackground.setBackgroundResource(R.drawable.bg_rank_number)
                    binding.rankBackground.backgroundTintList = ContextCompat.getColorStateList(context, R.color.rank_second)
                    binding.ivTrophy.apply {
                        visibility = View.VISIBLE
                        imageTintList = ContextCompat.getColorStateList(context, R.color.rank_second)
                    }
                }
                3 -> {
                    binding.rankBackground.setBackgroundResource(R.drawable.bg_rank_number)
                    binding.rankBackground.backgroundTintList = ContextCompat.getColorStateList(context, R.color.rank_third)
                    binding.ivTrophy.apply {
                        visibility = View.VISIBLE
                        imageTintList = ContextCompat.getColorStateList(context, R.color.rank_third)
                    }
                }
                else -> {
                    binding.rankBackground.backgroundTintList = ContextCompat.getColorStateList(context, R.color.md_theme_surface_variant)
                    binding.ivTrophy.visibility = View.GONE
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LeaderboardUser>() {
        override fun areItemsTheSame(oldItem: LeaderboardUser, newItem: LeaderboardUser): Boolean = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: LeaderboardUser, newItem: LeaderboardUser): Boolean = oldItem == newItem
    }
}
