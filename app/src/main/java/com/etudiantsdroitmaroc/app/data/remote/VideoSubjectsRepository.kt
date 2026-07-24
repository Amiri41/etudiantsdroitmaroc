package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.VideoSubject
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/** مواد خاصة بشجرة الفيديوهات فقط - مستقلة تماما عن مواد الـ PDF */
class VideoSubjectsRepository {

    private val firestore = Firebase.firestore

    suspend fun getAllActive(): List<VideoSubject> {
        val snapshot = firestore.collection("videoSubjects").get().await()
        return snapshot.toObjects<VideoSubject>().filter { it.active }
    }
}
