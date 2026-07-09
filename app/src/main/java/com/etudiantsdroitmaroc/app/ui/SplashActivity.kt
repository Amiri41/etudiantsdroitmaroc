package com.etudiantsdroitmaroc.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.etudiantsdroitmaroc.app.ui.auth.LoginActivity
import com.etudiantsdroitmaroc.app.utils.AdManager
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 1200)
    }

    private fun navigateNext() {
        // App Open Ad كتبان مرة وحدة عند فتح التطبيق (إلا كانت جاهزة)
        AdManager.showAppOpenAdIfReady(this) {
            val destination = if (FirebaseAuth.getInstance().currentUser != null) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish()
        }
    }
}
