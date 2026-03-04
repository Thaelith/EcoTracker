package com.ecotracker.ui.leaderboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecotracker.R

data class DummyUser(
    val rank: Int,
    val username: String,
    val levelText: String,
    val xpText: String
)

class LeaderboardAdapter(private val users: List<DummyUser>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        val tvUsername: TextView = itemView.findViewById(R.id.tvLeaderboardUsername)
        val tvLevel: TextView = itemView.findViewById(R.id.tvLeaderboardLevel)
        val tvXp: TextView = itemView.findViewById(R.id.tvLeaderboardXp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val user = users[position]
        holder.tvRank.text = "#${user.rank}"
        holder.tvUsername.text = user.username
        holder.tvLevel.text = user.levelText
        holder.tvXp.text = user.xpText
    }

    override fun getItemCount() = users.size
}
