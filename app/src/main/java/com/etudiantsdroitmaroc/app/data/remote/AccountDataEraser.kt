package com.etudiantsdroitmaroc.app.data.remote

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * كيمسح بيانات المستعمل من Firestore ملي كيحذف حسابو (متطلب Google Play).
 * كل خطوة مستقلة: إلا فشلات وحدة (بحال index ناقص) الباقي كيكمل، وكيرجع false
 * باش التطبيق يعلم المستعمل بلي شي حاجة تبقات (والحساب كيتمسح على كل حال).
 * ما كيمسحش: الإبلاغات (كتبقى لحماية المستعملين)، الإعجابات (علامة بلا محتوى)، صور imgbb.
 */
class AccountDataEraser(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /** true = كلشي تمسح. false = شي خطوة فشلات. */
    suspend fun eraseAll(uid: String): Boolean {
        var complete = true
        suspend fun step(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                complete = false
            }
        }
        step { deleteMyComments(uid) }
        step { deleteMyMessages(uid) }
        step { clearThreadPreviews(uid) }
        step { leaveGroups(uid) }
        step { deleteMyPosts(uid) }
        step { deleteFriendLinks(uid) }
        step { deleteOwnLists(uid) }
        return complete
    }

    /** تعليقاتي فجميع المنشورات + تنقيص عداد التعليقات (كل تحديث -1 كيوافق القواعد) */
    private suspend fun deleteMyComments(uid: String) {
        val refs = db.collectionGroup("comments").whereEqualTo("authorUid", uid)
            .get().await().documents.map { it.reference }
        val posts = refs.mapNotNull { it.parent.parent }
        deleteInBatches(refs)
        for (post in posts) {
            try {
                post.update("commentsCount", FieldValue.increment(-1)).await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e // المنشور ممكن يكون تمسح: عادي
            }
        }
    }

    /** الرسائل اللي صيفطت: دردشة خاصة + مجموعات المنتدى (كلاهما فيه senderUid) */
    private suspend fun deleteMyMessages(uid: String) {
        val refs = db.collectionGroup("messages").whereEqualTo("senderUid", uid)
            .get().await().documents.map { it.reference }
        deleteInBatches(refs)
    }

    /** معاينة آخر رسالة فالمحادثات الخاصة ممكن تكون ديالي: نفرغوها */
    private suspend fun clearThreadPreviews(uid: String) {
        val threads = db.collection("chat_threads").whereArrayContains("participantUids", uid)
            .get().await().documents
        for (t in threads) {
            t.reference.update("lastMessage", "").await()
        }
    }

    private suspend fun leaveGroups(uid: String) {
        val groups = db.collection("forum_groups").whereArrayContains("memberUids", uid)
            .get().await().documents
        for (g in groups) {
            g.reference.update("memberUids", FieldValue.arrayRemove(uid)).await()
        }
    }

    private suspend fun deleteMyPosts(uid: String) {
        val refs = db.collection("posts").whereEqualTo("authorUid", uid)
            .get().await().documents.map { it.reference }
        deleteInBatches(refs)
    }

    /** نمسحو راسي من لوائح أصدقاء الآخرين (كان كيبقى ظاهر عندهم) */
    private suspend fun deleteFriendLinks(uid: String) {
        val mine = db.collection("users").document(uid).collection("friends").get().await().documents
        val reverse = mine.map { db.collection("users").document(it.id).collection("friends").document(uid) }
        for (ref in reverse) {
            try {
                ref.delete().await()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    private suspend fun deleteOwnLists(uid: String) {
        for (sub in listOf("friends", "incoming_requests", "blocked")) {
            val refs = db.collection("users").document(uid).collection(sub)
                .get().await().documents.map { it.reference }
            deleteInBatches(refs)
        }
    }

    private suspend fun deleteInBatches(refs: List<DocumentReference>) {
        for (chunk in refs.chunked(100)) {
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }
}
