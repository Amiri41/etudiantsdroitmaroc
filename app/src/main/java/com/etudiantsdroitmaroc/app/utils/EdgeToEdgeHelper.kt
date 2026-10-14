package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.app.Application
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
