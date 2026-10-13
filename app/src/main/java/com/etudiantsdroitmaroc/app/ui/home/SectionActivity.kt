package com.etudiantsdroitmaroc.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import com.etudiantsdroitmaroc.app.data.model.Chapter
import com.etudiantsdroitmaroc.app.data.model.Subject
import com.etudiantsdroitmaroc.app.data.remote.ChaptersRepository
import com.etudiantsdroitmaroc.app.databinding.ActivitySectionBinding
import com.etudiantsdroitmaroc.app.ui.subject.SubjectDetailActivity
import com.etudiantsdroitmaroc.app.utils.ErrorUtils
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** صفحة قسم (خاص/عام/ماستر/دكتوراه): كتعرض الفصول والمواد، من الكاش أولا ومن بعد من السيرفر */
class SectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySectionBinding
    private val chaptersRepository = ChaptersRepository()
    private lateinit var subjectAdapter: SubjectAdapter
    private lateinit var chapterRowAdapter: ChapterRowAdapter
    private var section: String = "private"
    private var lastRenderSig: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        section = intent.getStringExtra("section") ?: "private"
        binding.toolbar.title = intent.getStringExtra("title") ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        subjectAdapter = SubjectAdapter(emptyList()) { openSubject(it) }
        binding.rvSubjects.layoutManager = GridLayoutManager(this, 2)
        binding.rvSubjects.adapter = subjectAdapter

        chapterRowAdapter = ChapterRowAdapter(emptyList()) { openSubject(it) }
        binding.rvChapters.layoutManager = LinearLayoutManager(this)
        binding.rvChapters.adapter = chapterRowAdapter

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
                if (!shown) Toast.makeText(this@SectionActivity, ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchAndRender(source: Source): Boolean {
        val snapshot = Firebase.firestore.collection("subjects").get(source).await()
        if (source == Source.CACHE && snapshot.isEmpty) return false
        val subjects = snapshot.toObjects<Subject>().filter { it.active && it.section == section }
        val needsChapters = section == "private" || section == "public"
        if (needsChapters) {
            val chapters = chaptersRepository.getChaptersForSection(section, source)
            if (source == Source.CACHE && chapters.isEmpty()) return false
            val sig = Pair(subjects, chapters)
            if (sig == lastRenderSig) return true
            lastRenderSig = sig
            applyChapterRows(subjects, chapters)
        } else {
            if (subjects == lastRenderSig) return true
            lastRenderSig = subjects
            val sorted = subjects.sortedBy { it.orderIndex }
            subjectAdapter.updateData(sorted)
            binding.rvSubjects.visibility = View.VISIBLE
            binding.rvChapters.visibility = View.GONE
            binding.tvEmptySubjects.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        }
        return true
    }

    private fun applyChapterRows(subjects: List<Subject>, chapters: List<Chapter>) {
        val rows = chapters.mapNotNull { chapter ->
            val list = subjects.filter { it.chapterId == chapter.id }.sortedBy { it.orderIndex }
            if (list.isEmpty()) null else ChapterWithSubjects(chapter, list)
        }
        chapterRowAdapter.updateData(rows)
        binding.rvChapters.visibility = View.VISIBLE
        binding.rvSubjects.visibility = View.GONE
        binding.tvEmptySubjects.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openSubject(subject: Subject) {
        startActivity(Intent(this, SubjectDetailActivity::class.java)
            .putExtra("subjectId", subject.id)
            .putExtra("subjectName", subject.name))
    }
}
