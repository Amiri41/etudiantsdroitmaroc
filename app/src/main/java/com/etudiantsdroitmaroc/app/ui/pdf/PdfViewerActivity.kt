package com.etudiantsdroitmaroc.app.ui.pdf

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.etudiantsdroitmaroc.app.databinding.ActivityPdfViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding

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
            binding.pdfView.fromFile(File(localPath)).load()
        } else if (remoteUrl != null) {
            // مسار أونلاين - كيتحمل مؤقتا فالذاكرة باش يتقرا
            loadRemotePdf(remoteUrl)
        } else {
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
                    val file = File(cacheDir, "temp_view.pdf")
                    response.body?.byteStream()?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    file
                }
                binding.pdfView.fromFile(tempFile).load()
            } catch (e: Exception) {
                Toast.makeText(this@PdfViewerActivity, "تحتاج انترنت باش تفتح هاد الملف", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
