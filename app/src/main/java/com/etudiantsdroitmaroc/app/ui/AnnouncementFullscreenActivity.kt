package com.etudiantsdroitmaroc.app.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.remote.AnnouncementRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityAnnouncementFullscreenBinding
import kotlinx.coroutines.launch

/**
 * إعلان بشاشة كاملة. شكلين:
 * 1) تصميم جاهز: صورة مدورة + عنوان + نص + زر (من حقول لوحة الأدمن)
 * 2) HTML+CSS من لوحة الأدمن (JavaScript معطل): إلا كان htmlContent موجود
 * دايما كاينة علامة "إعلان" وزر إغلاق واضح.
 */
class AnnouncementFullscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnnouncementFullscreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnnouncementFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra("type") ?: "promo"
        val title = intent.getStringExtra("title") ?: ""
        val message = intent.getStringExtra("message") ?: ""
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val linkUrl = intent.getStringExtra("linkUrl") ?: ""
        val buttonText = intent.getStringExtra("buttonText") ?: ""
        val hasHtml = intent.getBooleanExtra("hasHtml", false)

        binding.tvBadge.text = if (type == "update") "🚀 تحديث جديد" else "إعلان"
        binding.btnClose.setOnClickListener { finish() }

        bindNative(title, message, imageUrl, linkUrl, buttonText)

        if (hasHtml) {
            // نخبيو التصميم الجاهز حتى يجي الـ HTML؛ إلا فشل كنرجعو ليه
            binding.scrollNative.visibility = View.INVISIBLE
            lifecycleScope.launch {
                val html = AnnouncementRepository.getCurrentAnnouncement()?.htmlContent.orEmpty()
                if (html.isNotBlank()) showHtml(html) else binding.scrollNative.visibility = View.VISIBLE
            }
        }
    }

    private fun bindNative(title: String, message: String, imageUrl: String, linkUrl: String, buttonText: String) {
        binding.tvTitle.text = title
        binding.tvMessage.text = message
        binding.tvTitle.visibility = if (title.isBlank()) View.GONE else View.VISIBLE
        binding.tvMessage.visibility = if (message.isBlank()) View.GONE else View.VISIBLE

        if (imageUrl.isNotEmpty()) {
            binding.cardImage.visibility = View.VISIBLE
            Glide.with(this).load(imageUrl).into(binding.ivAnnouncementImage)
        }

        if (linkUrl.isNotEmpty()) {
            binding.btnAction.visibility = View.VISIBLE
            if (buttonText.isNotBlank()) binding.btnAction.text = buttonText
            binding.btnAction.setOnClickListener { openExternal(Uri.parse(linkUrl)) }
        }
    }

    private fun showHtml(html: String) {
        val wv = binding.webView
        wv.settings.javaScriptEnabled = false          // HTML+CSS فقط
        wv.settings.allowFileAccess = false
        wv.settings.allowContentAccess = false
        wv.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        wv.setBackgroundColor(Color.WHITE)
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                openExternal(request.url)   // ما كنخليوش الـ WebView يتنقل، الروابط كتحل برا
                return true
            }
        }
        wv.loadDataWithBaseURL("https://amiri41.github.io/", html, "text/html", "utf-8", null)
        wv.visibility = View.VISIBLE
        binding.scrollNative.visibility = View.GONE
    }

    /** كنفتحو غير http/https/mailto/tel، وبلا crash إلا ما كاين حتى تطبيق */
    private fun openExternal(uri: Uri) {
        val scheme = uri.scheme?.lowercase()
        if (scheme !in listOf("https", "http", "mailto", "tel")) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
        }
    }
}
