package com.etudiantsdroitmaroc.app.utils

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * كيتبع حالة "متصل الآن" بدقة أكبر:
 * - كيحدد isOnline=true + lastSeen ملي التطبيق يولي فواجهة المستخدم (foreground)، مع نبضة (heartbeat)
 *   كل 30 ثانية باش lastSeen يبقى محدث
 * - كيحدد isOnline=false ملي التطبيق يمشي للخلفية (background) أو يتسد
 * - كنعتمدو ProcessLifecycleOwner باش الحالة توصف التطبيق كاملو (ماشي كل Activity بوحدها)،
 *   وهكا التنقل بين الصفحات جوا التطبيق ما كيديش يبان "غير متصل" غلط
 */
object PresenceManager : DefaultLifecycleObserver {

    private const val HEARTBEAT_MS = 30_000L
    private val handler = Handler(Looper.getMainLooper())
    private var heartbeatRunnable: Runnable? = null

    override fun onStart(owner: LifecycleOwner) {
        setOnline(true)
        startHeartbeat()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopHeartbeat()
        setOnline(false)
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        val runnable = object : Runnable {
            override fun run() {
                updateLastSeen()
                handler.postDelayed(this, HEARTBEAT_MS)
            }
        }
        heartbeatRunnable = runnable
        handler.postDelayed(runnable, HEARTBEAT_MS)
    }

    private fun stopHeartbeat() {
        heartbeatRunnable?.let { handler.removeCallbacks(it) }
        heartbeatRunnable = null
    }

    private fun setOnline(online: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "isOnline" to online,
                    "lastSeen" to System.currentTimeMillis()
                )
            )
    }

    private fun updateLastSeen() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .update("lastSeen", System.currentTimeMillis())
    }
}
