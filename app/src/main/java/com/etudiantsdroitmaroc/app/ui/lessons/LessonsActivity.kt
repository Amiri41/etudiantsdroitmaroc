package com.etudiantsdroitmaroc.app.ui.lessons

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.etudiantsdroitmaroc.app.data.model.PdfDocument
import com.etudiantsdroitmaroc.app.data.model.VideoLesson
import com.etudiantsdroitmaroc.app.data.remote.PdfRepository
import com.etudiantsdroitmaroc.app.data.remote.VideosRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityLessonsBinding
import com.etudiantsdroitmaroc.app.ui.pdf.PdfViewerActivity
import com.etudiantsdroitmaroc.app.ui.videos.VideoPlayerActivity
import com.etudiantsdroitmaroc.app.utils.AdManager
import com.etudiantsdroitmaroc.app.utils.BannerAdHelper
import com.etudiantsdroitmaroc.app.utils.ErrorUtils
import com.etudiantsdroitmaroc.app.utils.goHome
import com.etudiantsdroitmaroc.app.utils.ReviewHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** صفحة المادة: بطاقات (PDF + فيديوهات يوتيوب مرتبطة بالمادة) */
class LessonsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLessonsBinding
    private lateinit var pdfRepository: PdfRepository
    private val videosRepository = VideosRepository()
    private lateinit var adapter: LessonCardAdapter
    private val downloadedIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLessonsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val subjectId = intent.getStringExtra("subjectId") ?: ""
        val videoSubjectId = intent.getStringExtra("videoSubjectId") ?: ""
        if (subjectId.isEmpty() && videoSubjectId.isEmpty()) { finish(); return }
        val subjectName = intent.getStringExtra("subjectName") ?: ""

        binding.toolbar.title = subjectName
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnHome.setOnClickListener { goHome() }
        BannerAdHelper.attach(this, binding.bannerAdContainer)

        pdfRepository = PdfRepository(this)
        AdManager.loadRewarded(this)

        adapter = LessonCardAdapter(
            items = emptyList(),
            isDownloaded = { downloadedIds.contains(it) },
            onOpen = { openCard(it) },
            onDownload = { downloadPdf(it) }
        )
        binding.rvLessons.layoutManager = GridLayoutManager(this, GRID_COLUMNS) // 4 بطاقات فكل سطر، وكتنزل لتحت
        binding.rvLessons.adapter = adapter

        load(subjectId, videoSubjectId)
    }

    /** كنعرضو كل محتوى المادة: ملفات PDF + الفيديوهات المرتبطة بها (إلا كانو) */
    private fun load(subjectId: String, videoSubjectId: String) {
        lifecycleScope.launch {
            try {
                if (videoSubjectId.isNotEmpty()) {
                    // مادة من شجرة الفيديوهات
                    val cards = videosRepository.getVideosForVideoSubject(videoSubjectId).map { it.toCard() }
                    adapter.updateData(cards)
                    binding.tvEmpty.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
                    return@launch
                }
                val pdfs = pdfRepository.getPdfsForSubject(subjectId)
                downloadedIds.addAll(pdfs.filter { pdfRepository.isDownloaded(it.id) }.map { it.id })
                val videos = try {
                    videosRepository.getVideosForSubject(subjectId)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    emptyList()
                }
                val cards = pdfs.map { it.toCard() } + videos.map { it.toCard() }
                adapter.updateData(cards)
                binding.tvEmpty.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Toast.makeText(this@LessonsActivity, ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun PdfDocument.toCard() = LessonCard(
        id = id, title = title, description = description, imageUrl = coverImageUrl, pdf = this
    )

    private fun VideoLesson.toCard() = LessonCard(
        id = id,
        title = title,
        description = description,
        imageUrl = thumbnailUrl.ifEmpty { "https://img.youtube.com/vi/$youtubeId/mqdefault.jpg" }
            .replace("/hqdefault.jpg", "/mqdefault.jpg"),
        video = this
    )

    private fun openCard(card: LessonCard) {
        card.pdf?.let { openPdf(it) }
        card.video?.let { openVideo(it) }
    }

    // ---------- PDF ----------

    private fun openPdf(pdf: PdfDocument) {
        lifecycleScope.launch {
            val localPath = pdfRepository.getLocalPath(pdf.id)
            AdManager.showInterstitialIfReady(this@LessonsActivity) {
                val intent = Intent(this@LessonsActivity, PdfViewerActivity::class.java)
                intent.putExtra("title", pdf.title)
                if (localPath != null) intent.putExtra("localPath", localPath)
                else intent.putExtra("remoteUrl", pdf.storageUrl)
                startActivity(intent)
            }
        }
    }

    private fun downloadPdf(pdf: PdfDocument) {
        AlertDialog.Builder(this)
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
            Toast.makeText(this@LessonsActivity, "جاري التحميل…", Toast.LENGTH_SHORT).show()
            val result = pdfRepository.downloadForOffline(pdf)
            if (result.isSuccess) {
                downloadedIds.add(pdf.id)
                adapter.notifyDataSetChanged()
                Toast.makeText(this@LessonsActivity, "تم التحميل ✅", Toast.LENGTH_SHORT).show()
                ReviewHelper.recordPositiveAction(this@LessonsActivity)
            } else {
                Toast.makeText(this@LessonsActivity, "فشل التحميل، تأكد من الانترنت", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- Video ----------

    private fun openVideo(video: VideoLesson) {
        AlertDialog.Builder(this)
            .setTitle("مشاهدة الفيديو")
            .setMessage("شوف إعلان قصير باش تفتح هاد الفيديو")
            .setPositiveButton("شوف الإعلان وكمل") { _, _ ->
                AdManager.showRewarded(
                    this,
                    onReward = { launchVideoPlayer(video) },
                    onUnavailable = { launchVideoPlayer(video) }
                )
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun launchVideoPlayer(video: VideoLesson) {
        ReviewHelper.recordPositiveAction(this)
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("title", video.title)
        intent.putExtra("description", video.description)
        intent.putExtra("youtubeId", video.youtubeId)
        startActivity(intent)
    }

    companion object {
        private const val GRID_COLUMNS = 4
    }
}
