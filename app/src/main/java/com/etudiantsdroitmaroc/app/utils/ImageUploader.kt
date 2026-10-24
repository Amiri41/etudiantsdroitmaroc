package com.etudiantsdroitmaroc.app.utils

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * رفع الصور عبر الـ Worker (كيرفعها لـ imgbb): مفتاح imgbb كيبقى فالـ Worker فقط،
 * والتطبيق كيصيفط الصورة مع الـ Firebase ID token ديال المستعمل.
 */
object ImageUploader {

    private const val UPLOAD_URL = "https://etudiants-notify.wamiri459.workers.dev/upload"
    private const val MAX_BYTES = 8 * 1024 * 1024
    private val client = OkHttpClient()

    suspend fun uploadImage(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val idToken = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                ?: return@withContext Result.failure(Exception("خاصك تسجل الدخول باش ترفع صورة"))

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("تعذر قراءة الصورة"))
            if (bytes.size > MAX_BYTES) {
                return@withContext Result.failure(Exception("الصورة كبيرة بزاف (أقل من 8MB)"))
            }
            val mime = (context.contentResolver.getType(uri) ?: "image/jpeg").takeIf { it.startsWith("image/") }
                ?: "image/jpeg"

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", "Bearer $idToken")
                .post(bytes.toRequestBody(mime.toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("فشل الرفع: ${response.code}"))
                }
                val url = JSONObject(bodyStr).getString("url")
                Result.success(url)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }
}
