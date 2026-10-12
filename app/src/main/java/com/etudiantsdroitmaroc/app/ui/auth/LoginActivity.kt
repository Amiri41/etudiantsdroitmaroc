package com.etudiantsdroitmaroc.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.remote.AuthRepository
import com.etudiantsdroitmaroc.app.ui.MainActivity
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
        setContentView(R.layout.activity_login)

        authRepository = AuthRepository(this)

        findViewById<android.view.View>(R.id.btnGoogleSignIn).setOnClickListener {
            googleSignInLauncher.launch(authRepository.getGoogleSignInIntent())
        }
    }
}
