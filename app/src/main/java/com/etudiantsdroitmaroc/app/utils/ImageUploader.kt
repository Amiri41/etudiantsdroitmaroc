package com.etudiantsdroitmaroc.app.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

/**
 * كيرفع الصور لـ imgbb.com (خدمة مجانية بلا بطاقة بنكية) - بديل لـ Firebase Storage.
 * خاصك تبدل IMGBB_API_KEY بالمفتاح ديالك من api.imgbb.com
 */
object ImageUploader {

    private const val IMGBB_API_KEY = "REPLACE_WITH_YOUR_IMGBB_KEY"
    private val client = OkHttpClient()

    suspend fun uploadImage(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "upload_temp.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image", tempFile.name,
                    tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=$IMGBB_API_KEY")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("فشل الرفع: ${response.code}"))
            }

            val json = JSONObject(bodyStr)
            val url = json.getJSONObject("data").getString("url")
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
