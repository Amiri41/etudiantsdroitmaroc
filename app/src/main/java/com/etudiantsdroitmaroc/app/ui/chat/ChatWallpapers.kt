package com.etudiantsdroitmaroc.app.ui.chat

import com.etudiantsdroitmaroc.app.R

/** خلفيات الدردشة (نقوش دراسية/قانونية). الخلفية الأصلية (default) = لون الشاشة. */
object ChatWallpapers {

    data class Wallpaper(val key: String, val label: String, val drawableRes: Int, val dark: Boolean)

    val all = listOf(
        Wallpaper("default", "الأصلية", 0, false),
        Wallpaper("law", "القانون", R.drawable.wp_law, false),
        Wallpaper("study", "التفوق", R.drawable.wp_study, false),
        Wallpaper("library", "المكتبة", R.drawable.wp_library, false),
        Wallpaper("night", "ليلي", R.drawable.wp_night, true),
        Wallpaper("rose", "وردي", R.drawable.wp_rose, false),
        Wallpaper("science", "العلوم", R.drawable.wp_science, false),
        Wallpaper("zellij", "زليج", R.drawable.wp_zellij, false)
    )

    fun find(key: String?): Wallpaper? = all.firstOrNull { it.key == key }
}
