package com.etudiantsdroitmaroc.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etudiantsdroitmaroc.app.data.model.AppSectionsConfig
import com.etudiantsdroitmaroc.app.data.remote.AppConfigRepository
import com.etudiantsdroitmaroc.app.databinding.FragmentHomeBinding
import com.etudiantsdroitmaroc.app.ui.videos.VideosHomeActivity
import com.etudiantsdroitmaroc.app.utils.AdIds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch

/** الشاشة الرئيسية: 5 أزرار عمودية عريضة، كل واحد كيفتح قسمو */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val appConfigRepository = AppConfigRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBannerAd()

        // marquee كيتحرك غير إلا كان النص كبر من العرض
        listOf(binding.btnVideos, binding.btnPrivate, binding.btnPublic, binding.btnMaster, binding.btnPhd)
            .forEach { it.isSelected = true }

        binding.btnVideos.setOnClickListener {
            startActivity(Intent(requireContext(), VideosHomeActivity::class.java))
        }
        binding.btnPrivate.setOnClickListener { openSection("private", "⚖️ القانون الخاص") }
        binding.btnPublic.setOnClickListener { openSection("public", "🏛️ القانون العام") }
        binding.btnMaster.setOnClickListener { openSection("master", "🎓 بحوث الماستر") }
        binding.btnPhd.setOnClickListener { openSection("phd", "🎓 بحوث الدكتوراه") }

        viewLifecycleOwner.lifecycleScope.launch {
            // فوري من الكاش، ومن بعد تحديث من السيرفر
            val cached = appConfigRepository.getCachedSectionsConfig()
            applyConfig(cached ?: AppSectionsConfig())
            val fresh = appConfigRepository.getFreshSectionsConfig()
            if (fresh != null && fresh != cached) applyConfig(fresh)
        }
    }

    private fun applyConfig(c: AppSectionsConfig) {
        val b = _binding ?: return
        b.btnVideos.visibility = if (c.showVideos) View.VISIBLE else View.GONE
        b.btnPrivate.visibility = if (c.showPrivate) View.VISIBLE else View.GONE
        b.btnPublic.visibility = if (c.showPublic) View.VISIBLE else View.GONE
        b.btnMaster.visibility = if (c.showMaster) View.VISIBLE else View.GONE
        b.btnPhd.visibility = if (c.showPhd) View.VISIBLE else View.GONE
    }

    private fun openSection(section: String, title: String) {
        // الماستر والدكتوراه: PDF مباشرة. الخاص والعام: فصول ← مواد ← PDF
        val target = if (section == "master" || section == "phd")
            com.etudiantsdroitmaroc.app.ui.subject.SubjectDetailActivity::class.java
        else SectionActivity::class.java
        startActivity(Intent(requireContext(), target)
            .putExtra("section", section)
            .putExtra("title", title))
    }

    private fun setupBannerAd() {
        val adView = AdView(requireContext())
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = AdIds.BANNER
        binding.bannerAdContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
