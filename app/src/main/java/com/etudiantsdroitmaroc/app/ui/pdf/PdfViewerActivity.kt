package com.etudiantsdroitmaroc.app.ui.pdf

import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.databinding.ActivityPdfViewerBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.Executors

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val renderDispatcher = executor.asCoroutineDispatcher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = intent.getStringExtra("title") ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        val localPath = intent.getStringExtra("localPath")
        val remoteUrl = intent.getStringExtra("remoteUrl")

        if (localPath != null) {
            // مسار أوفلاين - كيتفتح مباشرة من الهاتف بلا انترنت
            openFile(File(localPath))
        } else if (remoteUrl != null) {
            // مسار أونلاين - كيتحمل مؤقتا فالذاكرة باش يتقرا
            loadRemotePdf(remoteUrl)
        } else {
            Toast.makeText(this, "تعذر فتح الملف", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun openFile(file: File) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val r = PdfRenderer(pfd)
            if (r.pageCount == 0) throw IllegalStateException("empty")
            descriptor = pfd
            renderer = r

            val width = (resources.displayMetrics.widthPixels * 1.5f).toInt().coerceAtMost(1600)
            val adapter = PdfPagesAdapter(r, lifecycleScope, renderDispatcher, width)
            val lm = LinearLayoutManager(this)
            binding.rvPages.layoutManager = lm
            binding.rvPages.adapter = adapter
            binding.rvPages.setItemViewCacheSize(2)
            binding.progress.visibility = View.GONE
            binding.tvPageIndicator.visibility = View.VISIBLE
            binding.tvPageIndicator.text = "1 / ${r.pageCount}"
            binding.rvPages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    val pos = lm.findFirstVisibleItemPosition()
                    if (pos >= 0) binding.tvPageIndicator.text = "${pos + 1} / ${r.pageCount}"
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الملف", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadRemotePdf(url: String) {
        lifecycleScope.launch {
            try {
                val tempFile = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val response = client.newCall(Request.Builder().url(url).build()).execute()
                    if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}")
                    val file = File(cacheDir, "temp_view.pdf")
                    response.body?.byteStream()?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    file
                }
                openFile(tempFile)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Toast.makeText(this@PdfViewerActivity, "تحتاج انترنت باش تفتح هاد الملف", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        binding.rvPages.adapter = null
        try { renderer?.close() } catch (_: Exception) {}
        try { descriptor?.close() } catch (_: Exception) {}
        executor.shutdown()
        super.onDestroy()
    }
}
