package com.etudiantsdroitmaroc.app.ui.subject

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.PdfDocument
import com.etudiantsdroitmaroc.app.data.remote.PdfRepository
import com.etudiantsdroitmaroc.app.databinding.ActivitySubjectDetailBinding
import com.etudiantsdroitmaroc.app.ui.pdf.PdfViewerActivity
import com.etudiantsdroitmaroc.app.utils.AdManager
import kotlinx.coroutines.launch

class SubjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectDetailBinding
    private lateinit var repository: PdfRepository
    private lateinit var adapter: PdfAdapter
    private val downloadedIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val subjectId = intent.getStringExtra("subjectId") ?: return
        val subjectName = intent.getStringExtra("subjectName") ?: ""
        binding.toolbar.title = subjectName
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSubjectVideos.setOnClickListener {
            val intent = Intent(this, com.etudiantsdroitmaroc.app.ui.videos.VideosListActivity::class.java)
            intent.putExtra("subjectId", subjectId)
            intent.putExtra("subjectName", subjectName)
            startActivity(intent)
        }

        repository = PdfRepository(this)

        adapter = PdfAdapter(
            pdfs = emptyList(),
            isDownloaded = { downloadedIds.contains(it) },
            onOpen = { openPdf(it) },
            onDownload = { downloadPdf(it) }
        )
        binding.rvPdfs.layoutManager = LinearLayoutManager(this)
        binding.rvPdfs.adapter = adapter

        loadPdfs(subjectId)
    }

    private fun loadPdfs(subjectId: String) {
        lifecycleScope.launch {
            val pdfs = repository.getPdfsForSubject(subjectId)
            // نتأكد وحدة وحدة واش كل PDF محملة محليا
            val downloaded = pdfs.filter { repository.isDownloaded(it.id) }
            downloadedIds.addAll(downloaded.map { it.id })
            adapter.updateData(pdfs)
        }
    }

    private fun downloadPdf(pdf: PdfDocument) {
        lifecycleScope.launch {
            Toast.makeText(this@SubjectDetailActivity, "جاري التحميل…", Toast.LENGTH_SHORT).show()
            val result = repository.downloadForOffline(pdf)
            if (result.isSuccess) {
                downloadedIds.add(pdf.id)
                adapter.notifyDataSetChanged()
                Toast.makeText(this@SubjectDetailActivity, "تم التحميل ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SubjectDetailActivity, "فشل التحميل، تأكد من الانترنت", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPdf(pdf: PdfDocument) {
        lifecycleScope.launch {
            val localPath = repository.getLocalPath(pdf.id)
            AdManager.showInterstitialIfReady(this@SubjectDetailActivity) {
                val intent = Intent(this@SubjectDetailActivity, PdfViewerActivity::class.java)
                intent.putExtra("title", pdf.title)
                if (localPath != null) {
                    intent.putExtra("localPath", localPath)
                } else {
                    intent.putExtra("remoteUrl", pdf.storageUrl)
                }
                startActivity(intent)
            }
        }
    }
}
