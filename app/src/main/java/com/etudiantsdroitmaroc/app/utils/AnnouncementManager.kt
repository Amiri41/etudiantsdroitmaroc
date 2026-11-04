package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.etudiantsdroitmaroc.app.data.remote.AnnouncementRepository
import com.etudiantsdroitmaroc.app.ui.AnnouncementFullscreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * كيتحكم فظهور البانر الإعلاني بشاشة كاملة:
 * - intervalMinutes = 0 فـ Firestore → كيبان مرة وحدة بس (لكل محتوى جديد كيبدلو الأدمين)
 * - intervalMinutes > 0 → كيتكرر كل X دقيقة مادام التطبيق نشيط (بحال PeriodicAdManager)
 */
object AnnouncementManager : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private val handler = Handler(Looper.getMainLooper())
    private var currentActivity: Activity? = null
    private var targetElapsedTime = 0L
    private var isScheduled = false

    private val fireRunnable = Runnable { showNowAndReschedule() }

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        CoroutineScope(Dispatchers.Main).launch { checkAndSchedule() }
    }

    override fun onStop(owner: LifecycleOwner) {
        isScheduled = false
        handler.removeCallbacks(fireRunnable)
    }

    private suspend fun checkAndSchedule() {
        val ann = AnnouncementRepository.getCurrentAnnouncement() ?: return
        if (!ann.active) return

        if (ann.intervalMinutes <= 0) {
            val activity = currentActivity ?: return
            val prefs = activity.getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
            val lastShown = prefs.getLong("last_announcement_seen", 0L)
            if (ann.updatedAt <= lastShown) return
            launchFullscreen(ann)
            prefs.edit().putLong("last_announcement_seen", ann.updatedAt).apply()
        } else {
            targetElapsedTime = SystemClock.elapsedRealtime() + (ann.intervalMinutes * 60_000L)
            scheduleTimer()
        }
    }

    private fun scheduleTimer() {
        if (isScheduled) return
        isScheduled = true
        val delay = (targetElapsedTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        handler.postDelayed(fireRunnable, delay)
    }

    private fun showNowAndReschedule() {
        isScheduled = false
        CoroutineScope(Dispatchers.Main).launch {
            val ann = AnnouncementRepository.getCurrentAnnouncement()
            if (ann != null && ann.active && ann.intervalMinutes > 0) {
                launchFullscreen(ann)
                targetElapsedTime = SystemClock.elapsedRealtime() + (ann.intervalMinutes * 60_000L)
                scheduleTimer()
            }
        }
    }

    private fun launchFullscreen(ann: com.etudiantsdroitmaroc.app.data.model.AppAnnouncement) {
        val activity = currentActivity ?: return
        val intent = Intent(activity, AnnouncementFullscreenActivity::class.java)
        intent.putExtra("type", ann.type)
        intent.putExtra("title", ann.title)
        intent.putExtra("message", ann.message)
        intent.putExtra("imageUrl", ann.imageUrl)
        intent.putExtra("linkUrl", ann.linkUrl)
        intent.putExtra("buttonText", ann.buttonText)
        // الـ HTML ما كنمرروهوش فالـ Intent (حد الحجم)، الشاشة كتجيبو بوحدها
        intent.putExtra("hasHtml", ann.htmlContent.isNotBlank())
        activity.startActivity(intent)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
