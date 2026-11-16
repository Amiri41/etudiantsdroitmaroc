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
 * إعلانات الأدمن داخل التطبيق، حسب سياسة Google Play (الإعلانات المزعجة):
 * - ما كيبانش أبدا إعلان بشاشة كاملة بوحدو (لا ملي يحل التطبيق، لا وسط الاستعمال، لا بالتكرار).
 * - كيبان إشعار صغير (Snackbar) مكتوب فيه "إعلان" أو "تحديث جديد" مع زر "عرض".
 * - الشاشة الكاملة كتحل غير ملي المستخدم يضغط على "عرض" (وفيها زر إغلاق واضح من الأول).
 * - كيبان مرة وحدة لكل محتوى جديد.
 */
object AnnouncementManager : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        CoroutineScope(Dispatchers.Main).launch { checkAndNotify() }
    }

    private suspend fun checkAndNotify() {
        val ann = AnnouncementRepository.getCurrentAnnouncement() ?: return
        if (!ann.active) return
        val activity = currentActivity ?: return
        val prefs = activity.getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
        val lastShown = prefs.getLong("last_announcement_seen", 0L)
        if (ann.updatedAt <= lastShown) return
        if (showNotice(activity, ann)) {
            prefs.edit().putLong("last_announcement_seen", ann.updatedAt).apply()
        }
    }

    /** كيرجع true إلا بانت الإشعار فعلا */
    private fun showNotice(activity: Activity, ann: AppAnnouncement): Boolean {
        // كنبينوها غير فالشاشة الرئيسية ديال التطبيق، ماشي وسط شاشة أخرى
        if (activity.javaClass.simpleName != "MainActivity") return false
        val root: View = activity.findViewById(android.R.id.content) ?: return false
        val label = if (ann.type == "update") "🚀 تحديث جديد" else "إعلان"
        val subject = ann.title.ifBlank { ann.message }.trim().take(60).ifBlank { "جديد" }
        Snackbar.make(root, "$label: $subject", 10_000)
            .setAction("عرض") { openFullscreen(activity, ann) }
            .show()
        return true
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
