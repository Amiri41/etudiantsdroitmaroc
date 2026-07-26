package com.etudiantsdroitmaroc.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
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

        playLogoAnimation()
    }

    private fun playLogoAnimation() {
        // كنعطيو عمق للكاميرا باش الدوران يبان بمظهر ثلاثي الأبعاد حقيقي، ماشي مسطح
        binding.ivLogo.cameraDistance = 14000f
        binding.ivLogo.rotationY = 100f
        binding.ivLogo.scaleX = 0.4f
        binding.ivLogo.scaleY = 0.4f

        val rotateIn = ObjectAnimator.ofFloat(binding.ivLogo, "rotationY", 100f, 0f)
        val scaleXIn = ObjectAnimator.ofFloat(binding.ivLogo, "scaleX", 0.4f, 1f)
        val scaleYIn = ObjectAnimator.ofFloat(binding.ivLogo, "scaleY", 0.4f, 1f)
        val fadeIn = ObjectAnimator.ofFloat(binding.ivLogo, "alpha", 0f, 1f)

        val logoSet = AnimatorSet().apply {
            playTogether(rotateIn, scaleXIn, scaleYIn, fadeIn)
            duration = 900
            interpolator = OvershootInterpolator(1.1f)
        }

        // تأثير "طيران" خفيف مستمر بعد الدخول (طفو فوق وتحت)
        val floatUp = ObjectAnimator.ofFloat(binding.ivLogo, "translationY", 0f, -14f).apply {
            duration = 1000
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = 1
        }

        logoSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                floatUp.start()
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
