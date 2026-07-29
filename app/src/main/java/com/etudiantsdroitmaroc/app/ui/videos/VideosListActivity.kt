package com.etudiantsdroitmaroc.app.ui.videos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.VideoLesson
import com.etudiantsdroitmaroc.app.data.remote.VideosRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityVideosListBinding
import kotlinx.coroutines.launch

/** كتعرض فيديوهات مادة فيديو معينة (videoSubjectId) - كيتم فتحها من VideosHomeActivity */
class VideosListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideosListBinding
    private val repository = VideosRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoSubjectId = intent.getStringExtra("videoSubjectId")
        val videoSubjectName = intent.getStringExtra("videoSubjectName")
        binding.toolbar.title = if (!videoSubjectName.isNullOrEmpty()) "🎥 فيديوهات: $videoSubjectName" else "🎥 فيديوهات"

        binding.toolbar.setNavigationOnClickListener { finish() }
        com.etudiantsdroitmaroc.app.utils.BannerAdHelper.attach(this, binding.bannerAdContainer)
        com.etudiantsdroitmaroc.app.utils.AdManager.loadRewarded(this)

        val adapter = VideoAdapter(emptyList()) { openVideo(it) }
        binding.rvVideos.layoutManager = LinearLayoutManager(this)
        binding.rvVideos.adapter = adapter

        if (videoSubjectId.isNullOrEmpty()) {
            binding.tvEmpty.visibility = android.view.View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                val videos = repository.getVideosForVideoSubject(videoSubjectId)
                adapter.updateData(videos)
                binding.tvEmpty.visibility = if (videos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@VideosListActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openVideo(video: VideoLesson) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("مشاهدة الفيديو")
            .setMessage("شوف إعلان قصير باش تفتح هاد الفيديو")
            .setPositiveButton("شوف الإعلان وكمل") { _, _ ->
                com.etudiantsdroitmaroc.app.utils.AdManager.showRewarded(
                    this,
                    onReward = { launchVideoPlayer(video) },
                    onUnavailable = { launchVideoPlayer(video) } // الإعلان ماكانش جاهز، نخليوه يشوف الفيديو بلا ما نعطلوه
                )
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun launchVideoPlayer(video: VideoLesson) {
        com.etudiantsdroitmaroc.app.utils.ReviewHelper.recordPositiveAction(this)
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("title", video.title)
        intent.putExtra("description", video.description)
        intent.putExtra("youtubeId", video.youtubeId)
        startActivity(intent)
    }
}
