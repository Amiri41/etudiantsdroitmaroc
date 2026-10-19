package com.etudiantsdroitmaroc.app.ui.videos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.VideoLesson
import com.etudiantsdroitmaroc.app.data.remote.VideosRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityVideosListBinding
import com.etudiantsdroitmaroc.app.ui.common.ContentCard
import com.etudiantsdroitmaroc.app.ui.common.ContentCardAdapter
import com.etudiantsdroitmaroc.app.utils.AdManager
import com.etudiantsdroitmaroc.app.utils.BannerAdHelper
import com.etudiantsdroitmaroc.app.utils.ErrorUtils
import com.etudiantsdroitmaroc.app.utils.ReviewHelper
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** بطاقات فيديوهات مادة معينة (صورة، عنوان marquee، وصف 4 أسطر)، من الكاش أولا */
class VideosListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideosListBinding
    private val repository = VideosRepository()
    private lateinit var adapter: ContentCardAdapter
    private var videos: List<VideoLesson> = emptyList()
    private var lastVideos: List<VideoLesson>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoSubjectId = intent.getStringExtra("videoSubjectId")
        binding.toolbar.title = intent.getStringExtra("videoSubjectName") ?: "🎥 فيديوهات"
        binding.toolbar.setNavigationOnClickListener { finish() }
        BannerAdHelper.attach(this, binding.bannerAdContainer)
        AdManager.loadRewarded(this)

        adapter = ContentCardAdapter(onClick = { card ->
            videos.firstOrNull { it.id == card.id }?.let { openVideo(it) }
        })
        binding.rvVideos.layoutManager = LinearLayoutManager(this)
        binding.rvVideos.adapter = adapter

        if (videoSubjectId.isNullOrEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }
        load(videoSubjectId)
    }

    private fun load(videoSubjectId: String) {
        lifecycleScope.launch {
            var shown = false
            try {
                shown = fetchAndRender(videoSubjectId, Source.CACHE)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
            try {
                fetchAndRender(videoSubjectId, Source.SERVER)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (!shown) Toast.makeText(this@VideosListActivity, ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchAndRender(videoSubjectId: String, source: Source): Boolean {
        val list = repository.getVideosForVideoSubject(videoSubjectId, source)
        if (source == Source.CACHE && list.isEmpty()) return false
        if (list == lastVideos) return true
        lastVideos = list
        videos = list
        adapter.submitList(list.map { v ->
            ContentCard(
                id = v.id,
                title = v.title,
                description = v.description,
                imageUrl = v.thumbnailUrl.ifEmpty { "https://img.youtube.com/vi/${v.youtubeId}/hqdefault.jpg" },
                placeholderRes = R.drawable.ic_play_circle,
                isVideo = true
            )
        })
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        return true
    }

    private fun openVideo(video: VideoLesson) {
        // الفيديو كيتشغل مباشرة (حيدنا شرط مشاهدة الإعلان)
        launchVideoPlayer(video)
    }

    private fun launchVideoPlayer(video: VideoLesson) {
        ReviewHelper.recordPositiveAction(this)
        startActivity(Intent(this, VideoPlayerActivity::class.java)
            .putExtra("title", video.title)
            .putExtra("description", video.description)
            .putExtra("youtubeId", video.youtubeId))
    }
}
