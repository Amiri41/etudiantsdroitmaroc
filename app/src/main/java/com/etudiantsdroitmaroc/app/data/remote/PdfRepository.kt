package com.etudiantsdroitmaroc.app.data.remote

import android.content.Context
import com.etudiantsdroitmaroc.app.data.local.AppDatabase
import com.etudiantsdroitmaroc.app.data.local.DownloadedPdfEntity
import com.etudiantsdroitmaroc.app.data.model.PdfDocument
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * كيدير join بين Firestore (لائحة PDFs الأونلاين) و Room (الملفات المحملة أوفلاين).
 * - PDF ما تحملش بعد: كيتقرا مباشرة من storageUrl (يحتاج انترنت)
 * - PDF متحمل: كيتفتح من الملف المحلي (بلا انترنت)
 */
class PdfRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val firestore = Firebase.firestore
    private val httpClient = OkHttpClient()

    suspend fun getPdfsForSubject(subjectId: String): List<PdfDocument> {
        val snapshot = firestore.collection("subjects")
            .document(subjectId)
            .collection("pdfs")
            .get()
            .await()
        return snapshot.toObjects()
    }

    suspend fun isDownloaded(pdfId: String): Boolean {
        return db.downloadedPdfDao().getById(pdfId) != null
    }

    suspend fun getLocalPath(pdfId: String): String? {
        return db.downloadedPdfDao().getById(pdfId)?.localFilePath
    }

    /** تحميل PDF من Firebase Storage وتخزينه محليا باش يتقرا بلا انترنت */
    suspend fun downloadForOffline(pdf: PdfDocument): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(pdf.storageUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("فشل التحميل: ${response.code}"))
            }

            val pdfDir = File(context.filesDir, "offline_pdfs")
            if (!pdfDir.exists()) pdfDir.mkdirs()
            val localFile = File(pdfDir, "${pdf.id}.pdf")

            response.body?.byteStream()?.use { input ->
                localFile.outputStream().use { output -> input.copyTo(output) }
            }

            db.downloadedPdfDao().insert(
                DownloadedPdfEntity(
                    pdfId = pdf.id,
                    subjectId = pdf.subjectId,
                    title = pdf.title,
                    localFilePath = localFile.absolutePath
                )
            )

            Result.success(localFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOfflineCopy(pdfId: String) = withContext(Dispatchers.IO) {
        db.downloadedPdfDao().getById(pdfId)?.let { entity ->
            File(entity.localFilePath).delete()
            db.downloadedPdfDao().deleteById(pdfId)
        }
    }
}
