package com.etudiantsdroitmaroc.app.data.remote

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
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val myUid get() = auth.currentUser?.uid.orEmpty()

    suspend fun getMyThreads(): List<ChatThread> {
        val snapshot = firestore.collection("chat_threads")
            .whereArrayContains("participantUids", myUid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.toObjects()
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = firestore.collection("users").document(uid).get().await()
        return doc.toObject<UserProfile>()
    }

    suspend fun isFriend(otherUid: String): Boolean {
        val doc = firestore.collection("users").document(myUid)
            .collection("friends").document(otherUid).get().await()
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
            val otherDoc = firestore.collection("users").document(otherUid).get().await()
            val fcmToken = otherDoc.getString("fcmToken")
            if (!fcmToken.isNullOrBlank()) {
                PushNotifier.sendNotification(fcmToken, request.fromName, "بعث ليك طلب صداقة 👋")
            }
        } catch (_: Exception) { }
    }

    /** واش كاين طلب صداقة معلق مني للشخص هادا */
    suspend fun hasPendingRequestTo(otherUid: String): Boolean {
        val doc = firestore.collection("users").document(otherUid)
            .collection("incoming_requests").document(myUid).get().await()
        return doc.exists()
    }

    suspend fun getIncomingRequests(): List<FriendRequest> {
        val snapshot = firestore.collection("users").document(myUid)
            .collection("incoming_requests")
            .get().await()
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

    suspend fun getIncomingRequestsCount(): Int {
        return try {
            firestore.collection("users").document(myUid)
                .collection("incoming_requests").get().await().size()
        } catch (e: Exception) {
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
        val snapshot = firestore.collection("users").get().await()
        return snapshot.toObjects<UserProfile>().filter { it.uid != myUid }
    }

    suspend fun getMyFriendsUids(): Set<String> {
        val snapshot = firestore.collection("users").document(myUid)
            .collection("friends").get().await()
        return snapshot.documents.map { it.id }.toSet()
    }

    suspend fun getMyFriends(): List<UserProfile> {
        val friendUids = getMyFriendsUids()
        if (friendUids.isEmpty()) return emptyList()
        return friendUids.mapNotNull { uid ->
            try {
                firestore.collection("users").document(uid).get().await()
                    .toObject(UserProfile::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getOrCreateThread(otherUid: String, otherName: String): String {
        val threadId = listOf(myUid, otherUid).sorted().joinToString("_")
        val docRef = firestore.collection("chat_threads").document(threadId)
        val existing = docRef.get().await()

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

    private suspend fun notifyOtherParticipant(threadId: String, previewText: String) {
        try {
            val threadDoc = firestore.collection("chat_threads").document(threadId).get().await()
            val thread = threadDoc.toObject<ChatThread>() ?: return
            val otherUid = thread.participantUids.firstOrNull { it != myUid } ?: return

            val otherUserDoc = firestore.collection("users").document(otherUid).get().await()
            val fcmToken = otherUserDoc.getString("fcmToken") ?: return

            val myName = auth.currentUser?.displayName ?: "رسالة جديدة"
            PushNotifier.sendNotification(fcmToken, myName, previewText)
        } catch (e: Exception) {
            // ما تكسرش الإرسال العادي إلا فشل الإشعار
        }
    }

    suspend fun sendImageMessage(threadId: String, imageUrl: String) {
        val message = ChatMessage(senderUid = myUid, type = "image", imageUrl = imageUrl)
        val docRef = firestore.collection("chat_threads").document(threadId)
            .collection("messages").document()
        message.id = docRef.id
        docRef.set(message).await()

        firestore.collection("chat_threads").document(threadId).update(
            mapOf(
                "lastMessage" to "📷 صورة",
                "lastMessageAt" to System.currentTimeMillis()
            )
        ).await()

        notifyOtherParticipant(threadId, "📷 صورة")
    }

    suspend fun sendMessage(threadId: String, text: String) {
        val message = ChatMessage(senderUid = myUid, text = text)
        val docRef = firestore.collection("chat_threads").document(threadId)
            .collection("messages").document()
        message.id = docRef.id
        docRef.set(message).await()

        firestore.collection("chat_threads").document(threadId).update(
            mapOf(
                "lastMessage" to text,
                "lastMessageAt" to System.currentTimeMillis()
            )
        ).await()

        notifyOtherParticipant(threadId, text)
    }

    suspend fun getMessages(threadId: String): List<ChatMessage> {
        val snapshot = firestore.collection("chat_threads").document(threadId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.toObjects()
    }

    suspend fun getOnlineUsers(): List<UserProfile> {
        // فحالة انهيار التطبيق (بلا onStop)، isOnline يقدر يبقى true غلط -
        // فنزيدو فلترة زيادة بـ lastSeen (آخر 90 ثانية) كحماية إضافية
        val recentThreshold = System.currentTimeMillis() - 90_000L
        val snapshot = firestore.collection("users")
            .whereEqualTo("isOnline", true)
            .get()
            .await()
        return snapshot.toObjects<UserProfile>()
            .filter { it.uid != myUid && it.lastSeen >= recentThreshold }
    }
}
