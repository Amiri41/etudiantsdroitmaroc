package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.Chapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ChaptersRepository {

    private val firestore = Firebase.firestore

    suspend fun getChaptersForSection(section: String): List<Chapter> {
        val snapshot = firestore.collection("chapters")
            .whereEqualTo("section", section)
            .get()
            .await()
        return snapshot.toObjects<Chapter>()
            .filter { it.active }
            .sortedBy { it.orderIndex }
    }
}
