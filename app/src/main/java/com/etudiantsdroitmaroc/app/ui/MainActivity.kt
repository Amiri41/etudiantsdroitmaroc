package com.etudiantsdroitmaroc.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.etudiantsdroitmaroc.app.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        showLastCrashIfAny()
    }

    private fun showLastCrashIfAny() {
        val file = java.io.File(filesDir, "last_crash.txt")
        if (file.exists()) {
            val content = file.readText()
            file.delete()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("آخر انهيار ديال التطبيق")
                .setMessage(content.take(2000))
                .setPositiveButton("حسنا", null)
                .show()
        }
    }
}
