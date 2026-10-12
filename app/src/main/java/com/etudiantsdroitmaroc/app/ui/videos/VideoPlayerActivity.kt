package com.etudiantsdroitmaroc.app.ui.videos

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.etudiantsdroitmaroc.app.databinding.ActivityVideoPlayerBinding
import com.etudiantsdroitmaroc.app.utils.BannerAdHelper

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding

    // تكبير الشاشة (fullscreen)
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val youtubeId = intent.getStringExtra("youtubeId") ?: ""

        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvVideoTitle.text = title
        binding.tvVideoDescription.text = description
        BannerAdHelper.attach(this, binding.bannerAdContainer)

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                enterFullscreen(view, callback)
            }

            override fun onHideCustomView() {
                exitFullscreen()
            }
        }

        // زر الرجوع كيخرج من تكبير الشاشة أولا
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    exitFullscreen()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        val html = """
            <html><body style="margin:0;padding:0;background:#000;">
            <iframe width="100%" height="100%"
                src="https://www.youtube.com/embed/$youtubeId?rel=0&fs=1&playsinline=1"
                frameborder="0" allow="autoplay; encrypted-media; fullscreen" allowfullscreen>
            </iframe>
            </body></html>
        """.trimIndent()

        binding.webView.loadDataWithBaseURL(
            "https://www.youtube.com", html, "text/html", "utf-8", null
        )
    }

    private fun enterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        customView = view
        customViewCallback = callback
        originalOrientation = requestedOrientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        (window.decorView as FrameLayout).addView(
            view,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        binding.root.visibility = View.GONE

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun exitFullscreen() {
        val view = customView ?: return
        (window.decorView as FrameLayout).removeView(view)
        customView = null
        binding.root.visibility = View.VISIBLE

        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        requestedOrientation = originalOrientation

        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    override fun onPause() {
        binding.webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onDestroy() {
        if (customView != null) exitFullscreen()
        binding.webView.destroy()
        super.onDestroy()
    }
}
