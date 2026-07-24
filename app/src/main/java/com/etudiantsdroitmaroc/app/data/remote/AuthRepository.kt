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

            val userDocRef = firestore.collection("users").document(user.uid)
            val existingDoc = userDocRef.get().await()
            val existingPhoto = existingDoc.getString("photoUrl")

            // ما نبدلوش الصورة المخصصة اللي رفعها المستخدم بيده - غير أول مرة كنحطو صورة Google
            val photoToUse = if (!existingPhoto.isNullOrEmpty()) existingPhoto else (user.photoUrl?.toString() ?: "")

            val profile = UserProfile(
                uid = user.uid,
                name = user.displayName ?: "",
                email = user.email ?: "",
                photoUrl = photoToUse,
                isOnline = true
            )
            // merge() ضروري: بلا merge، set() كان كيمسح fcmToken (ومعلومات أخرى بحال الجامعة/المستوى)
            // كل مرة يدخل فيها المستخدم من جديد، لأن UserProfile ماعندهاش هاد الحقول
            userDocRef.set(profile, com.google.firebase.firestore.SetOptions.merge()).await()

            // نجيبو التوكن الحالي ونحفظوه ديركت - onNewToken() فـ NotificationService وحدو ماكافيش
            // لأن الطوكن كيتخلق مرة وحدة ملي يبدا التطبيق (قبل تسجيل الدخول)، وماغاديش يتعاود
            try {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                userDocRef.update("fcmToken", token).await()
            } catch (e: Exception) {
                // ما تكسرش تسجيل الدخول إلا فشل حفظ التوكن
            }

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
