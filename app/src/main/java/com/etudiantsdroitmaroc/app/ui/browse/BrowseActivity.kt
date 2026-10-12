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
import com.etudiantsdroitmaroc.app.databinding.ActivityBrowseBinding
import com.etudiantsdroitmaroc.app.utils.BannerAdHelper
import com.etudiantsdroitmaroc.app.utils.ErrorUtils
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * صفحة تنقل عامة بأزرار عمودية، كتنزل درجة درجة:
 * القسم ← السنة (إلا كانت) ← السداسي ← المادة ← (LessonsActivity: بطاقات الدروس)
 */
class BrowseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowseBinding
    private val chaptersRepository = ChaptersRepository()

    private var level = LEVEL_ROOT
    private var section = ""
    private var year = ""
    private var chapterId = ""
    private var subjectId = ""
    private var subjectName = ""
    private var lastSig: Any? = null

    private val usesChapters get() = section == "private" || section == "public"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        level = intent.getStringExtra(EXTRA_LEVEL) ?: LEVEL_ROOT
        section = intent.getStringExtra(EXTRA_SECTION) ?: ""
        year = intent.getStringExtra(EXTRA_YEAR) ?: ""
        chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID) ?: ""
        subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: ""
        subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME) ?: ""

        binding.toolbar.title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        binding.toolbar.setNavigationOnClickListener { finish() }
        BannerAdHelper.attach(this, binding.bannerAdContainer)

        load()
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
        val snapshot = Firebase.firestore.collection("subjects").get(source).await()
        if (source == Source.CACHE && snapshot.isEmpty) return false
        val subjects = snapshot.toObjects<Subject>()
            .filter { it.active && it.section == section }
            .sortedBy { it.orderIndex }

        val needChapters = usesChapters && level != LEVEL_SUBJECTS
        val chapters = if (needChapters) chaptersRepository.getChaptersForSection(section, source) else emptyList()
        if (source == Source.CACHE && needChapters && chapters.isEmpty()) return false

        val sig = Pair(subjects, chapters)
        if (sig == lastSig) return true
        lastSig = sig
        render(subjects, chapters)
        return true
    }

    private fun render(subjects: List<Subject>, chapters: List<Chapter>) {
        when {
            // ماستر / دكتوراه: كيدوزو مباشرة للمواد
            !usesChapters -> showSubjects(subjects)
            level == LEVEL_SUBJECTS -> showSubjects(subjects.filter { it.chapterId == chapterId })
            else -> {
                // نخبيو السداسيات الفارغة (بلا مواد)
                val withSubjects = chapters.filter { ch -> subjects.any { it.chapterId == ch.id } }
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
                start(this, LEVEL_SEMESTERS, if (y.isBlank()) "باقي السداسيات" else y, section, year = y)
            }
        }
        finishButtons()
    }

    private fun showChapters(chapters: List<Chapter>) {
        resetButtons()
        chapters.forEach { ch ->
            addButton("📖 ${ch.name}") {
                start(this, LEVEL_SUBJECTS, ch.name, section, year = year, chapterId = ch.id)
            }
        }
        finishButtons()
    }

    private fun showSubjects(subjects: List<Subject>) {
        resetButtons()
        subjects.forEach { s ->
            addButton("📝 ${s.name}") {
                val intent = Intent(this, com.etudiantsdroitmaroc.app.ui.lessons.LessonsActivity::class.java)
                intent.putExtra("subjectId", s.id)
                intent.putExtra("subjectName", s.name)
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
        val button = layoutInflater.inflate(R.layout.item_browse_button, binding.buttonsContainer, false) as MaterialButton
        button.text = text
        button.setOnClickListener { onClick() }
        binding.buttonsContainer.addView(button)
    }

    companion object {
        const val LEVEL_ROOT = "root"
        const val LEVEL_SEMESTERS = "semesters"
        const val LEVEL_SUBJECTS = "subjects"

        private const val EXTRA_LEVEL = "level"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_SECTION = "section"
        private const val EXTRA_YEAR = "year"
        private const val EXTRA_CHAPTER_ID = "chapterId"
        private const val EXTRA_SUBJECT_ID = "subjectId"
        private const val EXTRA_SUBJECT_NAME = "subjectName"

        fun start(
            context: Context,
            level: String,
            title: String,
            section: String,
            year: String = "",
            chapterId: String = "",
            subjectId: String = "",
            subjectName: String = ""
        ) {
            val intent = Intent(context, BrowseActivity::class.java)
                .putExtra(EXTRA_LEVEL, level)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_SECTION, section)
                .putExtra(EXTRA_YEAR, year)
                .putExtra(EXTRA_CHAPTER_ID, chapterId)
                .putExtra(EXTRA_SUBJECT_ID, subjectId)
                .putExtra(EXTRA_SUBJECT_NAME, subjectName)
            context.startActivity(intent)
        }
    }
}
