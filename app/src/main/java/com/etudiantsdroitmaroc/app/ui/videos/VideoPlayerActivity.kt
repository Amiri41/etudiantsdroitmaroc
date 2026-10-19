package com.etudiantsdroitmaroc.app.ui.videos

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.databinding.ActivityVideoPlayerBinding
import com.etudiantsdroitmaroc.app.utils.NetworkStatus
import com.etudiantsdroitmaroc.app.utils.YoutubeIds

/**
 * مشغّل يوتيوب داخل التطبيق (IFrame Player API):
 * - كيقبل معرّف أو أي رابط (watch / youtu.be / live / embed / shorts)، يعني حتى روابط البث المباشر.
 * - إلا فشل التضمين (صاحب الفيديو منع التشغيل خارج يوتيوب...) كنعاودو مرة وحدة بنطاق youtube-nocookie،
 *   وإلا بقا الخطأ كتبان رسالة وزر "افتح فـ YouTube".
 * - تكبير الشاشة (fullscreen) مدعوم.
 */
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var videoId = ""
    private var retriedNoCookie = false
    private var started = false
    private val handler = Handler(Looper.getMainLooper())
    private val watchdog = Runnable { onWatchdogFired() }

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
        val raw = intent.getStringExtra("youtubeId") ?: ""
        videoId = YoutubeIds.extract(raw) ?: raw.trim()

        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvVideoTitle.text = title
        binding.tvVideoDescription.text = description
        binding.btnOpenYoutube.setOnClickListener { openInYoutube() }
        binding.btnOpenYoutubeSmall.setOnClickListener { openInYoutube() }

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(true)
        }
        binding.webView.addJavascriptInterface(PlayerBridge(), "AndroidBridge")
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                if (request.isForMainFrame) runOnUiThread { showErrorOverlay(R.string.video_error_embed) }
            }
        }
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                enterFullscreen(view, callback)
            }

            override fun onHideCustomView() = exitFullscreen()

            // زر "Watch on YouTube" داخل المشغّل كيفتح تطبيق يوتيوب/المتصفح
            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
            ): Boolean {
                val temp = WebView(view.context)
                temp.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                        openExternal(request.url)
                        v.destroy()
                        return true
                    }
                }
                (resultMsg.obj as WebView.WebViewTransport).webView = temp
                resultMsg.sendToTarget()
                return true
            }
        }

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

        loadPlayer(noCookie = false)
    }

    private fun loadPlayer(noCookie: Boolean) {
        binding.videoErrorOverlay.visibility = View.GONE
        handler.removeCallbacks(watchdog)

        if (NetworkStatus.online.value == false) {
            showErrorOverlay(R.string.video_error_offline)
            return
        }
        if (videoId.isEmpty()) {
            showErrorOverlay(R.string.video_error_unavailable)
            return
        }

        val host = if (noCookie) "https://www.youtube-nocookie.com" else "https://www.youtube.com"
        // iframe بنفسنا مع referrerpolicy="strict-origin" (يوتيوب كيطلب Referer باش يتفادى الخطأ 153)
        val html = """
            <!DOCTYPE html><html><head>
            <meta charset="utf-8">
            <meta name="referrer" content="strict-origin">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>html,body{margin:0;height:100%;background:#000}iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style>
            </head><body>
            <iframe id="player" referrerpolicy="strict-origin"
              src="$host/embed/$videoId?enablejsapi=1&playsinline=1&rel=0&fs=1&autoplay=1&origin=$ORIGIN"
              allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen></iframe>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
            function onYouTubeIframeAPIReady() {
              new YT.Player('player', {
                events: {
                  onReady: function (e) { AndroidBridge.onPlayerReady(); try { e.target.playVideo(); } catch (x) {} },
                  onStateChange: function (e) { if (e.data === 1 || e.data === 3) AndroidBridge.onPlayerReady(); },
                  onError: function (e) { AndroidBridge.onPlayerError(e.data); }
                }
              });
            }
            </script></body></html>
        """.trimIndent()
        binding.webView.loadDataWithBaseURL("$ORIGIN/", html, "text/html", "utf-8", null)
        // إلا ما تشغل الفيديو فـ 12 ثانية (WebView قديم، خطأ صامت...) كنعاودو ولا كنبينو الزر
        handler.postDelayed(watchdog, 12_000L)
    }

    private fun onWatchdogFired() {
        if (started) return
        if (!retriedNoCookie) {
            retriedNoCookie = true
            loadPlayer(noCookie = true)
        } else {
            showErrorOverlay(R.string.video_error_timeout)
        }
    }

    private fun showErrorOverlay(messageRes: Int) {
        handler.removeCallbacks(watchdog)
        binding.tvVideoError.setText(messageRes)
        binding.videoErrorOverlay.visibility = View.VISIBLE
    }

    /** كيتنادى من JavaScript */
    private inner class PlayerBridge {
        @JavascriptInterface
        fun onPlayerReady() {
            runOnUiThread {
                started = true
                handler.removeCallbacks(watchdog)
            }
        }

        @JavascriptInterface
        fun onPlayerError(code: Int) {
            runOnUiThread { handlePlayerError(code) }
        }
    }

    private fun handlePlayerError(code: Int) {
        // 101/150/152/153 = التضمين ممنوع أو ناقص تعريف، 5 = خطأ مشغّل HTML5
        val embedIssue = code == 101 || code == 150 || code == 152 || code == 153 || code == 5
        if (embedIssue && !retriedNoCookie) {
            retriedNoCookie = true
            started = false
            loadPlayer(noCookie = true)
            return
        }
        showErrorOverlay(if (code == 100) R.string.video_error_unavailable else R.string.video_error_embed)
    }

    private fun openInYoutube() {
        if (videoId.isEmpty()) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")))
        } catch (e: ActivityNotFoundException) {
            openExternal(Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        }
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.video_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- تكبير الشاشة ----------

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
        handler.removeCallbacks(watchdog)
        if (customView != null) exitFullscreen()
        binding.webView.destroy()
        super.onDestroy()
    }

    companion object {
        // أصل صفحة التضمين: موقع المشروع الرسمي (كيعطي يوتيوب Referer و origin واضحين)
        private const val ORIGIN = "https://amiri41.github.io"
    }
}
