package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.etudiantsdroitmaroc.app.data.model.AppAnnouncement
import com.etudiantsdroitmaroc.app.data.remote.AnnouncementRepository
import com.etudiantsdroitmaroc.app.ui.AnnouncementFullscreenActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * إعلانات الأدمن، بحال إعلانات AdMob الكبيرة وفنفس اللحظات الطبيعية (سياسة Google Play):
 * - إعلان promo: شاشة كاملة، كتبان غير ملي المستخدم يسد ملف PDF ولا فيديو ويرجع للائحة.
 *   ما كيبانش ملي يحل التطبيق، ولا قبل ما يتحل المحتوى، ولا بتوقيت أوتوماتيكي وسط القراءة.
 * - مرة وحدة لكل إعلان جديد، وإلا حدد الأدمن "تكرار": بين كل ظهور 15 دقيقة على الأقل (أو التكرار لكبر)،
 *   وأقصى 3 مرات فاليوم.
 * - إعلان update (تحديث): إشعار صغير (Snackbar) فالشاشة الرئيسية بزر "عرض"، ماشي إعلان بشاشة كاملة.
 * - الشاشة الكاملة فيها علامة "إعلان" وزر إغلاق واضح من الأول.
 */
object AnnouncementManager : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private const val PREFS = "app_prefs"
    private const val K_NOTICE_SEEN = "last_announcement_seen"   // الإشعار الصغير (update)
    private const val K_AD_SEEN = "ad_seen_updated_at"
    private const val K_AD_LAST_AT = "ad_last_shown_at"
    private const val K_AD_DAY = "ad_day"
    private const val K_AD_COUNT = "ad_count_today"
    private const val MIN_GAP_MIN = 15
    private const val MAX_PER_DAY = 3

    private val CONTENT = setOf("PdfViewerActivity", "VideoPlayerActivity")
    private val NO_AD_ON = setOf(
        "PdfViewerActivity", "VideoPlayerActivity", "AnnouncementFullscreenActivity",
        "SplashActivity", "LoginActivity"
    )

    private var currentActivity: Activity? = null
    private var pendingBreak = false

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    /** إشعار التحديث فقط (ماشي إعلان) */
    override fun onStart(owner: LifecycleOwner) {
        CoroutineScope(Dispatchers.Main).launch {
            val ann = AnnouncementRepository.getCurrentAnnouncement() ?: return@launch
            if (!ann.active || ann.type != "update") return@launch
            val activity = currentActivity ?: return@launch
            val prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
            if (ann.updatedAt <= prefs.getLong(K_NOTICE_SEEN, 0L)) return@launch
            if (showNotice(activity, ann)) {
                prefs.edit().putLong(K_NOTICE_SEEN, ann.updatedAt).apply()
            }
        }
    }

    private fun showNotice(activity: Activity, ann: AppAnnouncement): Boolean {
        if (activity.javaClass.simpleName != "MainActivity") return false
        val root: View = activity.findViewById(android.R.id.content) ?: return false
        val subject = ann.title.ifBlank { ann.message }.trim().take(60).ifBlank { "جديد" }
        Snackbar.make(root, "🚀 تحديث جديد: $subject", 10_000)
            .setAction("عرض") { openFullscreen(activity, ann) }
            .show()
        return true
    }

    /** لحظة طبيعية: رجع المستخدم من ملف/فيديو للشاشة اللي قبلها */
    private suspend fun maybeShowAd(activity: Activity) {
        val ann = AnnouncementRepository.getCurrentAnnouncement() ?: return
        if (!ann.active || ann.type == "update") return
        if (currentActivity !== activity || activity.isFinishing) return

        val prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val minGapMs = maxOf(MIN_GAP_MIN, ann.intervalMinutes) * 60_000L
        if (now - prefs.getLong(K_AD_LAST_AT, 0L) < minGapMs) return

        val day = now / 86_400_000L
        val count = if (prefs.getLong(K_AD_DAY, -1L) == day) prefs.getInt(K_AD_COUNT, 0) else 0
        if (count >= MAX_PER_DAY) return

        val isNew = ann.updatedAt > prefs.getLong(K_AD_SEEN, 0L)
        if (!isNew && ann.intervalMinutes <= 0) return   // مرة وحدة لكل إعلان

        prefs.edit()
            .putLong(K_AD_SEEN, ann.updatedAt)
            .putLong(K_AD_LAST_AT, now)
            .putLong(K_AD_DAY, day)
            .putInt(K_AD_COUNT, count + 1)
            .apply()
        openFullscreen(activity, ann)
    }

    private fun openFullscreen(activity: Activity, ann: AppAnnouncement) {
        val intent = Intent(activity, AnnouncementFullscreenActivity::class.java)
        intent.putExtra("type", ann.type)
        intent.putExtra("title", ann.title)
        intent.putExtra("message", ann.message)
        intent.putExtra("imageUrl", ann.imageUrl)
        intent.putExtra("linkUrl", ann.linkUrl)
        intent.putExtra("buttonText", ann.buttonText)
        intent.putExtra("videoUrl", ann.videoUrl)
        // الـ HTML ما كنمرروهوش فالـ Intent (حد الحجم)، الشاشة كتجيبو بوحدها
        intent.putExtra("hasHtml", ann.htmlContent.isNotBlank())
        activity.startActivity(intent)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        if (pendingBreak && activity.javaClass.simpleName !in NO_AD_ON) {
            pendingBreak = false
            CoroutineScope(Dispatchers.Main).launch { maybeShowAd(activity) }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // المستخدم سد ملف/فيديو (finish) => الشاشة اللي كترجع هي لحظة طبيعية
        if (activity.isFinishing && activity.javaClass.simpleName in CONTENT) pendingBreak = true
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
