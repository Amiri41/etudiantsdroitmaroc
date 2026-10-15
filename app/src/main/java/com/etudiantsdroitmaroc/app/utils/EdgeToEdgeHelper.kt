package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.etudiantsdroitmaroc.app.R

/**
 * Android 15+ كيفرض edge-to-edge مع targetSdk 35+. هنا كنخلو المحتوى بعيد على شريط الحالة
 * وشريط التنقل (وعلى الكيبورد)، والمنطقة وراهم كحلية بحال الهيدر.
 */
object EdgeToEdgeHelper : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is AppCompatActivity) return
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        if (activity.javaClass.simpleName == "LoginActivity") {
            // صفحة الدخول كتتكلف بالمسافات بوحدها، وفيها وضع فاتح بأيقونات داكنة
            val night = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val ctrl = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            ctrl.isAppearanceLightStatusBars = !night
            ctrl.isAppearanceLightNavigationBars = false
            return
        }
        // الشعار كيستعمل خلفية خاصة بيه
        if (activity.javaClass.simpleName != "SplashActivity") {
            activity.window.setBackgroundDrawable(
                ColorDrawable(ContextCompat.getColor(activity, R.color.navy_dark))
            )
        }
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity !is AppCompatActivity) return
        if (activity.javaClass.simpleName == "LoginActivity") return
        val content = activity.findViewById<View>(android.R.id.content) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(content)
    }

    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
