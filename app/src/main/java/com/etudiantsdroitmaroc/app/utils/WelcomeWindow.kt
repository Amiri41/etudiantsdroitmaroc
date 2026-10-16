package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * شاشات الترحيب (الدخول / مرحباً بعودتك) كتمتد تحت أشرطة النظام:
 * الأشرطة شفافة، وأيقونات شريط الحالة كتتبدل حسب الوضع (فاتح/داكن).
 * الشريط السفلي كحلي (الفوتر) فأيقوناتو ديما فاتحة.
 */
fun Activity.setupWelcomeWindow() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    @Suppress("DEPRECATION")
    window.statusBarColor = Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.navigationBarColor = Color.TRANSPARENT
    val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = !night
        isAppearanceLightNavigationBars = false
    }
}
