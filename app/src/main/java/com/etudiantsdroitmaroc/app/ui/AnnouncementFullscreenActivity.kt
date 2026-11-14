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
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.remote.AnnouncementRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityAnnouncementFullscreenBinding
import com.etudiantsdroitmaroc.app.ui.videos.VideoPlayerActivity
import kotlinx.coroutines.launch

/**
 * إعلان بشاشة كاملة. أشكال:
 * 1) تصميم جاهز: صورة مدورة + عنوان + نص + زر (من حقول لوحة الأدمن)
 * 2) فيديو: يوتيوب (كيفتح مشغل التطبيق) أو mp4 مباشر (كيشتغل داخل الإعلان)
 * 3) HTML+CSS من لوحة الأدمن (JavaScript معطل): إلا كان htmlContent موجود
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
        val videoUrl = intent.getStringExtra("videoUrl") ?: ""
        val hasHtml = intent.getBooleanExtra("hasHtml", false)

        binding.tvBadge.text = if (type == "update") "🚀 تحديث جديد" else "إعلان"
        binding.btnClose.setOnClickListener { finish() }

        bindNative(title, message, imageUrl, linkUrl, buttonText, videoUrl)

        if (hasHtml) {
            // نخبيو التصميم الجاهز حتى يجي الـ HTML؛ إلا فشل كنرجعو ليه
            binding.scrollNative.visibility = View.INVISIBLE
            lifecycleScope.launch {
                val html = AnnouncementRepository.getCurrentAnnouncement()?.htmlContent.orEmpty()
                if (html.isNotBlank()) showHtml(html) else binding.scrollNative.visibility = View.VISIBLE
            }
        }
    }

    private fun bindNative(
        title: String, message: String, imageUrl: String,
        linkUrl: String, buttonText: String, videoUrl: String
    ) {
        binding.tvTitle.text = title
        binding.tvMessage.text = message
        binding.tvTitle.visibility = if (title.isBlank()) View.GONE else View.VISIBLE
        binding.tvMessage.visibility = if (message.isBlank()) View.GONE else View.VISIBLE

        var targetLink = linkUrl
        var label = buttonText
        val ytId = youtubeId(videoUrl)

        when {
            ytId != null -> {
                // يوتيوب: صورة مصغرة + زر تشغيل، والضغط كيفتح مشغل التطبيق
                showMedia(imageUrl.ifBlank { "https://img.youtube.com/vi/$ytId/hqdefault.jpg" }, video = true)
                binding.mediaFrame.setOnClickListener {
                    startActivity(
                        Intent(this, VideoPlayerActivity::class.java)
                            .putExtra("title", binding.tvTitle.text.toString())
                            .putExtra("description", binding.tvMessage.text.toString())
                            .putExtra("youtubeId", videoUrl)
                    )
                }
            }
            isDirectVideo(videoUrl) -> {
                showMedia(imageUrl, video = true)
                binding.mediaFrame.setOnClickListener { playInline(videoUrl) }
            }
            else -> {
                if (imageUrl.isNotEmpty()) showMedia(imageUrl, video = false)
                // فيسبوك/إنستغرام/أي رابط آخر: كيتحل برا عبر الزر
                if (videoUrl.isNotBlank() && targetLink.isBlank()) {
                    targetLink = videoUrl
                    if (label.isBlank()) label = "شاهد الفيديو"
                }
            }
        }

        if (targetLink.isNotEmpty()) {
            binding.btnAction.visibility = View.VISIBLE
            if (label.isNotBlank()) binding.btnAction.text = label
            binding.btnAction.setOnClickListener { openExternal(Uri.parse(targetLink)) }
        }
    }

    private fun showMedia(url: String, video: Boolean) {
        binding.cardImage.visibility = View.VISIBLE
        if (video) {
            // 16:9 بعرض الشاشة (ناقص الهوامش)
            val w = resources.displayMetrics.widthPixels - (56 * resources.displayMetrics.density).toInt()
            binding.mediaFrame.layoutParams = FrameLayout.LayoutParams(w, w * 9 / 16)
            binding.ivAnnouncementImage.layoutParams =
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            binding.ivPlay.visibility = View.VISIBLE
        }
        if (url.isNotEmpty()) Glide.with(this).load(url).into(binding.ivAnnouncementImage)
    }

    private fun playInline(url: String) {
        binding.mediaFrame.setOnClickListener(null)
        binding.ivPlay.visibility = View.GONE
        binding.ivAnnouncementImage.visibility = View.GONE
        val vv = binding.videoView
        vv.visibility = View.VISIBLE
        vv.setMediaController(MediaController(this).also { it.setAnchorView(vv) })
        vv.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "ما قدرناش نشغلو الفيديو", Toast.LENGTH_SHORT).show()
            openExternal(Uri.parse(url))
            true
        }
        vv.setVideoURI(Uri.parse(url))
        vv.setOnPreparedListener { it.start() }
    }

    private fun youtubeId(url: String): String? =
        YT_REGEX.find(url)?.groupValues?.get(1)

    private fun isDirectVideo(url: String): Boolean {
        val path = url.substringBefore('?').lowercase()
        return url.startsWith("https://") && (path.endsWith(".mp4") || path.endsWith(".webm") ||
            path.endsWith(".3gp") || path.endsWith(".m3u8"))
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

    private companion object {
        val YT_REGEX = Regex("""(?:youtube\.com/(?:watch\?v=|embed/|shorts/|live/)|youtu\.be/)([A-Za-z0-9_-]{11})""")
    }
}
