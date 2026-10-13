package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.AppSectionsConfig
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class AppConfigRepository {

    private val firestore = Firebase.firestore

    /** من الكاش المحلي مباشرة (null إلا ماكانش) - فوري */
    suspend fun getCachedSectionsConfig(): AppSectionsConfig? {
        return try {
            val doc = firestore.collection("app_config").document("sections").get(Source.CACHE).await()
            if (doc.exists()) doc.toObject(AppSectionsConfig::class.java) else null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    /** من السيرفر (null إلا فشل) - كيحدث الكاش أوتوماتيكيا */
    suspend fun getFreshSectionsConfig(): AppSectionsConfig? {
        return try {
            val doc = firestore.collection("app_config").document("sections").get(Source.SERVER).await()
            doc.toObject(AppSectionsConfig::class.java)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    suspend fun getSectionsConfig(): AppSectionsConfig {
        return try {
            val doc = firestore.collection("app_config").document("sections").get().await()
            doc.toObject(AppSectionsConfig::class.java) ?: AppSectionsConfig()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AppSectionsConfig()
        }
    }

    /** ترتيب الصفحة الرئيسية (الأقسام + أماكن الإعلانات): null إلا ماكاينش ولا فشل */
    suspend fun getHomeOrder(source: Source): List<String>? {
        return try {
            val doc = firestore.collection("app_config").document("home_layout").get(source).await()
            if (!doc.exists()) null
            else (doc.get("order") as? List<*>)?.mapNotNull { it as? String }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }
}
