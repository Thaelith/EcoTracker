package com.ecotracker.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.navigateUp
import android.content.Intent
import com.ecotracker.R
import com.ecotracker.databinding.ActivityMainBinding
import com.ecotracker.ui.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val topLevelDestinations = setOf(
            R.id.scanFragment,
            R.id.historyFragment,
            R.id.statisticsFragment,
            R.id.leaderboardFragment,
            R.id.questsFragment,
            R.id.profileFragment
        )

        appBarConfig = AppBarConfiguration(topLevelDestinations, binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfig)
        
        binding.navView.setupWithNavController(navController)
        
        // Handle custom drawer items
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> {
                    // Let Navigation UI handle other items like the Profile Fragment
                    androidx.navigation.ui.NavigationUI.onNavDestinationSelected(menuItem, navController)
                    binding.drawerLayout.close()
                    true
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
}
