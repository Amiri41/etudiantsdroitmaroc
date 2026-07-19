package com.etudiantsdroitmaroc.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // كنعلمو PeriodicAdManager واش المستخدم فصفحة "البداية" (الهوم) باش يحبس الإعلان فيها
        navController.addOnDestinationChangedListener { _, destination, _ ->
            com.etudiantsdroitmaroc.app.utils.PeriodicAdManager
                .setOnHomeScreen(destination.id == R.id.homeFragment)
        }

        showLastCrashIfAny()
    }

    override fun onResume() {
        super.onResume()
        updateUnreadBadge()
    }

    private fun updateUnreadBadge() {
        lifecycleScope.launch {
            try {
                val count = ChatRepository().getTotalUnreadCount()
                val badge = bottomNav.getOrCreateBadge(R.id.chatListFragment)
                if (count > 0) {
                    badge.isVisible = true
                    badge.number = count
                } else {
                    badge.isVisible = false
                }
            } catch (_: Exception) { }
        }
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
