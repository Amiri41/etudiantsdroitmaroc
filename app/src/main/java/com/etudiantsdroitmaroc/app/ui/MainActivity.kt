package com.etudiantsdroitmaroc.app.ui

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.remote.AnnouncementRepository
import com.etudiantsdroitmaroc.app.databinding.DialogAnnouncementBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // كنعلمو PeriodicAdManager واش المستخدم فصفحة "البداية" (الهوم) باش يحبس الإعلان فيها
        navController.addOnDestinationChangedListener { _, destination, _ ->
            com.etudiantsdroitmaroc.app.utils.PeriodicAdManager
                .setOnHomeScreen(destination.id == R.id.homeFragment)
        }

        showLastCrashIfAny()
        checkAnnouncement()
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

    private fun checkAnnouncement() {
        lifecycleScope.launch {
            val announcement = AnnouncementRepository.getCurrentAnnouncement() ?: return@launch
            if (!announcement.active) return@launch

            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val lastShown = prefs.getLong("last_announcement_seen", 0L)
            if (announcement.updatedAt <= lastShown) return@launch

            val dialog = Dialog(this@MainActivity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val binding = DialogAnnouncementBinding.inflate(layoutInflater)
            dialog.setContentView(binding.root)
            dialog.setCancelable(true)

            binding.tvAnnouncementTitle.text = announcement.title
            binding.tvAnnouncementMessage.text = announcement.message

            if (announcement.imageUrl.isNotBlank()) {
                binding.ivAnnouncementImage.visibility = android.view.View.VISIBLE
                Glide.with(this@MainActivity).load(announcement.imageUrl).into(binding.ivAnnouncementImage)
            }

            if (announcement.linkUrl.isNotBlank()) {
                binding.btnAnnouncementAction.visibility = android.view.View.VISIBLE
                binding.btnAnnouncementAction.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(announcement.linkUrl)))
                }
            }

            binding.btnAnnouncementClose.setOnClickListener {
                prefs.edit().putLong("last_announcement_seen", announcement.updatedAt).apply()
                dialog.dismiss()
            }
            dialog.setOnDismissListener {
                prefs.edit().putLong("last_announcement_seen", announcement.updatedAt).apply()
            }

            dialog.show()
        }
    }
}
