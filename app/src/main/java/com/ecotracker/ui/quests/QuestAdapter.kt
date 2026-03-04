package com.ecotracker.ui.quests

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecotracker.R

data class DummyQuest(
    val title: String,
    val description: String,
    val reward: String,
    val currentProgress: Int,
    val target: Int
)

class QuestAdapter(private val quests: List<DummyQuest>) :
    RecyclerView.Adapter<QuestAdapter.QuestViewHolder>() {

    class QuestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvQuestTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvQuestDesc)
        val tvReward: TextView = itemView.findViewById(R.id.tvQuestReward)
        val pbProgress: ProgressBar = itemView.findViewById(R.id.pbQuestProgress)
        val tvProgressText: TextView = itemView.findViewById(R.id.tvQuestProgressText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quest, parent, false)
        return QuestViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        val quest = quests[position]
        holder.tvTitle.text = quest.title
        holder.tvDesc.text = quest.description
        holder.tvReward.text = quest.reward
        
        holder.pbProgress.max = quest.target
        holder.pbProgress.progress = quest.currentProgress
        holder.tvProgressText.text = "${quest.currentProgress} / ${quest.target}"
    }

    override fun getItemCount() = quests.size
}
