package com.etudiantsdroitmaroc.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.ui.chat.ChatListFragment
import com.etudiantsdroitmaroc.app.ui.forum.ForumFragment
import com.etudiantsdroitmaroc.app.ui.friends.FriendsFragment
import com.etudiantsdroitmaroc.app.ui.home.HomeFragment
import com.etudiantsdroitmaroc.app.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var currentTabId = R.id.homeFragment

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ماشي مشكل إلا رفض */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        val offlineBanner = findViewById<android.view.View>(R.id.tvOfflineBanner)
        com.etudiantsdroitmaroc.app.utils.NetworkStatus.online.observe(this) { on ->
            offlineBanner.visibility = if (on) android.view.View.GONE else android.view.View.VISIBLE
        }
        currentTabId = savedInstanceState?.getInt(KEY_TAB, R.id.homeFragment) ?: R.id.homeFragment

        // أول مرة: نزيدو الصفحة الرئيسية. فالمرات الأخرى النظام كيرجع الصفحات اللي كانت محفوظة.
        if (savedInstanceState == null) switchToTab(R.id.homeFragment)

        bottomNav.selectedItemId = currentTabId
        bottomNav.setOnItemSelectedListener { item ->
            switchToTab(item.itemId)
            true
        }
        bottomNav.setOnItemReselectedListener { /* ما كنعاودو والو */ }

        com.etudiantsdroitmaroc.app.utils.PeriodicAdManager
            .setOnHomeScreen(currentTabId == R.id.homeFragment)

        requestNotificationPermissionIfNeeded()
        ensureFcmTokenSaved()
        checkNotBanned()
        cleanOldEmailField()
    }

    /** مرة وحدة: نمسحو الإيميل القديم من الوثيقة العامة ديال المستعمل */
    private fun cleanOldEmailField() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("email_cleaned", false)) return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(user.uid)
            .update("email", com.google.firebase.firestore.FieldValue.delete())
            .addOnSuccessListener { prefs.edit().putBoolean("email_cleaned", true).apply() }
    }

    /** إلا الأدمين وقف الحساب: كنخرجو المستعمل ونرجعوه لصفحة الدخول */
    private fun checkNotBanned() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(user.uid)
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                if (doc.getBoolean("banned") == true) {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    android.widget.Toast.makeText(
                        this, "تم إيقاف حسابك بسبب مخالفة شروط الاستخدام.", android.widget.Toast.LENGTH_LONG
                    ).show()
                    startActivity(
                        android.content.Intent(this, com.etudiantsdroitmaroc.app.ui.auth.LoginActivity::class.java)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
            }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getStringExtra("tab") == "forum") bottomNav.selectedItemId = R.id.forumFragment
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB, currentTabId)
    }

    /**
     * التنقل بين التابات: كنخليو كل صفحة حية (إخفاء/إظهار) بدل ما نهدمها ونعاود نبنيوها،
     * هكا التنقل فوري، والمحتوى كيبقى باين، وما كاين حتى إعادة تحميل غير ضرورية.
     */
    private fun switchToTab(id: Int) {
        val fm = supportFragmentManager
        val target = fm.findFragmentByTag(tagFor(id))
        val current = fm.findFragmentByTag(tagFor(currentTabId))
        if (target != null && target === current && !target.isHidden) return

        fm.beginTransaction().apply {
            setReorderingAllowed(true)
            if (current != null && current !== target) {
                hide(current)
                setMaxLifecycle(current, Lifecycle.State.STARTED)
            }
            if (target == null) {
                add(R.id.nav_host_fragment, createTab(id), tagFor(id))
            } else {
                show(target)
                setMaxLifecycle(target, Lifecycle.State.RESUMED)
            }
        }.commit()

        currentTabId = id
        com.etudiantsdroitmaroc.app.utils.PeriodicAdManager.setOnHomeScreen(id == R.id.homeFragment)
    }

    private fun tagFor(id: Int) = "tab_$id"

    private fun createTab(id: Int): Fragment = when (id) {
        R.id.forumFragment -> ForumFragment()
        R.id.friendsFragment -> FriendsFragment()
        R.id.chatListFragment -> ChatListFragment()
        R.id.profileFragment -> ProfileFragment()
        else -> HomeFragment()
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

    /** كنتأكدو التوكن ديال الإشعارات محفوظ فبروفايل المستخدم، فكل مرة يحل فيها التطبيق. */
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

    companion object {
        private const val KEY_TAB = "current_tab"
    }
}
