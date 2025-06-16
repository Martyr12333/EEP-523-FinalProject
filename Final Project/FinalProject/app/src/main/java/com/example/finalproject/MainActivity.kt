package com.example.finalproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.finalproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Set up Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Set up the top-level destinations
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_dashboard,
                R.id.nav_history,
                R.id.nav_settings
            )
        )
        
        // Set up the ActionBar with the NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Set up the bottom navigation with the NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle navigation item selection manually
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.nav_home)
                    true
                }
                R.id.nav_dashboard -> {
                    navController.navigate(R.id.nav_dashboard)
                    true
                }
                R.id.nav_history -> {
                    navController.navigate(R.id.nav_history)
                    true
                }
                R.id.nav_settings -> {
                    navController.navigate(R.id.nav_settings)
                    true
                }
                else -> false
            }
        }
    }
}