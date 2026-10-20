package com.etudiantsdroitmaroc.app.utils

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** علامة فـ الـ coroutine: القراءة من الكاش فقط (باش الشاشة تبان فورا) */
class CacheOnly : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CacheOnly>
}

private const val SERVER_TIMEOUT_MS = 3500L

/**
 * قراءة سريعة: السيرفر أولا بمهلة قصيرة، وإلا ضعف النت ولا انقطع كنرجعو للكاش بلا ما نبقاو نتسناو.
 * داخل CacheOnly كنقراو من الكاش مباشرة.
 */
suspend fun Query.getFast(): QuerySnapshot {
    if (coroutineContext[CacheOnly.Key] != null) return get(Source.CACHE).await()
    return try {
        withTimeout(SERVER_TIMEOUT_MS) { get(Source.SERVER).await() }
    } catch (e: TimeoutCancellationException) {
        get(Source.CACHE).await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        get(Source.CACHE).await()
    }
}

suspend fun DocumentReference.getFast(): DocumentSnapshot {
    if (coroutineContext[CacheOnly.Key] != null) return get(Source.CACHE).await()
    return try {
        withTimeout(SERVER_TIMEOUT_MS) { get(Source.SERVER).await() }
    } catch (e: TimeoutCancellationException) {
        get(Source.CACHE).await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        get(Source.CACHE).await()
    }
}

// ---------------------------------------------------------------------------------------------
// قراءة "صارمة" للمحادثات: getFast() كيخبي أي خطأ ويرجع الكاش (حتى فارغ)، فالمحادثات كتبان فارغة بلا ما نعرفو العلة.
// هنا: كنرجعو للكاش غير فالمهلة وانقطاع الشبكة، وإلا كان الكاش فارغ كنطلعو خطأ واضح. أخطاء الصلاحيات
// والـ index (FAILED_PRECONDITION) كنطلعوها دايما وكنسجلوها فـ Logcat (الرسالة فيها رابط إنشاء الـ index).
// ---------------------------------------------------------------------------------------------

private const val STRICT_SERVER_TIMEOUT_MS = 8000L

/** السيرفر ما جاوبش فالوقت وما كاين حتى شي فالكاش: خطأ شبكة (كيتعرض للمستخدم برسالة واضحة بدل لائحة فارغة) */
class SlowConnectionException : java.io.IOException("Server did not answer in time and cache is empty")

private fun isTransient(e: FirebaseFirestoreException) =
    e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
        e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED

suspend fun Query.getFastStrict(): QuerySnapshot {
    if (coroutineContext[CacheOnly.Key] != null) return get(Source.CACHE).await()
    return try {
        withTimeout(STRICT_SERVER_TIMEOUT_MS) { get(Source.SERVER).await() }
    } catch (e: TimeoutCancellationException) {
        queryCacheOrThrow(SlowConnectionException())
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        if (isTransient(e)) queryCacheOrThrow(e) else {
            android.util.Log.e("EDM", "Firestore query failed: ${e.code} - ${e.message}", e)
            throw e
        }
    }
}

private suspend fun Query.queryCacheOrThrow(cause: Exception): QuerySnapshot {
    val cached = get(Source.CACHE).await()
    if (cached.isEmpty) throw cause
    return cached
}

/** مستند: النتيجة من السيرفر مضمونة. فالمهلة/انقطاع الشبكة كنرجعو غير مستند موجود فالكاش، وإلا خطأ (ما كنخمنوش). */
suspend fun DocumentReference.getFastStrict(): DocumentSnapshot {
    if (coroutineContext[CacheOnly.Key] != null) return get(Source.CACHE).await()
    return try {
        withTimeout(STRICT_SERVER_TIMEOUT_MS) { get(Source.SERVER).await() }
    } catch (e: TimeoutCancellationException) {
        docCacheOrThrow(SlowConnectionException())
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        if (isTransient(e)) docCacheOrThrow(e) else {
            android.util.Log.e("EDM", "Firestore doc read failed: ${e.code} - ${e.message}", e)
            throw e
        }
    }
}

private suspend fun DocumentReference.docCacheOrThrow(cause: Exception): DocumentSnapshot {
    val cached = get(Source.CACHE).await()
    if (!cached.exists()) throw cause
    return cached
}

/**
 * كيشغل [block] جوج مرات: الأولى من الكاش (عرض فوري)، والثانية من السيرفر (تحديث).
 * إلا كان المحتوى بان من الكاش وفشل السيرفر (بلا نت)، ما كنطلعوش خطأ.
 */
suspend fun cacheThenServer(onError: (Exception) -> Unit, block: suspend () -> Unit) {
    var shown = false
    try {
        withContext(CacheOnly()) { block() }
        shown = true
    } catch (e: Exception) {
        if (e is CancellationException) throw e
    }
    try {
        block()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        if (!shown) onError(e)
    }
}
