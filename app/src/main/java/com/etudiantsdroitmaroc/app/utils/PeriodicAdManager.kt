package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * كيبين إعلان (Interstitial) كل 8 دقايق مادام المستخدم خدام فالتطبيق بصفة نشيطة.
 * ملاحظات:
 * - ما كيبانش الإعلان مباشرة ملي التطبيق يبدا - غير كيبدا العد بعد أول 8 دقايق من الاستعمال
 * - إلا خرج المستخدم من التطبيق (خلفية)، العد كيتوقف، وكيبدا من جديد ملي يرجع
 */
object PeriodicAdManager : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private const val INTERVAL_MS = 8 * 60 * 1000L // 8 دقايق

    private val handler = Handler(Looper.getMainLooper())
    private var timerRunning = false
    private var currentActivity: Activity? = null

    private val showAdRunnable = Runnable {
        val activity = currentActivity
        if (activity != null && !activity.isFinishing) {
            AdManager.showInterstitialForced(activity) {
                scheduleNext()
            }
        } else {
            scheduleNext()
        }
    }

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        scheduleNext()
    }

    override fun onStop(owner: LifecycleOwner) {
        timerRunning = false
        handler.removeCallbacks(showAdRunnable)
    }

    private fun scheduleNext() {
        if (timerRunning) handler.removeCallbacks(showAdRunnable)
        timerRunning = true
        handler.postDelayed(showAdRunnable, INTERVAL_MS)
    }

    // ---------- تتبع الـ Activity الحالية باش نقدرو نبينو الإعلان فوقها ----------

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
