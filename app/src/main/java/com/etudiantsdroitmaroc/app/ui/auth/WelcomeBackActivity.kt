package com.etudiantsdroitmaroc.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.ui.MainActivity
import com.etudiantsdroitmaroc.app.utils.setupWelcomeWindow
import com.google.firebase.auth.FirebaseAuth

/**
 * شاشة "مرحباً بعودتك": كتبان للمستخدم اللي عندو جلسة Firebase ديجا.
 * - "متابعة" كتفتح الشاشة الرئيسية.
 * - إلا ما كانتش جلسة (مثلا بعد تسجيل الخروج) كنرجعو لشاشة الدخول.
 */
class WelcomeBackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // حماية: بلا مستخدم مسجل ← شاشة الدخول (بلا حلقات توجيه)
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupWelcomeWindow()
        setContentView(R.layout.activity_welcome_back)

        findViewById<View>(R.id.btnContinue).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val top = findViewById<View>(R.id.welcomeTop)
        val footer = findViewById<View>(R.id.welcomeFooter)
        val topPad = top.paddingTop
        val footerHeight = footer.layoutParams.height
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.welcomeRoot)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            top.updatePadding(top = topPad + bars.top)
            footer.updateLayoutParams { height = footerHeight + bars.bottom }
            WindowInsetsCompat.CONSUMED
        }
    }
}
