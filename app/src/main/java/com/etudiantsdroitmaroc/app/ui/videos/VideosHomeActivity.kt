package com.etudiantsdroitmaroc.app.ui.videos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.remote.VideoChaptersRepository
import com.etudiantsdroitmaroc.app.data.remote.VideoSubjectsRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityVideosHomeBinding
import com.etudiantsdroitmaroc.app.ui.common.NavButtonAdapter
import com.etudiantsdroitmaroc.app.ui.common.NavItem
import com.etudiantsdroitmaroc.app.utils.ErrorUtils
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * شجرة الفيديوهات (مستقلة عن PDF): بلا extras = زرين (خاص/عام)، section = الفصول،
 * section + chapterId = المواد، والمادة كتفتح VideosListActivity (بطاقات الفيديو).
 */
class VideosHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideosHomeBinding
    private val chaptersRepository = VideoChaptersRepository()
    private val subjectsRepository = VideoSubjectsRepository()
    private lateinit var adapter: NavButtonAdapter
    private var section: String? = null
    private var chapterId: String? = null
    private var lastItems: List<NavItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        section = intent.getStringExtra("section")
        chapterId = intent.getStringExtra("chapterId")
        binding.toolbar.title = intent.getStringExtra("title") ?: "الفيديوهات"
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = NavButtonAdapter { onItemClick(it) }
        binding.rvVideoChapters.layoutManager = LinearLayoutManager(this)
        binding.rvVideoChapters.adapter = adapter

        if (section == null) {
            adapter.submitList(listOf(
                NavItem("private", "فيديوهات القانون الخاص"),
                NavItem("public", "فيديوهات القانون العام")
            ))
        } else {
            load()
        }
    }

    private fun onItemClick(item: NavItem) {
        val sec = section
        val cid = chapterId
        when {
            sec == null -> startActivity(Intent(this, VideosHomeActivity::class.java)
                .putExtra("section", item.id)
                .putExtra("title", item.title))
            cid == null -> startActivity(Intent(this, VideosHomeActivity::class.java)
                .putExtra("section", sec)
                .putExtra("chapterId", item.id)
                .putExtra("title", item.title))
            else -> startActivity(Intent(this, VideosListActivity::class.java)
                .putExtra("videoSubjectId", item.id)
                .putExtra("videoSubjectName", item.title))
        }
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
                if (!shown) Toast.makeText(this@VideosHomeActivity, ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchAndRender(source: Source): Boolean {
        val sec = section ?: return true
        val cid = chapterId
        val items: List<NavItem> = if (cid == null) {
            chaptersRepository.getChaptersForSection(sec, source).map { NavItem(it.id, it.name) }
        } else {
            subjectsRepository.getAllActive(source)
                .filter { it.section == sec && it.chapterId == cid }
                .sortedBy { it.orderIndex }
                .map { NavItem(it.id, it.name) }
        }
        if (source == Source.CACHE && items.isEmpty()) return false
        if (items == lastItems) return true
        lastItems = items
        adapter.submitList(items)
        binding.tvEmptyVideoSubjects.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        return true
    }
}
