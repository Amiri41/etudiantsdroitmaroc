package com.etudiantsdroitmaroc.app.ui.subject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.PdfDocument
import com.etudiantsdroitmaroc.app.data.model.Subject
import com.etudiantsdroitmaroc.app.data.remote.PdfRepository
import com.etudiantsdroitmaroc.app.databinding.ActivitySubjectDetailBinding
import com.etudiantsdroitmaroc.app.ui.common.ContentCard
import com.etudiantsdroitmaroc.app.ui.common.ContentCardAdapter
import com.etudiantsdroitmaroc.app.ui.pdf.PdfViewerActivity
import com.etudiantsdroitmaroc.app.utils.AdManager
import com.etudiantsdroitmaroc.app.utils.ErrorUtils
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * بطاقات PDF. بـ subjectId = ملفات مادة وحدة. بـ section (master/phd) = ملفات القسم كاملة مباشرة.
 * كتتعرض من الكاش أولا ومن بعد كتتحدث من السيرفر.
 */
class SubjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectDetailBinding
    private lateinit var repository: PdfRepository
    private lateinit var adapter: ContentCardAdapter
    private val downloadedIds = mutableSetOf<String>()
    private var pdfs: List<PdfDocument> = emptyList()
    private var lastPdfs: List<PdfDocument>? = null
    private var subjectId: String? = null
    private var section: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        subjectId = intent.getStringExtra("subjectId")
        section = intent.getStringExtra("section")
        if (subjectId == null && section == null) {
            finish()
            return
        }
        binding.toolbar.title = intent.getStringExtra("subjectName") ?: intent.getStringExtra("title") ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        repository = PdfRepository(this)
        AdManager.loadRewarded(this)

        adapter = ContentCardAdapter(
            onClick = { card -> pdfs.firstOrNull { it.id == card.id }?.let { openPdf(it) } },
            onStatusClick = { card ->
                pdfs.firstOrNull { it.id == card.id }?.let {
                    if (downloadedIds.contains(it.id)) openPdf(it) else downloadPdf(it)
                }
            },
            portrait = true
        )
        // شبكة من عمودين، بطاقات عمودية متوسطة الحجم (4:5)
        binding.rvPdfs.layoutManager = GridLayoutManager(this, 2)
        binding.rvPdfs.adapter = adapter

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            var shown = false
            try {
                shown = fetchAndRender(Source.CACHE)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
            try {
                fetchAndRender(Source.SERVER)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (!shown) Toast.makeText(this@SubjectDetailActivity, ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchAndRender(source: Source): Boolean {
        val sid = subjectId
        val sec = section
        val list: List<PdfDocument> = if (sid != null) {
            repository.getPdfsForSubject(sid, source)
        } else {
            loadSection(sec ?: return true, source)
        }
        if (source == Source.CACHE && list.isEmpty()) return false
        if (list == lastPdfs) return true
        lastPdfs = list
        pdfs = list
        downloadedIds.clear()
        list.forEach { if (repository.isDownloaded(it.id)) downloadedIds.add(it.id) }
        render()
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        return true
    }

    private suspend fun loadSection(sec: String, source: Source): List<PdfDocument> {
        val subjects = Firebase.firestore.collection("subjects")
            .whereEqualTo("section", sec)
            .get(source).await()
            .toObjects<Subject>()
            .filter { it.active }
            .sortedBy { it.orderIndex }
        return coroutineScope {
            subjects.map { s -> async { repository.getPdfsForSubject(s.id, source) } }.awaitAll().flatten()
        }
    }

    private fun render() {
        adapter.submitList(pdfs.map { p ->
            ContentCard(
                id = p.id,
                title = p.title,
                description = p.description,
                imageUrl = p.coverImageUrl,
                placeholderRes = R.drawable.ic_pdf,
                downloaded = downloadedIds.contains(p.id)
            )
        })
    }

    private fun downloadPdf(pdf: PdfDocument) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("تحميل الملف")
            .setMessage("شوف إعلان قصير باش تحمل هاد الملف وتقدر تقراه بلا أنترنت")
            .setPositiveButton("شوف الإعلان وحمل") { _, _ ->
                AdManager.showRewarded(
                    this,
                    onReward = { doDownloadPdf(pdf) },
                    onUnavailable = { doDownloadPdf(pdf) }
                )
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun doDownloadPdf(pdf: PdfDocument) {
        lifecycleScope.launch {
            Toast.makeText(this@SubjectDetailActivity, "جاري التحميل…", Toast.LENGTH_SHORT).show()
            val result = repository.downloadForOffline(pdf)
            if (result.isSuccess) {
                downloadedIds.add(pdf.id)
                render()
                Toast.makeText(this@SubjectDetailActivity, "تم التحميل ✅", Toast.LENGTH_SHORT).show()
                com.etudiantsdroitmaroc.app.utils.ReviewHelper.recordPositiveAction(this@SubjectDetailActivity)
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
