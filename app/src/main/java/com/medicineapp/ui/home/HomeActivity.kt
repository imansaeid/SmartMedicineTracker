package com.medicineapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.medicineapp.R
import com.medicineapp.data.network.SessionManager
import com.medicineapp.ui.auth.LoginActivity
import com.medicineapp.ui.medicines.MedicinesFragment
import com.medicineapp.ui.profile.ProfileFragment
import com.medicineapp.ui.scan.ScanActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        session = SessionManager(this)
        drawerLayout = findViewById(R.id.drawerLayout)

        val fabScan = findViewById<FloatingActionButton>(R.id.fabScan)
        fabScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        loadFragment(DashboardFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home      -> loadFragment(DashboardFragment())
                R.id.nav_medicines -> loadFragment(MedicinesFragment())
                R.id.nav_profile   -> loadFragment(ProfileFragment())
            }
            true
        }

        // Setup drawer
        val navView = findViewById<NavigationView>(R.id.navView)
        val header = navView.getHeaderView(0)
        header.findViewById<TextView>(R.id.tvDrawerName).text = session.getName() ?: "User"
        header.findViewById<TextView>(R.id.tvDrawerEmail).text = session.getEmail() ?: ""

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.drawer_home      -> { loadFragment(DashboardFragment()); bottomNav.selectedItemId = R.id.nav_home }
                R.id.drawer_medicines -> { loadFragment(MedicinesFragment()); bottomNav.selectedItemId = R.id.nav_medicines }
                R.id.drawer_profile   -> { loadFragment(ProfileFragment()); bottomNav.selectedItemId = R.id.nav_profile }
                R.id.drawer_logout    -> logout()
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.END)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun logout() {
        session.clearSession()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}