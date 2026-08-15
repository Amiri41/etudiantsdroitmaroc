package com.etudiantsdroitmaroc.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.etudiantsdroitmaroc.app.data.model.AppSectionsConfig
import com.etudiantsdroitmaroc.app.data.model.Chapter
import com.etudiantsdroitmaroc.app.data.model.Subject
import com.etudiantsdroitmaroc.app.data.remote.AppConfigRepository
import com.etudiantsdroitmaroc.app.data.remote.ChaptersRepository
import com.etudiantsdroitmaroc.app.databinding.FragmentHomeBinding
import com.etudiantsdroitmaroc.app.ui.subject.SubjectDetailActivity
import com.etudiantsdroitmaroc.app.ui.videos.VideosHomeActivity
import com.etudiantsdroitmaroc.app.utils.AdIds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val chaptersRepository = ChaptersRepository()
    private val appConfigRepository = AppConfigRepository()

    private lateinit var subjectAdapter: SubjectAdapter
    private lateinit var chapterRowAdapter: ChapterRowAdapter
    private var allSubjects: List<Subject> = emptyList()
    private var currentSection: String = "private"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBannerAd()

        subjectAdapter = SubjectAdapter(emptyList()) { openSubject(it) }
        binding.rvSubjects.layoutManager = GridLayoutManager(context, 2)
        binding.rvSubjects.setHasFixedSize(true)
        binding.rvSubjects.adapter = subjectAdapter

        chapterRowAdapter = ChapterRowAdapter(emptyList()) { openSubject(it) }
        binding.rvChapters.layoutManager = LinearLayoutManager(context)
        binding.rvChapters.setHasFixedSize(true)
        binding.rvChapters.adapter = chapterRowAdapter

        binding.btnVideosEntry.setOnClickListener {
            startActivity(Intent(requireContext(), VideosHomeActivity::class.java))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val config = appConfigRepository.getSectionsConfig()
            buildSectionChips(config)
            loadSubjectsAndChapters()
        }
    }

    private fun setupBannerAd() {
        val adView = AdView(requireContext())
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = AdIds.BANNER
        binding.bannerAdContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    /** كنبنيو أزرار الأقسام الرئيسية ديناميكيا - غير الأقسام اللي فعلها الأدمين كتبان */
    private fun buildSectionChips(config: AppSectionsConfig) {
        binding.chipGroupSection.removeAllViews()

        data class SectionItem(val key: String, val label: String, val visible: Boolean)

        val items = listOf(
            SectionItem("private", "⚖️ القانون الخاص", config.showPrivate),
            SectionItem("public", "📜 القانون العام", config.showPublic),
            SectionItem("master", "🎓 بحوث الماستر", config.showMaster),
            SectionItem("phd", "🏆 بحوث الدكتوراه", config.showPhd)
        ).filter { it.visible }

        binding.btnVideosEntry.visibility = if (config.showVideos) View.VISIBLE else View.GONE

        if (items.isEmpty()) return

        var firstContentChip: Chip? = null

        items.forEach { item ->
            val chip = Chip(requireContext())
            chip.text = item.label
            chip.isCheckable = true
            chip.isClickable = true

            if (firstContentChip == null) firstContentChip = chip
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currentSection = item.key
                    loadSubjectsAndChapters()
                }
            }
            binding.chipGroupSection.addView(chip)
        }

        firstContentChip?.isChecked = true
    }

    private fun loadSubjectsAndChapters() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val snapshot = Firebase.firestore.collection("subjects").get().await()
                allSubjects = snapshot.toObjects<Subject>().filter { it.active }

                val needsChapters = currentSection == "private" || currentSection == "public"

                if (needsChapters) {
                    val chapters = chaptersRepository.getChaptersForSection(currentSection)
                    applyChapterRows(chapters)
                } else {
                    applyFlatSubjects()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "خطأ Firestore: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    /** كنعرضو كل فصل بعنوانو والمواد ديالو مصفوفة أفقيا تحتو (بحال Netflix) */
    private fun applyChapterRows(chapters: List<Chapter>) {
        val rows = chapters.mapNotNull { chapter ->
            val subjectsForChapter = allSubjects
                .filter { it.section == currentSection && it.chapterId == chapter.id }
                .sortedBy { it.orderIndex }
            if (subjectsForChapter.isEmpty()) null else ChapterWithSubjects(chapter, subjectsForChapter)
        }

        chapterRowAdapter.updateData(rows)
        binding.rvChapters.visibility = View.VISIBLE
        binding.rvSubjects.visibility = View.GONE
        binding.tvEmptySubjects.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    /** الأقسام اللي ماعندهاش فصول (ماستر/دكتوراه) كتبان بشكل شبكة عادية */
    private fun applyFlatSubjects() {
        val filtered = allSubjects.filter { it.section == currentSection }.sortedBy { it.orderIndex }

        subjectAdapter.updateData(filtered)
        binding.rvSubjects.visibility = View.VISIBLE
        binding.rvChapters.visibility = View.GONE
        binding.tvEmptySubjects.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openSubject(subject: Subject) {
        val intent = Intent(requireContext(), SubjectDetailActivity::class.java)
        intent.putExtra("subjectId", subject.id)
        intent.putExtra("subjectName", subject.name)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
