package com.etudiantsdroitmaroc.app.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * حالة "متصل الآن" و"آخر ظهور":
 * - كتتخزن فـ presence/{uid} (ماشي فوثيقة المستعمل العامة). القواعد كتخلي غير صاحبها وأصدقاءو يقراوها.
 * - المستعمل يقدر يخبيها من "حسابي": كتولي isOnline=false وlastSeen=0 وما كتتحدثش.
 * - ProcessLifecycleOwner: الحالة كتوصف التطبيق كامل، مع نبضة كل 30 ثانية.
 */
object PresenceManager : DefaultLifecycleObserver {

    private const val HEARTBEAT_MS = 30_000L
    private const val PREFS = "app_prefs"
    private const val KEY_VISIBLE = "show_presence"
    private const val KEY_MIGRATED = "presence_migrated_v1"

    private val handler = Handler(Looper.getMainLooper())
    private var heartbeatRunnable: Runnable? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isVisible(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_VISIBLE, true)

    /** كيتنادى من "حسابي" */
    fun setVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VISIBLE, visible).apply()
        if (visible) {
            setOnline(true)
            startHeartbeat()
        } else {
            stopHeartbeat()
            hideNow()
        }
    }

    private fun visibleNow(): Boolean = appContext?.let { isVisible(it) } ?: true

    override fun onStart(owner: LifecycleOwner) {
        migrateOldFieldsOnce()
        if (visibleNow()) {
            setOnline(true)
            startHeartbeat()
        } else {
            hideNow()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        stopHeartbeat()
        if (visibleNow()) setOnline(false)
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        val runnable = object : Runnable {
            override fun run() {
                if (visibleNow()) writePresence(mapOf("lastSeen" to System.currentTimeMillis()))
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
        writePresence(mapOf("isOnline" to online, "lastSeen" to System.currentTimeMillis()))
    }

    private fun hideNow() {
        writePresence(mapOf("isOnline" to false, "lastSeen" to 0L))
    }

    private fun writePresence(data: Map<String, Any>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("presence").document(uid).set(data, SetOptions.merge())
    }

    /** مرة وحدة: نمسحو القيم القديمة اللي كانت مكشوفة فوثيقة المستعمل العامة */
    private fun migrateOldFieldsOnce() {
        val ctx = appContext ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        Firebase.firestore.collection("users").document(uid)
            .update(mapOf("isOnline" to false, "lastSeen" to 0L))
            .addOnSuccessListener { prefs.edit().putBoolean(KEY_MIGRATED, true).apply() }
    }
}
