package com.etudiantsdroitmaroc.app.utils

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import com.etudiantsdroitmaroc.app.R

/** جينغل بداية التطبيق: ما كيتشغلش فوضع الصمت/الاهتزاز، وكيتوقف من "حسابي" */
object IntroSound {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_ENABLED = "intro_sound_enabled"
    private var player: MediaPlayer? = null

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun play(context: Context) {
        try {
            if (!isEnabled(context)) return
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
            stop()
            val mp = MediaPlayer.create(context.applicationContext, R.raw.intro_jingle) ?: return
            mp.setOnCompletionListener { it.release(); if (player === it) player = null }
            mp.start()
            player = mp
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            stop()
        }
    }

    fun stop() {
        try { player?.release() } catch (_: Exception) {}
        player = null
    }
}
