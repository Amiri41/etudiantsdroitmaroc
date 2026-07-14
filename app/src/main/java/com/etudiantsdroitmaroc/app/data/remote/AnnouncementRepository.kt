package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.AppAnnouncement
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/** كيجيب البانر/الإعلان الحالي المتحكم فيه من لوحة التحكم */
object AnnouncementRepository {

    suspend fun getCurrentAnnouncement(): AppAnnouncement? {
        return try {
            val snap = Firebase.firestore.collection("app_announcement")
                .document("current").get().await()
            if (!snap.exists()) return null
            snap.toObject(AppAnnouncement::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
