package com.etudiantsdroitmaroc.app.data.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.etudiantsdroitmaroc.app.data.model.ChatMessage
import com.etudiantsdroitmaroc.app.data.model.ChatThread
import com.etudiantsdroitmaroc.app.data.model.FriendRequest
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.etudiantsdroitmaroc.app.utils.PushNotifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.etudiantsdroitmaroc.app.utils.getFast
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val myUid get() = auth.currentUser?.uid.orEmpty()

    suspend fun getMyThreads(): List<ChatThread> {
        val blocked = getBlockedUids()
        val snapshot = firestore.collection("chat_threads")
            .whereArrayContains("participantUids", myUid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .getFast()
        return snapshot.toObjects<ChatThread>().filter { thread ->
            val otherUid = thread.participantUids.firstOrNull { it != myUid }
            otherUid == null || !blocked.contains(otherUid)
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = firestore.collection("users").document(uid).getFast()
        return doc.toObject<UserProfile>()
    }

    suspend fun isFriend(otherUid: String): Boolean {
        val doc = firestore.collection("users").document(myUid)
            .collection("friends").document(otherUid).getFast()
        return doc.exists()
    }

    /** إضافة صداقة متبادلة (كتنعمل غير من داخل acceptFriendRequest بعد القبول) */
    suspend fun addFriend(otherUid: String) {
        val batch = firestore.batch()
        batch.set(
            firestore.collection("users").document(myUid).collection("friends").document(otherUid),
            mapOf("addedAt" to System.currentTimeMillis())
        )
        batch.set(
            firestore.collection("users").document(otherUid).collection("friends").document(myUid),
            mapOf("addedAt" to System.currentTimeMillis())
        )
        batch.commit().await()
    }

    /** بحال فيسبوك: كنبعتو طلب صداقة، ماشي صداقة مباشرة */
    suspend fun sendFriendRequest(otherUid: String) {
        val myProfile = getUserProfile(myUid)
        val request = FriendRequest(
            fromUid = myUid,
            fromName = myProfile?.name ?: (auth.currentUser?.displayName ?: ""),
            fromPhoto = myProfile?.photoUrl ?: ""
        )
        firestore.collection("users").document(otherUid)
            .collection("incoming_requests").document(myUid)
            .set(request).await()

        try {
            PushNotifier.sendFriendRequest(otherUid, request.fromName, "بعث ليك طلب صداقة 👋")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
    }

    /** واش كاين طلب صداقة معلق مني للشخص هادا */
    suspend fun hasPendingRequestTo(otherUid: String): Boolean {
        return try {
            firestore.collection("users").document(otherUid)
                .collection("incoming_requests").document(myUid).getFast().exists()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            false
        }
    }

    suspend fun getIncomingRequests(): List<FriendRequest> {
        val snapshot = firestore.collection("users").document(myUid)
            .collection("incoming_requests")
            .getFast()
        return snapshot.toObjects()
    }

    suspend fun acceptFriendRequest(fromUid: String) {
        addFriend(fromUid)
        firestore.collection("users").document(myUid)
            .collection("incoming_requests").document(fromUid).delete().await()
    }

    suspend fun declineFriendRequest(fromUid: String) {
        firestore.collection("users").document(myUid)
            .collection("incoming_requests").document(fromUid).delete().await()
    }

    /** كنلغيو طلب الصداقة اللي بعتناه (من جهة المُرسل) - الدردشة كتبقى ممكنة فأي وقت بحال واتساب */
    suspend fun cancelFriendRequest(otherUid: String) {
        firestore.collection("users").document(otherUid)
            .collection("incoming_requests").document(myUid).delete().await()
    }

    suspend fun getIncomingRequestsCount(): Int {
        return try {
            firestore.collection("users").document(myUid)
                .collection("incoming_requests").getFast().size()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            0
        }
    }

    suspend fun removeFriend(otherUid: String) {
        val batch = firestore.batch()
        batch.delete(firestore.collection("users").document(myUid).collection("friends").document(otherUid))
        batch.delete(firestore.collection("users").document(otherUid).collection("friends").document(myUid))
        batch.commit().await()
    }

    suspend fun getAllUsers(): List<UserProfile> {
        val blocked = getBlockedUids()
        val snapshot = firestore.collection("users").getFast()
        return snapshot.toObjects<UserProfile>().filter { it.uid != myUid && !blocked.contains(it.uid) }
    }

    suspend fun getMyFriendsUids(): Set<String> {
        val snapshot = firestore.collection("users").document(myUid)
            .collection("friends").getFast()
        return snapshot.documents.map { it.id }.toSet()
    }

    suspend fun getMyFriends(): List<UserProfile> {
        val friendUids = getMyFriendsUids()
        if (friendUids.isEmpty()) return emptyList()
        return coroutineScope {
            friendUids.map { uid ->
                async {
                    try {
                        firestore.collection("users").document(uid).getFast()
                            .toObject(UserProfile::class.java)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    suspend fun blockUser(otherUid: String) {
        firestore.collection("users").document(myUid)
            .collection("blocked").document(otherUid)
            .set(mapOf("blockedAt" to System.currentTimeMillis())).await()
    }

    suspend fun unblockUser(otherUid: String) {
        firestore.collection("users").document(myUid)
            .collection("blocked").document(otherUid).delete().await()
    }

    suspend fun getBlockedUids(): Set<String> {
        val snapshot = firestore.collection("users").document(myUid)
            .collection("blocked").getFast()
        return snapshot.documents.map { it.id }.toSet()
    }

    suspend fun clearChatMessages(threadId: String) {
        val messages = firestore.collection("chat_threads").document(threadId)
            .collection("messages").get().await()
        val batch = firestore.batch()
        messages.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        firestore.collection("chat_threads").document(threadId)
            .update(mapOf("lastMessage" to "", "lastMessageAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun getOrCreateThread(otherUid: String, otherName: String): String {
        val threadId = listOf(myUid, otherUid).sorted().joinToString("_")
        val docRef = firestore.collection("chat_threads").document(threadId)
        val existing = docRef.getFast()

        if (!existing.exists()) {
            val myName = auth.currentUser?.displayName ?: ""
            val thread = ChatThread(
                id = threadId,
                participantUids = listOf(myUid, otherUid),
                participantNames = mapOf(myUid to myName, otherUid to otherName)
            )
            docRef.set(thread).await()
        }
        return threadId
    }

    private suspend fun notifyOtherParticipant(threadId: String, previewText: String, messageId: String = "") {
        try {
            val myName = auth.currentUser?.displayName ?: "رسالة جديدة"
            // الـ Worker كيتحقق من المحادثة ويجيب توكن الطرف الآخر بنفسو
            PushNotifier.sendChat(threadId, myName, previewText, messageId)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // الإشعار ماشي ضروري: نتجاهلو الخطأ
        }
    }

    suspend fun sendImageMessage(threadId: String, imageUrl: String) {
        val otherUid = getOtherParticipantUid(threadId)
        val message = ChatMessage(
            senderUid = myUid,
            type = "image",
            imageUrl = imageUrl,
            participantUids = listOfNotNull(myUid, otherUid)
        )
        val docRef = firestore.collection("chat_threads").document(threadId)
            .collection("messages").document()
        message.id = docRef.id
        docRef.set(message).await()

        val updates = mutableMapOf<String, Any>(
            "lastMessage" to "📷 صورة",
            "lastMessageAt" to System.currentTimeMillis()
        )
        if (otherUid != null) {
            updates["unreadCounts.$otherUid"] = com.google.firebase.firestore.FieldValue.increment(1)
        }
        firestore.collection("chat_threads").document(threadId).update(updates).await()

        notifyOtherParticipant(threadId, "📷 صورة", message.id)
    }

    suspend fun sendMessage(threadId: String, text: String) {
        val otherUid = getOtherParticipantUid(threadId)
        val message = ChatMessage(
            senderUid = myUid,
            text = text,
            participantUids = listOfNotNull(myUid, otherUid)
        )
        val docRef = firestore.collection("chat_threads").document(threadId)
            .collection("messages").document()
        message.id = docRef.id
        docRef.set(message).await()

        val updates = mutableMapOf<String, Any>(
            "lastMessage" to text,
            "lastMessageAt" to System.currentTimeMillis()
        )
        if (otherUid != null) {
            updates["unreadCounts.$otherUid"] = com.google.firebase.firestore.FieldValue.increment(1)
        }
        firestore.collection("chat_threads").document(threadId).update(updates).await()

        notifyOtherParticipant(threadId, text, message.id)
    }

    /**
     * ملي المستخدم كيشوف المحادثة: الرسائل الواردة كتولي "تمت قراءتها" (✓✓ زرقاء عند المرسل).
     * كنحدثو غير الرسائل اللي ما تقراوش (وكنزيدو وقت الوصول إلا ما كانش).
     */
    suspend fun markIncomingMessagesSeen(threadId: String, messages: List<ChatMessage>) {
        val pending = messages.filter { it.senderUid != myUid && !it.seen && it.id.isNotEmpty() }
        if (pending.isEmpty()) return
        try {
            val now = System.currentTimeMillis()
            val batch = firestore.batch()
            pending.forEach { m ->
                val ref = firestore.collection("chat_threads").document(threadId)
                    .collection("messages").document(m.id)
                val updates = mutableMapOf<String, Any>("seen" to true, "seenAt" to now)
                if (m.deliveredAt == 0L) updates["deliveredAt"] = now
                batch.update(ref, updates)
            }
            batch.commit().await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // ما نكسروش الدردشة إلا فشل التحديث (مثلا القواعد مازال ما تنشراتش)
        }
    }

    /** وصلات الرسالة لتطبيق المستقبل (كيتنادى من استقبال الإشعار): ✓✓ رمادية عند المرسل */
    suspend fun markMessageDelivered(threadId: String, messageId: String) {
        if (threadId.isEmpty() || messageId.isEmpty()) return
        try {
            val ref = firestore.collection("chat_threads").document(threadId)
                .collection("messages").document(messageId)
            ref.update("deliveredAt", System.currentTimeMillis()).await()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
    }

    private suspend fun getOtherParticipantUid(threadId: String): String? {
        val doc = firestore.collection("chat_threads").document(threadId).getFast()
        val thread = doc.toObject<ChatThread>() ?: return null
        return thread.participantUids.firstOrNull { it != myUid }
    }

    /** كنصفرو العداد ملي المستخدم يفتح المحادثة */
    suspend fun markThreadAsRead(threadId: String) {
        try {
            firestore.collection("chat_threads").document(threadId)
                .update("unreadCounts.$myUid", 0L).await()
        } catch (_: Exception) { }
    }

    suspend fun getTotalUnreadCount(): Int {
        return try {
            val snapshot = firestore.collection("chat_threads")
                .whereArrayContains("participantUids", myUid)
                .getFast()
            snapshot.toObjects<ChatThread>().sumOf { (it.unreadCounts[myUid] ?: 0L).toInt() }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            0
        }
    }

    suspend fun getMessages(threadId: String): List<ChatMessage> {
        val snapshot = firestore.collection("chat_threads").document(threadId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .getFast()
        return snapshot.toObjects()
    }

    suspend fun getOnlineUsers(): List<UserProfile> {
        // فحالة انهيار التطبيق (بلا onStop)، isOnline يقدر يبقى true غلط -
        // فنزيدو فلترة زيادة بـ lastSeen (آخر 90 ثانية) كحماية إضافية
        val recentThreshold = System.currentTimeMillis() - 90_000L
        val blocked = getBlockedUids()
        val snapshot = firestore.collection("users")
            .whereEqualTo("isOnline", true)
            .getFast()
        return snapshot.toObjects<UserProfile>()
            .filter { it.uid != myUid && it.lastSeen >= recentThreshold && !blocked.contains(it.uid) }
    }
}
