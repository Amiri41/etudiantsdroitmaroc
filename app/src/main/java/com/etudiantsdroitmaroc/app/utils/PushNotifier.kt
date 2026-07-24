package com.etudiantsdroitmaroc.app.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * كيبعت إشعار (push notification) عبر Cloudflare Worker
 * (بديل مجاني لـ Cloud Functions اللي كتطلب خطة Blaze المدفوعة).
 */
object PushNotifier {

    private const val TAG = "PushNotifier"
    private const val WORKER_URL = "https://etudiants-notify.wamiri459.workers.dev"
    private const val APP_SECRET = "etudiants-x9k2m4p7-secret-2026"

    private val client = OkHttpClient()

    /** كيبعت إشعار بصفة صامتة (بلا ما يوقف التطبيق إلا فشل) */
    suspend fun sendNotification(toToken: String, title: String, message: String) {
        if (toToken.isBlank()) {
            Log.w(TAG, "ما بعتناش الإشعار: fcmToken فارغ (المستقبل ماعندوش توكن محفوظ)")
            return
        }
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("token", toToken)
                    put("title", title)
                    put("message", message)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(WORKER_URL)
                    .addHeader("X-App-Secret", APP_SECRET)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "فشل الـ Worker: كود ${response.code} - ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "خطأ فبعث الإشعار: ${e.message}", e)
            }
        }
    }
}
