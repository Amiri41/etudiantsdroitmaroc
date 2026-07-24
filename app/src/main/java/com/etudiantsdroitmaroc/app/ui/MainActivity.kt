package com.etudiantsdroitmaroc.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ماشي مشكل إلا رفض */ }

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
        requestNotificationPermissionIfNeeded()
        ensureFcmTokenSaved()
    }

    /** Android 13+ (API 33) كيطلب إذن صريح للإشعارات، وإلا ماغاديش تبان حتى لو وصلات من FCM */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * كنتأكدو التوكن ديال الإشعارات محفوظ فبروفايل المستخدم، فكل مرة يحل فيها التطبيق.
     * ضروري للمستخدمين اللي دخلين من قبل (قبل هاد الإصلاح) وماعندهمش fcmToken محفوظ - بلا هاد الخطوة
     * ماغاديش يتوصلو بالإشعارات حتى لو عاودو حلو التطبيق بزاف ديال المرات.
     */
    private fun ensureFcmTokenSaved() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        lifecycleScope.launch {
            try {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                Firebase.firestore.collection("users").document(uid)
                    .update("fcmToken", token).await()
            } catch (_: Exception) {
                // ما تكسرش التطبيق إلا فشل الحفظ، غادي يتعاود المحاولة المرة الجاية
            }
        }
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
