package com.etudiantsdroitmaroc.app.utils

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

    private const val WORKER_URL = "https://etudiants-notify.wamiri459.workers.dev"
    private const val APP_SECRET = "etudiants-x9k2m4p7-secret-2026"

    private val client = OkHttpClient()

    /** كيبعت إشعار بصفة صامتة (بلا ما يوقف التطبيق إلا فشل) */
    suspend fun sendNotification(toToken: String, title: String, message: String) {
        if (toToken.isBlank()) return
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

                client.newCall(request).execute().close()
            } catch (e: Exception) {
                // ما تكسرش التطبيق إلا فشل الإشعار، الرسالة ديما كتبعت مزيان
            }
        }
    }
}
