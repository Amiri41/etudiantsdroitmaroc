package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.AppPage
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class PagesRepository {

    private val firestore = Firebase.firestore

    suspend fun getPages(): List<AppPage> {
        val snapshot = firestore.collection("pages")
            .orderBy("orderIndex", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.toObjects<AppPage>().filter { it.active }
    }
}
