package com.etudiantsdroitmaroc.app.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.databinding.ActivityOnboardingBinding
import com.etudiantsdroitmaroc.app.databinding.ItemOnboardingPageBinding
import com.etudiantsdroitmaroc.app.ui.auth.LoginActivity

/** كنعرضو هاد الصفحة مرة وحدة بركة للمستخدمين الجداد قبل صفحة تسجيل الدخول */
class OnboardingActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"

        fun hasSeenOnboarding(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_SHOWN, false)
        }

        private fun markAsSeen(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ONBOARDING_SHOWN, true).apply()
        }
    }

    private data class Page(val emoji: String, val title: String, val description: String)

    private val pages = listOf(
        Page(
            "⚖️",
            "مرحبا بيك فـ ÉTUDIANTS EN DROIT MAROC",
            "منصة اجتماعية وتعليمية كاملة لطلبة القانون بالمغرب، كل شي محتاجو فبلاصة واحدة."
        ),
        Page(
            "📚",
            "مواد، ملخصات، وفيديوهات",
            "مواد مرتبة حسب الفصول، ملخصات PDF جاهزة، وفيديوهات شرح لكل مادة."
        ),
        Page(
            "💬",
            "مجتمع طلبة كامل",
            "دردش مع زملائك، شارك فالمنتدى، وتبادل المعرفة مع طلبة القانون فكل المغرب."
        )
    )

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var dots: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = OnboardingPagerAdapter()
        setupDots()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                binding.btnNext.text = if (position == pages.size - 1) "بدا الآن" else "التالي"
            }
        })

        binding.btnNext.setOnClickListener {
            val next = binding.viewPager.currentItem + 1
            if (next < pages.size) {
                binding.viewPager.currentItem = next
            } else {
                finishOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun setupDots() {
        binding.dotsIndicator.removeAllViews()
        dots = pages.indices.map {
            View(this).apply {
                val size = (8 * resources.displayMetrics.density).toInt()
                val params = android.widget.LinearLayout.LayoutParams(size, size)
                params.marginStart = (4 * resources.displayMetrics.density).toInt()
                params.marginEnd = (4 * resources.displayMetrics.density).toInt()
                layoutParams = params
                setBackgroundResource(R.drawable.bg_circle_white)
            }
        }
        dots.forEach { binding.dotsIndicator.addView(it) }
        updateDots(0)
    }

    private fun updateDots(activeIndex: Int) {
        dots.forEachIndexed { index, dot ->
            dot.alpha = if (index == activeIndex) 1f else 0.3f
        }
    }

    private fun finishOnboarding() {
        markAsSeen(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private inner class OnboardingPagerAdapter : RecyclerView.Adapter<OnboardingPagerAdapter.VH>() {
        inner class VH(val binding: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages[position]
            holder.binding.tvEmoji.text = page.emoji
            holder.binding.tvTitle.text = page.title
            holder.binding.tvDescription.text = page.description
        }

        override fun getItemCount() = pages.size
    }
}
