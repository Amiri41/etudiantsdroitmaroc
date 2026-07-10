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
                    val errMsg = signInResult.exceptionOrNull()?.message ?: "غير معروف"
                    Toast.makeText(this@LoginActivity, "فشل Firebase: $errMsg", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: ApiException) {
            val codeStr = GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)
            Toast.makeText(this, "خطأ Google [${e.statusCode}]: $codeStr", Toast.LENGTH_LONG).show()
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
