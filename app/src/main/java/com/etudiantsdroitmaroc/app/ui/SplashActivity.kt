package com.etudiantsdroitmaroc.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.databinding.ActivitySplashBinding
import com.etudiantsdroitmaroc.app.ui.auth.LoginActivity
import com.etudiantsdroitmaroc.app.utils.AdManager
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // الشاشة كتمتد تحت أشرطة النظام: نزيدو المسافات يدويا بحال صفحة الدخول
        val top = findViewById<View>(R.id.splashTop)
        val footer = findViewById<View>(R.id.splashFooter)
        val topPad = top.paddingTop
        val footerPad = footer.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashRoot)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            top.updatePadding(top = topPad + bars.top)
            footer.updatePadding(bottom = footerPad + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        com.etudiantsdroitmaroc.app.utils.IntroSound.play(this)
        playLogoAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        // الجينغل 4 ثواني: كيكمل حتى الشاشة الجاية، ما كنوقفوهش هنا
    }

    private fun playLogoAnimation() {
        // ظهور نظيف: تكبير خفيف مع تلاشي، بلا دوران ثلاثي الأبعاد (كان كيخلي الشعار يبان مضبب ومعتم)
        binding.ivLogo.scaleX = 0.85f
        binding.ivLogo.scaleY = 0.85f

        val scaleXIn = ObjectAnimator.ofFloat(binding.ivLogo, "scaleX", 0.85f, 1f)
        val scaleYIn = ObjectAnimator.ofFloat(binding.ivLogo, "scaleY", 0.85f, 1f)
        val fadeIn = ObjectAnimator.ofFloat(binding.ivLogo, "alpha", 0f, 1f)

        val logoSet = AnimatorSet().apply {
            playTogether(scaleXIn, scaleYIn, fadeIn)
            duration = 550
            interpolator = android.view.animation.DecelerateInterpolator(1.6f)
        }

        logoSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                showWelcomeMessage()
            }
        })

        logoSet.start()
    }

    private fun showWelcomeMessage() {
        binding.welcomeGroup.scaleX = 0.7f
        binding.welcomeGroup.scaleY = 0.7f

        val fadeIn = ObjectAnimator.ofFloat(binding.welcomeGroup, "alpha", 0f, 1f)
        val scaleXIn = ObjectAnimator.ofFloat(binding.welcomeGroup, "scaleX", 0.7f, 1f)
        val scaleYIn = ObjectAnimator.ofFloat(binding.welcomeGroup, "scaleY", 0.7f, 1f)

        AnimatorSet().apply {
            playTogether(fadeIn, scaleXIn, scaleYIn)
            duration = 500
            interpolator = OvershootInterpolator(2f)
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 1400)
    }

    private fun navigateNext() {
        // App Open Ad كتبان مرة وحدة عند فتح التطبيق (إلا كانت جاهزة)
        AdManager.showAppOpenAdIfReady(this) {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            val destination = when {
                isLoggedIn -> MainActivity::class.java
                !com.etudiantsdroitmaroc.app.ui.onboarding.OnboardingActivity.hasSeenOnboarding(this) ->
                    com.etudiantsdroitmaroc.app.ui.onboarding.OnboardingActivity::class.java
                else -> LoginActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish()
        }
    }
}
