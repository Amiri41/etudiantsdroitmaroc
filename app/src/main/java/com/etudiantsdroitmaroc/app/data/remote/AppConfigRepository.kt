package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.AppSectionsConfig
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AppConfigRepository {

    private val firestore = Firebase.firestore

    suspend fun getSectionsConfig(): AppSectionsConfig {
        return try {
            val doc = firestore.collection("app_config").document("sections").get().await()
            doc.toObject(AppSectionsConfig::class.java) ?: AppSectionsConfig()
        } catch (e: Exception) {
            AppSectionsConfig()
        }
    }
}
