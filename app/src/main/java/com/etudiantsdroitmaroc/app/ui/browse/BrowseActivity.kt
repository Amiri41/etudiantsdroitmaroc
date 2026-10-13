package com.etudiantsdroitmaroc.app.ui.browse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.Chapter
import com.etudiantsdroitmaroc.app.data.model.Subject
import com.etudiantsdroitmaroc.app.data.remote.ChaptersRepository
import com.etudiantsdroitmaroc.app.data.remote.VideoChaptersRepository
import com.etudiantsdroitmaroc.app.data.remote.VideoSubjectsRepository
import com.etudiantsdroitmaroc.app.databinding.ActivityBrowseBinding
import com.etudiantsdroitmaroc.app.ui.lessons.LessonsActivity
import com.etudiantsdroitmaroc.app.utils.BannerAdHelper
import com.etudiantsdroitmaroc.app.utils.ErrorUtils
import com.etudiantsdroitmaroc.app.utils.goHome
import android.widget.TextView
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * صفحة تنقل عامة بأزرار عمودية، كتنزل درجة درجة:
 * القسم ← السنة (إلا كانت) ← السداسي ← المادة ← (LessonsActivity: بطاقات المحتوى)
 * نفس الصفحة كتخدم لشجرة الـ PDF ولشجرة الفيديوهات (tree).
 */
class BrowseActivity : AppCompatActivity() {

    private data class Node(
        val id: String,
        val name: String,
        val chapterId: String,
        val orderIndex: Int
    )

    private lateinit var binding: ActivityBrowseBinding
    private val chaptersRepository = ChaptersRepository()
    private val videoChaptersRepository = VideoChaptersRepository()
    private val videoSubjectsRepository = VideoSubjectsRepository()

    private var level = LEVEL_ROOT
    private var tree = TREE_PDF
    private var section = ""
    private var year = ""
    private var chapterId = ""
    private var lastSig: Any? = null

    private val usesChapters get() = section == "private" || section == "public"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        level = intent.getStringExtra(EXTRA_LEVEL) ?: LEVEL_ROOT
        tree = intent.getStringExtra(EXTRA_TREE) ?: TREE_PDF
        section = intent.getStringExtra(EXTRA_SECTION) ?: ""
        year = intent.getStringExtra(EXTRA_YEAR) ?: ""
        chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID) ?: ""

        binding.toolbar.title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnHome.setOnClickListener { goHome() }
        BannerAdHelper.attach(this, binding.bannerAdContainer)

        if (level == LEVEL_VIDEOS_ROOT) showVideoSections() else load()
    }

    /** أول صفحة ديال الفيديوهات: القانون الخاص / القانون العام */
    private fun showVideoSections() {
        resetButtons()
        listOf("private" to "⚖️ القانون الخاص", "public" to "📜 القانون العام").forEach { (key, label) ->
            addButton(label) { start(this, LEVEL_ROOT, label, key, tree = TREE_VIDEOS) }
        }
        finishButtons()
    }

    /** عرض فوري من الكاش، وبعدها تحديث من السيرفر (كيتعاود الرسم غير إلا تبدل شي) */
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
                if (!shown) Toast.makeText(this@BrowseActivity, ErrorUtils.friendly(e), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchAndRender(source: Source): Boolean {
        val isVideos = tree == TREE_VIDEOS

        val allNodes: List<Node> = if (isVideos) {
            videoSubjectsRepository.getAllActive(source)
                .filter { it.section == section }
                .map { Node(it.id, it.name, it.chapterId, it.orderIndex) }
        } else {
            Firebase.firestore.collection("subjects").get(source).await()
                .toObjects<Subject>()
                .filter { it.active && it.section == section }
                .map { Node(it.id, it.name, it.chapterId, it.orderIndex) }
        }
        val nodes = allNodes.sortedBy { it.orderIndex }

        val needChapters = usesChapters && level != LEVEL_SUBJECTS
        val chapters = if (!needChapters) emptyList() else if (isVideos) {
            videoChaptersRepository.getChaptersForSection(section, source)
        } else {
            chaptersRepository.getChaptersForSection(section, source)
        }

        // الكاش خاوي: نتسناو السيرفر بلا ما نبينو صفحة خاوية
        if (source == Source.CACHE && nodes.isEmpty()) return false
        if (source == Source.CACHE && needChapters && chapters.isEmpty()) return false

        val sig = Pair(nodes, chapters)
        if (sig == lastSig) return true
        lastSig = sig
        render(nodes, chapters)
        return true
    }

    private fun render(nodes: List<Node>, chapters: List<Chapter>) {
        when {
            // ماستر / دكتوراه: كيدوزو مباشرة للمواد
            !usesChapters -> showSubjects(nodes)
            level == LEVEL_SUBJECTS -> showSubjects(nodes.filter { it.chapterId == chapterId })
            else -> {
                // نخبيو السداسيات الفارغة (بلا مواد)
                val withSubjects = chapters.filter { ch -> nodes.any { it.chapterId == ch.id } }
                if (level == LEVEL_SEMESTERS) {
                    showChapters(withSubjects.filter { it.year == year })
                } else if (withSubjects.any { it.year.isNotBlank() }) {
                    showYears(withSubjects)
                } else {
                    showChapters(withSubjects)
                }
            }
        }
    }

    private fun showYears(chapters: List<Chapter>) {
        val years = chapters.map { it.year }.distinct() // كيحافظ على الترتيب (chapters مرتبة بـ orderIndex)
        resetButtons()
        years.forEach { y ->
            val label = if (y.isBlank()) "📖 باقي السداسيات" else "📚 $y"
            addButton(label) {
                start(this, LEVEL_SEMESTERS, if (y.isBlank()) "باقي السداسيات" else y, section, year = y, tree = tree)
            }
        }
        finishButtons()
    }

    private fun showChapters(chapters: List<Chapter>) {
        resetButtons()
        chapters.forEach { ch ->
            addButton("📖 ${ch.name}") {
                start(this, LEVEL_SUBJECTS, ch.name, section, year = year, chapterId = ch.id, tree = tree)
            }
        }
        finishButtons()
    }

    private fun showSubjects(nodes: List<Node>) {
        resetButtons()
        nodes.forEach { n ->
            addButton("📝 ${n.name}") {
                val intent = Intent(this, LessonsActivity::class.java)
                intent.putExtra("subjectName", n.name)
                if (tree == TREE_VIDEOS) intent.putExtra("videoSubjectId", n.id)
                else intent.putExtra("subjectId", n.id)
                startActivity(intent)
            }
        }
        finishButtons()
    }

    private fun resetButtons() = binding.buttonsContainer.removeAllViews()

    private fun finishButtons() {
        binding.tvEmpty.visibility = if (binding.buttonsContainer.childCount == 0) View.VISIBLE else View.GONE
    }

    private fun addButton(text: String, onClick: () -> Unit) {
        val card = layoutInflater.inflate(R.layout.item_section_card, binding.buttonsContainer, false)
        card.findViewById<TextView>(R.id.tvSectionTitle).text = text
        card.setOnClickListener { onClick() }
        binding.buttonsContainer.addView(card)
    }

    companion object {
        const val LEVEL_ROOT = "root"
        const val LEVEL_SEMESTERS = "semesters"
        const val LEVEL_SUBJECTS = "subjects"
        const val LEVEL_VIDEOS_ROOT = "videos_root"

        const val TREE_PDF = "pdf"
        const val TREE_VIDEOS = "videos"

        private const val EXTRA_LEVEL = "level"
        private const val EXTRA_TREE = "tree"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_SECTION = "section"
        private const val EXTRA_YEAR = "year"
        private const val EXTRA_CHAPTER_ID = "chapterId"

        fun start(
            context: Context,
            level: String,
            title: String,
            section: String = "",
            year: String = "",
            chapterId: String = "",
            tree: String = TREE_PDF
        ) {
            val intent = Intent(context, BrowseActivity::class.java)
                .putExtra(EXTRA_LEVEL, level)
                .putExtra(EXTRA_TREE, tree)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_SECTION, section)
                .putExtra(EXTRA_YEAR, year)
                .putExtra(EXTRA_CHAPTER_ID, chapterId)
            context.startActivity(intent)
        }
    }
}
