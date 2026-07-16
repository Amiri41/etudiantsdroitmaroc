package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * كيبين إعلان (Interstitial) كل 8 دقايق مادام المستخدم خدام فالتطبيق بصفة نشيطة.
 *
 * - ما كيبانش الإعلان مباشرة ملي التطبيق يبدا - العد كيبدا فأول 8 دقايق من الاستعمال.
 * - إلا خرج المستخدم للخلفية أو سد التطبيق، **العد كيتوقف وكيكمل** ملي يرجع (ماشي كيبدا من جديد).
 * - إلا العد سالا (توصل للصفر) ومازال المستخدم فصفحة "البداية" (الهوم)، الإعلان كيتحبس
 *   وكيبان **مباشرة** ملي المستخدم يخرج من صفحة البداية لأي صفحة أخرى.
 */
object PeriodicAdManager : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private const val INTERVAL_MS = 8 * 60 * 1000L // 8 دقايق

    private val handler = Handler(Looper.getMainLooper())
    private var currentActivity: Activity? = null

    // وقت مطلق (elapsedRealtime) اللي فيه خاص الإعلان يبان - ما كيتبدلش غير بعد ما يبان إعلان
    private var targetElapsedTime: Long = 0L
    private var isTimerScheduled = false

    private var isOnHomeScreen = false
    private var adPendingShow = false

    private val fireRunnable = Runnable { onTimerFired() }

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        targetElapsedTime = SystemClock.elapsedRealtime() + INTERVAL_MS
    }

    /** خاصها تتنادى من MainActivity ملي يتبدل الوجهة (Fragment) الحالية */
    fun setOnHomeScreen(onHome: Boolean) {
        val wasOnHome = isOnHomeScreen
        isOnHomeScreen = onHome

        // خرج من صفحة البداية وكاين إعلان محبوس فالانتظار → يبان مباشرة
        if (wasOnHome && !onHome && adPendingShow) {
            adPendingShow = false
            tryShowNow()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        scheduleTimer()
    }

    override fun onStop(owner: LifecycleOwner) {
        isTimerScheduled = false
        handler.removeCallbacks(fireRunnable)
    }

    private fun scheduleTimer() {
        if (isTimerScheduled) return
        isTimerScheduled = true
        val delay = (targetElapsedTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        handler.postDelayed(fireRunnable, delay)
    }

    private fun onTimerFired() {
        isTimerScheduled = false
        if (isOnHomeScreen) {
            // نحبسو الإعلان حتى يخرج المستخدم من صفحة البداية
            adPendingShow = true
        } else {
            tryShowNow()
        }
    }

    private fun tryShowNow() {
        val activity = currentActivity
        if (activity != null && !activity.isFinishing) {
            AdManager.showInterstitialForced(activity) {
                targetElapsedTime = SystemClock.elapsedRealtime() + INTERVAL_MS
                scheduleTimer()
            }
        } else {
            // ماكاينش Activity ظاهرة دابا، نعاودو نجربو بعد قليل
            targetElapsedTime = SystemClock.elapsedRealtime() + 5_000L
            scheduleTimer()
        }
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
