package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.VideoLesson
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class VideosRepository {

    private val firestore = Firebase.firestore

    suspend fun getVideos(): List<VideoLesson> {
        val snapshot = firestore.collection("videos")
            .orderBy("orderIndex", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.toObjects<VideoLesson>().filter { it.active }
    }

    /** الفيديوهات ديال مادة فيديو معينة (videoSubjects) - مستقلة عن مواد PDF */
    suspend fun getVideosForVideoSubject(videoSubjectId: String): List<VideoLesson> {
        val snapshot = firestore.collection("videos")
            .whereEqualTo("videoSubjectId", videoSubjectId)
            .get()
            .await()
        return snapshot.toObjects<VideoLesson>()
            .filter { it.active }
            .sortedBy { it.orderIndex }
    }
}
