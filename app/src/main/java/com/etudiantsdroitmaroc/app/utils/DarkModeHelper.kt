package com.etudiantsdroitmaroc.app.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** كنتحكمو فالوضع الليلي: نهار / ليل / حسب النظام (default) */
object DarkModeHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_MODE = "dark_mode_setting" // "light" | "dark" | "system"

    fun applySavedMode(context: Context) {
        val mode = getSavedMode(context)
        AppCompatDelegate.setDefaultNightMode(modeToConstant(mode))
    }

    fun getSavedMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODE, "system") ?: "system"
    }

    fun setMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(modeToConstant(mode))
    }

    private fun modeToConstant(mode: String): Int = when (mode) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
