package com.etudiantsdroitmaroc.app.ui.home

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
import android.content.Intent

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var privateAdapter: SubjectAdapter
    private lateinit var publicAdapter: SubjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBannerAd()

        privateAdapter = SubjectAdapter(emptyList()) { openSubject(it) }
        publicAdapter = SubjectAdapter(emptyList()) { openSubject(it) }

        binding.rvPrivateLaw.layoutManager = GridLayoutManager(context, 2)
        binding.rvPrivateLaw.adapter = privateAdapter

        binding.rvPublicLaw.layoutManager = GridLayoutManager(context, 2)
        binding.rvPublicLaw.adapter = publicAdapter

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
            val snapshot = Firebase.firestore.collection("subjects").get().await()
            val subjects: List<Subject> = snapshot.toObjects()
            privateAdapter.updateData(subjects.filter { it.category == "private" })
            publicAdapter.updateData(subjects.filter { it.category == "public" })
        }
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
