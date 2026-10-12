package com.etudiantsdroitmaroc.app.ui.videos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.Chapter
import com.etudiantsdroitmaroc.app.data.model.VideoSubject
import com.etudiantsdroitmaroc.app.data.remote.VideoChaptersRepository
import com.etudiantsdroitmaroc.app.data.remote.VideoSubjectsRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityVideosHomeBinding
import com.etudiantsdroitmaroc.app.utils.BannerAdHelper
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * شجرة الفيديوهات مستقلة تماما عن شجرة PDF: أقسام (خاص/عام) خاصة بيها، فصول خاصة بيها، مواد خاصة بيها.
 * الأدمين كيتحكم فيها بذاتها من لوحة التحكم (videoChapters + videoSubjects).
 */
class VideosHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideosHomeBinding
    private val chaptersRepository = VideoChaptersRepository()
    private val subjectsRepository = VideoSubjectsRepository()
    private lateinit var chapterRowAdapter: VideoChapterRowAdapter

    private var allVideoSubjects: List<VideoSubject> = emptyList()
    private var currentSection: String = "private"
    private var chaptersBySection: Map<String, List<Chapter>> = emptyMap()
    private var lastSig: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        BannerAdHelper.attach(this, binding.bannerAdContainer)

        chapterRowAdapter = VideoChapterRowAdapter(emptyList()) { subject -> openVideoSubject(subject) }
        binding.rvVideoChapters.layoutManager = LinearLayoutManager(this)
        binding.rvVideoChapters.adapter = chapterRowAdapter

        setupSectionChips()
        loadData()
    }

    private fun setupSectionChips() {
        val sections = listOf("private" to "⚖️ القانون الخاص", "public" to "📜 القانون العام")
        var firstChip: Chip? = null

        sections.forEach { (key, label) ->
            val chip = Chip(this)
            chip.text = label
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currentSection = key
                    applyChapterRows()
                }
            }
            if (firstChip == null) firstChip = chip
            binding.chipGroupVideoSection.addView(chip)
        }
        firstChip?.isChecked = true
    }

    /** عرض فوري من الكاش، وبعدها تحديث من السيرفر (كيتعاود الرسم غير إلا تبدل شي) */
    private fun loadData() {
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
                if (!shown) Toast.makeText(this@VideosHomeActivity, com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(e), Toast.LENGTH_LONG).show()
            }
        }
    }

    /** المواد وفصول القسمين كيتجبدو مع بعض (parallel) */
    private suspend fun fetchAndRender(source: Source): Boolean = coroutineScope {
        val subjectsJob = async { subjectsRepository.getAllActive(source) }
        val privateJob = async { chaptersRepository.getChaptersForSection("private", source) }
        val publicJob = async { chaptersRepository.getChaptersForSection("public", source) }
        val subjects = subjectsJob.await()
        val chapters = mapOf("private" to privateJob.await(), "public" to publicJob.await())
        if (source == Source.CACHE && subjects.isEmpty()) return@coroutineScope false
        val sig = Pair(subjects, chapters)
        if (sig == lastSig) return@coroutineScope true
        lastSig = sig
        allVideoSubjects = subjects
        chaptersBySection = chapters
        applyChapterRows()
        true
    }

    /** تبديل القسم (خاص/عام) كيتم من الذاكرة بلا ما نعاودو نجيبو من الشبكة */
    private fun applyChapterRows() {
        if (lastSig == null) return // مازال ما تحملات الداتا
        val chapters = chaptersBySection[currentSection].orEmpty()
        val rows = chapters.mapNotNull { chapter ->
            val subjectsForChapter = allVideoSubjects
                .filter { it.section == currentSection && it.chapterId == chapter.id }
                .sortedBy { it.orderIndex }
            if (subjectsForChapter.isEmpty()) null else VideoChapterWithSubjects(chapter, subjectsForChapter)
        }
        chapterRowAdapter.updateData(rows)
        binding.tvEmptyVideoSubjects.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openVideoSubject(subject: VideoSubject) {
        val intent = Intent(this, VideosListActivity::class.java)
        intent.putExtra("videoSubjectId", subject.id)
        intent.putExtra("videoSubjectName", subject.name)
        startActivity(intent)
    }
}
