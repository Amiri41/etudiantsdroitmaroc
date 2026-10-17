package com.etudiantsdroitmaroc.app.utils

/** كيستخرج معرّف يوتيوب (11 حرف) من معرّف خام أو من أي رابط: watch, youtu.be, live, embed, shorts */
object YoutubeIds {
    private val RAW = Regex("^[A-Za-z0-9_-]{11}$")
    private val PATTERNS = listOf(
        Regex("[?&]v=([A-Za-z0-9_-]{11})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
        Regex("youtube(?:-nocookie)?\\.com/(?:embed|live|shorts|v)/([A-Za-z0-9_-]{11})")
    )

    fun extract(input: String): String? {
        val s = input.trim()
        if (RAW.matches(s)) return s
        for (p in PATTERNS) {
            p.find(s)?.let { return it.groupValues[1] }
        }
        return null
    }
}
