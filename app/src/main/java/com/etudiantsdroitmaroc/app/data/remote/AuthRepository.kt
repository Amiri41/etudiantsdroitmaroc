package com.etudiantsdroitmaroc.app.data.remote

import android.content.Context
import android.content.Intent
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * تسجيل الدخول بحساب Google فقط (بحسب طلب وليد - بلا رقم الهاتف)
 */
class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    val currentUser get() = auth.currentUser

    fun getGoogleSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(Exception("فشل تسجيل الدخول"))

            // حفظ/تحديث البروفايل فـ Firestore
            val profile = UserProfile(
                uid = user.uid,
                name = user.displayName ?: "",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: "",
                isOnline = true
            )
            firestore.collection("users").document(user.uid).set(profile).await()

            // الاشتراك فـ topic عام باش يوصلو إشعارات (منشورات جديدة، تحديثات...)
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .subscribeToTopic("all_students")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        ).signOut()
    }
}
