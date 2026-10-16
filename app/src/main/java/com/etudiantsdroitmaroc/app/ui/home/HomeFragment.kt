package com.etudiantsdroitmaroc.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
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
import com.google.firebase.firestore.Source
import kotlinx.coroutines.launch

/** الشاشة الرئيسية: 5 أزرار عمودية عريضة، كل واحد كيفتح قسمو */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val appConfigRepository = AppConfigRepository()

    private var sectionsConfig = AppSectionsConfig()
    private var order: List<String> = DEFAULT_ORDER
    private var renderedKey: String? = null
    private val adSlots = mutableMapOf<String, FrameLayout>()
    private val adViews = mutableListOf<AdView>()

    companion object {
        /** الترتيب الافتراضي إلا ماكاينش home_layout: إعلان فوق، من بعد الأقسام */
        private val DEFAULT_ORDER = listOf("ad1", "videos", "private", "public", "master", "phd")
        private val AD_SLOTS = (1..7).map { "ad$it" }.toSet()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // marquee كيتحرك غير إلا كان النص كبر من العرض
        listOf(binding.tvVideos, binding.tvPrivate, binding.tvPublic, binding.tvMaster, binding.tvPhd)
            .forEach { it.isSelected = true }

        binding.btnVideos.setOnClickListener {
            startActivity(Intent(requireContext(), VideosHomeActivity::class.java))
        }
        binding.btnPrivate.setOnClickListener { openSection("private", "القانون الخاص") }
        binding.btnPublic.setOnClickListener { openSection("public", "القانون العام") }
        binding.btnMaster.setOnClickListener { openSection("master", "بحوث الماستر") }
        binding.btnPhd.setOnClickListener { openSection("phd", "بحوث الدكتوراه") }

        viewLifecycleOwner.lifecycleScope.launch {
            // فوري من الكاش، ومن بعد تحديث من السيرفر
            appConfigRepository.getCachedSectionsConfig()?.let { sectionsConfig = it }
            appConfigRepository.getHomeOrder(Source.CACHE)?.let { order = it }
            render()
            appConfigRepository.getFreshSectionsConfig()?.let { sectionsConfig = it }
            appConfigRepository.getHomeOrder(Source.SERVER)?.let { order = it }
            render()
        }
    }

    /**
     * كيرتب الأقسام وأماكن الإعلانات (ad1..ad7) حسب home_layout.order.
     * القسم المخفي كيتحيد والباقي كيتزحلق، والإعلانات المتتالية كيبان غير الأول فيهم.
     */
    private fun render() {
        val b = _binding ?: return
        val c = sectionsConfig
        val key = order.joinToString(",") + "|" +
            listOf(c.showVideos, c.showPrivate, c.showPublic, c.showMaster, c.showPhd).joinToString(",")
        if (key == renderedKey) return
        renderedKey = key

        val sections = mapOf(
            "videos" to (b.btnVideos to c.showVideos),
            "private" to (b.btnPrivate to c.showPrivate),
            "public" to (b.btnPublic to c.showPublic),
            "master" to (b.btnMaster to c.showMaster),
            "phd" to (b.btnPhd to c.showPhd)
        )
        val container = b.homeContainer
        container.removeAllViews()
        val seen = mutableSetOf<String>()
        var lastWasAd = false
        for (token in order) {
            if (!seen.add(token)) continue
            val section = sections[token]
            if (section != null) {
                if (section.second) {
                    container.addView(section.first)
                    lastWasAd = false
                }
            } else if (token in AD_SLOTS && !lastWasAd) {
                container.addView(adSlot(token))
                lastWasAd = true
            }
        }
    }

    private fun adSlot(token: String): FrameLayout = adSlots.getOrPut(token) {
        val ctx = requireContext()
        val frame = FrameLayout(ctx)
        frame.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (14 * resources.displayMetrics.density).toInt() }
        val adView = AdView(ctx)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = AdIds.BANNER
        frame.addView(adView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL
        ))
        adViews.add(adView)
        adView.loadAd(AdRequest.Builder().build())
        frame
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

    override fun onResume() {
        super.onResume()
        adViews.forEach { it.resume() }
    }

    override fun onPause() {
        adViews.forEach { it.pause() }
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adViews.forEach { it.destroy() }
        adViews.clear()
        adSlots.clear()
        renderedKey = null
        _binding = null
    }
}
