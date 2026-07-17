package com.etudiantsdroitmaroc.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.etudiantsdroitmaroc.app.data.model.Subject
import com.etudiantsdroitmaroc.app.databinding.FragmentHomeBinding
import com.etudiantsdroitmaroc.app.ui.subject.SubjectDetailActivity
import com.etudiantsdroitmaroc.app.utils.AdIds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var subjectAdapter: SubjectAdapter
    private var allSubjects: List<Subject> = emptyList()

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
        binding.rvSubjects.adapter = subjectAdapter

        binding.chipGroupSection.setOnCheckedStateChangeListener { _, _ -> applyFilters() }
        binding.chipGroupSemester.setOnCheckedStateChangeListener { _, _ -> applyFilters() }

        binding.btnVideos.setOnClickListener {
            startActivity(Intent(requireContext(), com.etudiantsdroitmaroc.app.ui.videos.VideosListActivity::class.java))
        }

        loadSubjects()
    }

    private fun setupBannerAd() {
        val adView = AdView(requireContext())
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = AdIds.BANNER
        binding.bannerAdContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun loadSubjects() {
        lifecycleScope.launch {
            try {
                val snapshot = Firebase.firestore.collection("subjects").get().await()
                allSubjects = snapshot.toObjects<Subject>().filter { it.active }
                applyFilters()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "خطأ Firestore: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun currentSection(): String = when (binding.chipGroupSection.checkedChipId) {
        binding.chipPrivate.id -> "private"
        binding.chipPublic.id -> "public"
        binding.chipMaster.id -> "master"
        binding.chipPhd.id -> "phd"
        binding.chipGeneral.id -> "general"
        else -> "private"
    }

    private fun currentSemester(): Int = when (binding.chipGroupSemester.checkedChipId) {
        binding.chipS1.id -> 1
        binding.chipS2.id -> 2
        binding.chipS3.id -> 3
        binding.chipS4.id -> 4
        binding.chipS5.id -> 5
        binding.chipS6.id -> 6
        else -> 1
    }

    private fun applyFilters() {
        val section = currentSection()
        val needsSemester = section == "private" || section == "public"
        binding.semesterScroll.visibility = if (needsSemester) View.VISIBLE else View.GONE

        val filtered = if (needsSemester) {
            val semester = currentSemester()
            allSubjects.filter { it.section == section && it.semester == semester }
        } else {
            allSubjects.filter { it.section == section }
        }

        subjectAdapter.updateData(filtered.sortedBy { it.orderIndex })
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
