package com.ecotracker.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ecotracker.R

class LeaderboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val rvLeaderboard = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvLeaderboard)
        
        val dummyData = listOf(
            DummyUser(1, "EcoHero99", "Level 5", "3,500 XP"),
            DummyUser(2, "GreenQueen", "Level 4", "2,900 XP"),
            DummyUser(3, "PlanetSaver", "Level 4", "2,650 XP"),
            DummyUser(4, "TreeHugger", "Level 3", "1,800 XP"),
            DummyUser(5, "RecyclePro", "Level 3", "1,750 XP"),
            DummyUser(6, "EarthDefender", "Level 2", "900 XP"),
            DummyUser(7, "LeafBlower", "Level 2", "750 XP"),
            DummyUser(8, "SunPraiser", "Level 2", "600 XP"),
            DummyUser(9, "VeganViking", "Level 1", "200 XP"),
            DummyUser(10, "CompostKing", "Level 1", "50 XP")
        )
        
        rvLeaderboard.adapter = LeaderboardAdapter(dummyData)
    }
}
