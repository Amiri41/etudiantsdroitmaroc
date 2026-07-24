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

    private fun loadData() {
        lifecycleScope.launch {
            try {
                allVideoSubjects = subjectsRepository.getAllActive()
                applyChapterRows()
            } catch (e: Exception) {
                Toast.makeText(this@VideosHomeActivity, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun applyChapterRows() {
        lifecycleScope.launch {
            try {
                val chapters = chaptersRepository.getChaptersForSection(currentSection)
                val rows = chapters.mapNotNull { chapter ->
                    val subjectsForChapter = allVideoSubjects
                        .filter { it.section == currentSection && it.chapterId == chapter.id }
                        .sortedBy { it.orderIndex }
                    if (subjectsForChapter.isEmpty()) null else VideoChapterWithSubjects(chapter, subjectsForChapter)
                }
                chapterRowAdapter.updateData(rows)
                binding.tvEmptyVideoSubjects.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@VideosHomeActivity, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openVideoSubject(subject: VideoSubject) {
        val intent = Intent(this, VideosListActivity::class.java)
        intent.putExtra("videoSubjectId", subject.id)
        intent.putExtra("videoSubjectName", subject.name)
        startActivity(intent)
    }
}
