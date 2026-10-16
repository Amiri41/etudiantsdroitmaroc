package com.etudiantsdroitmaroc.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.remote.AuthRepository
import com.etudiantsdroitmaroc.app.ui.MainActivity
import com.etudiantsdroitmaroc.app.utils.setupWelcomeWindow
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            lifecycleScope.launch {
                val signInResult = authRepository.firebaseAuthWithGoogle(account)
                if (signInResult.isSuccess) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    val err = signInResult.exceptionOrNull()
                    val msg = if (err != null) com.etudiantsdroitmaroc.app.utils.ErrorUtils.friendly(err) else "تعذر تسجيل الدخول، عاود المحاولة"
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: ApiException) {
            // 12501 = المستخدم سد النافذة بنفسو، ما كنبينوش ليه خطأ
            if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                Toast.makeText(this, "تعذر الدخول بحساب Google، عاود المحاولة (${e.statusCode})", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWelcomeWindow()
        setContentView(R.layout.activity_login)

        authRepository = AuthRepository(this)

        findViewById<View>(R.id.btnGoogleSignIn).setOnClickListener {
            googleSignInLauncher.launch(authRepository.getGoogleSignInIntent())
        }
        findViewById<View>(R.id.tvTerms).setOnClickListener {
            openUrl("https://amiri41.github.io/etudiantsdroitmaroc/terms.html")
        }
        findViewById<View>(R.id.tvPrivacy).setOnClickListener {
            openUrl("https://amiri41.github.io/etudiantsdroitmaroc/privacy.html")
        }

        // الشاشة كتمتد تحت أشرطة النظام: نزيدو المسافة الفوقانية والسفلية يدويا
        val top = findViewById<View>(R.id.topContainer)
        val footer = findViewById<View>(R.id.loginFooter)
        val topPad = top.paddingTop
        val footerPad = footer.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            top.updatePadding(top = topPad + bars.top)
            footer.updatePadding(bottom = footerPad + bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }
}
