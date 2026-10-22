package com.etudiantsdroitmaroc.app.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * إرسال الإشعارات عبر Cloudflare Worker.
 * التطبيق كيصيفط الـ Firebase ID token ديال المستعمل (ماشي كلمة سرية ثابتة)،
 * والـ Worker كيتحقق منو، وكيتأكد بلي المرسل فعلا طرف فالمحادثة ولا عندو طلب صداقة،
 * وكيجيب توكن الإشعار بنفسو (التطبيق ما بقاش خاصو يقرا توكنات الآخرين).
 */
object PushNotifier {

    private const val TAG = "PushNotifier"
    private const val WORKER_URL = "https://etudiants-notify.wamiri459.workers.dev"
    private val JSON_TYPE = "application/json".toMediaType()
    private val client = OkHttpClient()

    /** إشعار رسالة جديدة للطرف الآخر فالمحادثة */
    suspend fun sendChat(threadId: String, title: String, message: String, messageId: String = "") {
        post(JSONObject().apply {
            put("kind", "chat")
            put("threadId", threadId)
            put("title", title)
            put("message", message)
            if (messageId.isNotBlank()) put("messageId", messageId)
        })
    }

    /** إشعار طلب صداقة (كيتقبل فقط إلا كان الطلب كاين عند الطرف الآخر) */
    suspend fun sendFriendRequest(toUid: String, title: String, message: String) {
        post(JSONObject().apply {
            put("kind", "friend_request")
            put("toUid", toUid)
            put("title", title)
            put("message", message)
        })
    }

    private suspend fun post(json: JSONObject) {
        withContext(Dispatchers.IO) {
            try {
                val idToken = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                if (idToken.isNullOrBlank()) {
                    Log.w(TAG, "ما كاينش مستعمل مسجل: الإشعار ما تصيفطش")
                    return@withContext
                }
                val request = Request.Builder()
                    .url(WORKER_URL)
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(json.toString().toRequestBody(JSON_TYPE))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "خطأ من Worker: كود ${response.code} - ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "فشل إرسال الإشعار: ${ErrorUtils.friendly(e)}", e)
            }
        }
    }
}
