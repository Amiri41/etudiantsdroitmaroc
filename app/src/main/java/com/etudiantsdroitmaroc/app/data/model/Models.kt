package com.etudiantsdroitmaroc.app.data.model

import com.google.firebase.firestore.PropertyName

/** مادة قانونية (بحال: القانون المدني، الدستوري...) */
data class Subject(
    var id: String = "",
    var name: String = "",
    var category: String = "", // "private" أو "public"
    var iconName: String = "",
    var orderIndex: Int = 0
)

/** ملف PDF مرتبط بمادة */
data class PdfDocument(
    var id: String = "",
    var subjectId: String = "",
    var title: String = "",
    var storageUrl: String = "",
    var fileSizeKb: Long = 0,
    @get:PropertyName("isNew") @set:PropertyName("isNew")
    var isNew: Boolean = false,
    var uploadedAt: Long = System.currentTimeMillis()
)

/** منشور فالمنتدى (مرتبط بمادة أو عام) */
data class Post(
    var id: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var authorPhotoUrl: String = "",
    var subjectId: String = "", // فارغ = منتدى عام
    var content: String = "",
    var likesCount: Int = 0,
    var commentsCount: Int = 0,
    var createdAt: Long = System.currentTimeMillis()
)

data class Comment(
    var id: String = "",
    var postId: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var content: String = "",
    var createdAt: Long = System.currentTimeMillis()
)

/** محادثة خاصة بين طالبين */
data class ChatThread(
    var id: String = "",
    var participantUids: List<String> = emptyList(),
    var participantNames: Map<String, String> = emptyMap(),
    var lastMessage: String = "",
    var lastMessageAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    var id: String = "",
    var senderUid: String = "",
    var text: String = "",
    var sentAt: Long = System.currentTimeMillis(),
    var seen: Boolean = false
)

/** بروفايل المستخدم */
data class UserProfile(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var photoUrl: String = "",
    var isOnline: Boolean = false,
    var lastSeen: Long = System.currentTimeMillis()
)
