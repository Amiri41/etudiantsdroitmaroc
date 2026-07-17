package com.etudiantsdroitmaroc.app.ui.videos

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import com.etudiantsdroitmaroc.app.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding

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

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        val html = """
            <html><body style="margin:0;padding:0;">
            <iframe width="100%" height="100%" 
                src="https://www.youtube.com/embed/$youtubeId?rel=0" 
                frameborder="0" allow="autoplay; encrypted-media" allowfullscreen>
            </iframe>
            </body></html>
        """.trimIndent()

        binding.webView.loadDataWithBaseURL(
            "https://www.youtube.com", html, "text/html", "utf-8", null
        )
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
