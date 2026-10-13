package com.etudiantsdroitmaroc.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.AppSectionsConfig
import com.etudiantsdroitmaroc.app.data.remote.AppConfigRepository
import com.etudiantsdroitmaroc.app.databinding.FragmentHomeBinding
import com.etudiantsdroitmaroc.app.ui.browse.BrowseActivity
import com.etudiantsdroitmaroc.app.utils.AdIds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

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

        binding.btnVideosEntry.setOnClickListener {
            BrowseActivity.start(
                requireContext(),
                level = BrowseActivity.LEVEL_VIDEOS_ROOT,
                title = "🎥 فيديوهات"
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // 1) فوري: نبنيو الأزرار من الكاش المحلي (إلا كان) بلا انتظار الشبكة
            val cached = appConfigRepository.getCachedSectionsConfig()
            var shown: AppSectionsConfig? = cached
            if (cached != null) buildSectionButtons(cached)

            // 2) فالخلفية: نحدثو من السيرفر، ونعاودو البناء غير إلا تبدل شي
            val fresh = appConfigRepository.getFreshSectionsConfig()
            if (shown == null) {
                val cfg = fresh ?: AppSectionsConfig()
                shown = cfg
                buildSectionButtons(cfg)
            } else if (fresh != null && fresh != shown) {
                buildSectionButtons(fresh)
            }
        }
    }

    private fun setupBannerAd() {
        val adView = AdView(requireContext())
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = AdIds.BANNER
        binding.bannerAdContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    /** كنبنيو الأقسام الرئيسية كأزرار عمودية عريضة - غير الأقسام اللي فعلها الأدمين كتبان */
    private fun buildSectionButtons(config: AppSectionsConfig) {
        val b = _binding ?: return
        b.sectionButtons.removeAllViews()

        data class SectionItem(val key: String, val label: String, val visible: Boolean)

        val items = listOf(
            SectionItem("private", "⚖️ القانون الخاص", config.showPrivate),
            SectionItem("public", "📜 القانون العام", config.showPublic),
            SectionItem("master", "🎓 بحوث الماستر", config.showMaster),
            SectionItem("phd", "🏆 بحوث الدكتوراه", config.showPhd)
        ).filter { it.visible }

        b.btnVideosEntry.visibility = if (config.showVideos) View.VISIBLE else View.GONE
        b.tvEmptySubjects.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        items.forEach { item ->
            val button = layoutInflater.inflate(R.layout.item_browse_button, b.sectionButtons, false) as MaterialButton
            button.text = item.label
            button.setOnClickListener {
                BrowseActivity.start(
                    requireContext(),
                    level = BrowseActivity.LEVEL_ROOT,
                    title = item.label,
                    section = item.key
                )
            }
            b.sectionButtons.addView(button)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
