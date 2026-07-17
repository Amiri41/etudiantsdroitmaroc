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

class VideosListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideosListBinding
    private val repository = VideosRepository()
    private lateinit var adapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        com.etudiantsdroitmaroc.app.utils.BannerAdHelper.attach(this, binding.bannerAdContainer)

        adapter = VideoAdapter(emptyList()) { openVideo(it) }
        binding.rvVideos.layoutManager = LinearLayoutManager(this)
        binding.rvVideos.adapter = adapter

        loadVideos()
    }

    private fun loadVideos() {
        lifecycleScope.launch {
            try {
                val videos = repository.getVideos()
                adapter.updateData(videos)
                binding.tvEmpty.visibility = if (videos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
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
