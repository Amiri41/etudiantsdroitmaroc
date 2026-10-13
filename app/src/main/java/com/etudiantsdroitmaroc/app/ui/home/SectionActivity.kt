package com.etudiantsdroitmaroc.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.Subject
import com.etudiantsdroitmaroc.app.data.remote.ChaptersRepository
import com.etudiantsdroitmaroc.app.databinding.ActivitySectionBinding
import com.etudiantsdroitmaroc.app.ui.common.NavButtonAdapter
import com.etudiantsdroitmaroc.app.ui.common.NavItem
import com.etudiantsdroitmaroc.app.ui.subject.SubjectDetailActivity
import com.etudiantsdroitmaroc.app.utils.ErrorUtils
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * قانون خاص / عام: المستوى 1 = الفصول (أزرار عمودية)، المستوى 2 (chapterId) = المواد (أزرار عمودية)،
 * والمادة كتفتح قائمة بطاقات PDF. كل مستوى كيتعرض من الكاش أولا ومن بعد من السيرفر.
 */
class SectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySectionBinding
    private val chaptersRepository = ChaptersRepository()
    private lateinit var adapter: NavButtonAdapter
    private var section: String = "private"
    private var chapterId: String? = null
    private var lastItems: List<NavItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        section = intent.getStringExtra("section") ?: "private"
        chapterId = intent.getStringExtra("chapterId")
        binding.toolbar.title = intent.getStringExtra("title") ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = NavButtonAdapter { onItemClick(it) }
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter

        load()
    }

    private fun onItemClick(item: NavItem) {
        if (chapterId == null) {
            startActivity(Intent(this, SectionActivity::class.java)
                .putExtra("section", section)
                .putExtra("chapterId", item.id)
                .putExtra("title", item.title))
        } else {
            startActivity(Intent(this, SubjectDetailActivity::class.java)
                .putExtra("subjectId", item.id)
                .putExtra("subjectName", item.title))
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
                if (!shown) Toast.makeText(this@SectionActivity, ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchAndRender(source: Source): Boolean {
        val cid = chapterId
        val items: List<NavItem> = if (cid == null) {
            chaptersRepository.getChaptersForSection(section, source).map { NavItem(it.id, it.name) }
        } else {
            Firebase.firestore.collection("subjects")
                .whereEqualTo("chapterId", cid)
                .get(source).await()
                .toObjects<Subject>()
                .filter { it.active && it.section == section }
                .sortedBy { it.orderIndex }
                .map { NavItem(it.id, it.name) }
        }
        if (source == Source.CACHE && items.isEmpty()) return false
        if (items == lastItems) return true
        lastItems = items
        adapter.submitList(items)
        binding.tvEmptySubjects.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        return true
    }
}
