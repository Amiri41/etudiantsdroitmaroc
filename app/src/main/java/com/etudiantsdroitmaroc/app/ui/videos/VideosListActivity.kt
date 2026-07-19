package com.etudiantsdroitmaroc.app.ui.videos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.VideoLesson
import com.etudiantsdroitmaroc.app.data.remote.ChaptersRepository
import com.etudiantsdroitmaroc.app.data.remote.VideosRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityVideosListBinding
import kotlinx.coroutines.launch

private const val SECTION_PRIVATE = "private"
private const val SECTION_PUBLIC = "public"
private const val SECTION_MASTER = "master"
private const val SECTION_PHD = "phd"

class VideosListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideosListBinding
    private val repository = VideosRepository()
    private val chaptersRepository = ChaptersRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val subjectId = intent.getStringExtra("subjectId")
        val subjectName = intent.getStringExtra("subjectName")
        binding.toolbar.title = if (!subjectName.isNullOrEmpty()) "🎥 فيديوهات: $subjectName" else "🎥 فيديوهات قانونية"

        binding.toolbar.setNavigationOnClickListener { finish() }
        com.etudiantsdroitmaroc.app.utils.BannerAdHelper.attach(this, binding.bannerAdContainer)

        if (!subjectId.isNullOrEmpty()) {
            loadSingleSubjectVideos(subjectId)
        } else {
            loadGroupedVideos()
        }
    }

    private fun loadSingleSubjectVideos(subjectId: String) {
        val adapter = VideoAdapter(emptyList()) { openVideo(it) }
        binding.rvVideos.layoutManager = LinearLayoutManager(this)
        binding.rvVideos.adapter = adapter

        lifecycleScope.launch {
            try {
                val videos = repository.getVideosForSubject(subjectId)
                adapter.updateData(videos)
                binding.tvEmpty.visibility = if (videos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@VideosListActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadGroupedVideos() {
        val sectionAdapter = VideoSectionAdapter(emptyList()) { openVideo(it) }
        binding.rvVideos.layoutManager = LinearLayoutManager(this)
        binding.rvVideos.adapter = sectionAdapter

        lifecycleScope.launch {
            try {
                val privateChapters = chaptersRepository.getChaptersForSection(SECTION_PRIVATE)
                val publicChapters = chaptersRepository.getChaptersForSection(SECTION_PUBLIC)
                val chapterNames = (privateChapters + publicChapters).associate { it.id to it.name }

                val grouped = repository.getVideoSectionsGroupedBySubject()
                val sections = grouped.map { (subject, videos) ->
                    val chapterName = chapterNames[subject.chapterId] ?: ""
                    val sectionLabel = when (subject.section) {
                        SECTION_PRIVATE -> "القانون الخاص - $chapterName"
                        SECTION_PUBLIC -> "القانون العام - $chapterName"
                        SECTION_MASTER -> "ماستر"
                        SECTION_PHD -> "دكتوراه"
                        else -> "مواضيع قانونية عامة"
                    }
                    VideoSection(title = "$sectionLabel · ${subject.name}", videos = videos)
                }
                sectionAdapter.updateData(sections)
                binding.tvEmpty.visibility = if (sections.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@VideosListActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openVideo(video: VideoLesson) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("title", video.title)
        intent.putExtra("description", video.description)
        intent.putExtra("youtubeId", video.youtubeId)
        startActivity(intent)
    }
}
