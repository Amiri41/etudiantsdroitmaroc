package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.Subject
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

    suspend fun getVideosForSubject(subjectId: String): List<VideoLesson> {
        val snapshot = firestore.collection("videos")
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()
        return snapshot.toObjects<VideoLesson>()
            .filter { it.active }
            .sortedBy { it.orderIndex }
    }

    /** كيجيب الفيديوهات مجمعة حسب المادة (بحال نتفليكس) للتصفح الشامل */
    suspend fun getVideoSectionsGroupedBySubject(): List<Pair<Subject, List<VideoLesson>>> {
        val subjectsSnapshot = firestore.collection("subjects").get().await()
        val subjects = subjectsSnapshot.toObjects<Subject>().filter { it.active }

        val videosSnapshot = firestore.collection("videos").get().await()
        val allVideos = videosSnapshot.toObjects<VideoLesson>().filter { it.active }
        val videosBySubject = allVideos.groupBy { it.subjectId }

        val sectionOrder = listOf("private", "public", "master", "phd", "general")

        return subjects
            .filter { videosBySubject.containsKey(it.id) }
            .sortedWith(
                compareBy(
                    { sectionOrder.indexOf(it.section).let { i -> if (i == -1) 99 else i } },
                    { it.semester },
                    { it.orderIndex }
                )
            )
            .map { subject -> subject to (videosBySubject[subject.id] ?: emptyList()) }
    }
}
