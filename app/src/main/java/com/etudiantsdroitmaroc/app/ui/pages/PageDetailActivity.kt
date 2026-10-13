package com.etudiantsdroitmaroc.app.ui.pages

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.etudiantsdroitmaroc.app.databinding.ActivityPageDetailBinding

/** صفحة نص عادي، أو صفحة HTML (كتتعرض فـ WebView بلا JavaScript، والروابط كتفتح فالمتصفح) */
class PageDetailActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = intent.getStringExtra("title") ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        val html = intent.getStringExtra("html")
        if (html == null) {
            binding.tvContent.text = intent.getStringExtra("content") ?: ""
            return
        }

        binding.scrollText.visibility = View.GONE
        binding.webPage.visibility = View.VISIBLE
        binding.webPage.settings.apply {
            javaScriptEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        binding.webPage.setBackgroundColor(0)
        binding.webPage.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString())))
                } catch (_: Exception) {}
                return true
            }
        }

        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val bg = if (night) "#0A1F3D" else "#F7F8FA"
        val fg = if (night) "#F2F2F2" else "#1A1A1A"
        val page = """<!DOCTYPE html><html dir="rtl" lang="ar"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
body{margin:0;padding:16px;background:$bg;color:$fg;font-family:sans-serif;line-height:1.9;font-size:16px;word-wrap:break-word}
img,video,iframe,table{max-width:100%;height:auto}
a{color:#5AA9FF}
</style></head><body>$html</body></html>"""
        binding.webPage.loadDataWithBaseURL("https://localhost/", page, "text/html", "utf-8", null)
    }
}
