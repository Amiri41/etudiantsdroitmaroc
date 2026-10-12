package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.VideoSubject
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/** مواد خاصة بشجرة الفيديوهات فقط - مستقلة تماما عن مواد الـ PDF */
class VideoSubjectsRepository {

    private val firestore = Firebase.firestore

    suspend fun getAllActive(source: Source = Source.DEFAULT): List<VideoSubject> {
        val snapshot = firestore.collection("videoSubjects").get(source).await()
        return snapshot.toObjects<VideoSubject>().filter { it.active }
    }
}
