package com.etudiantsdroitmaroc.app.utils

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
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
