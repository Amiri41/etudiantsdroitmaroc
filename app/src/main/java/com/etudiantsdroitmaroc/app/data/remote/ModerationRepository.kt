package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.Report
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * كنستعملوه للإبلاغ عن محتوى (منشورات/تعليقات/رسائل/مستخدمين).
 * الحظر (block) كاين ديجا فـ ChatRepository (نفس المسار users/{uid}/blocked) - كنستعملوه هو باش نبقاو متوافقين.
 */
class ModerationRepository {

    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        targetOwnerUid: String,
        targetOwnerName: String,
        reason: String,
        extraDetails: String = ""
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("خاصك تسجل الدخول"))
        return try {
            val report = Report(
                reporterUid = user.uid,
                reporterName = user.displayName ?: "",
                targetType = targetType,
                targetId = targetId,
                targetOwnerUid = targetOwnerUid,
                targetOwnerName = targetOwnerName,
                reason = reason,
                extraDetails = extraDetails
            )
            val docRef = firestore.collection("reports").document()
            report.id = docRef.id
            docRef.set(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

