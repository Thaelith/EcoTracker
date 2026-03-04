package com.ecotracker.ui.quests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ecotracker.R

class QuestsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quests, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val rvQuests = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvQuests)
        
        val dummyData = listOf(
            DummyQuest("Daily Scanner", "Scan 5 different items today.", "+50 XP", 3, 5),
            DummyQuest("Eco-Warrior", "Scan an item with an 'A' or 'B' rating.", "+20 XP", 0, 1),
            emptyQuest(),
            DummyQuest("First Scan", "Scan your very first product ever!", "+10 XP", 1, 1),
            DummyQuest("Contributer", "Enter product data manually.", "+30 XP", 0, 1)
        )
        
        rvQuests.adapter = QuestAdapter(dummyData)
    }
    
    private fun emptyQuest() = DummyQuest("Weekly Scanner", "Scan 20 items this week.", "+150 XP", 12, 20)
}
