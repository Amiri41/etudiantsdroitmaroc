package com.etudiantsdroitmaroc.app.data.model

import com.google.firebase.firestore.PropertyName

/** مادة قانونية (بحال: القانون المدني، الدستوري...) */
data class Subject(
    var id: String = "",
    var name: String = "",
    var category: String = "", // "private" أو "public" (احتفظنا بيه للتوافق القديم)
    var section: String = "private", // "private" | "public" | "master" | "phd" | "general"
    var semester: Int = 0, // 1-6 (خاص غير بـ private/public)، 0 = ماكاينش (ماستر/دكتوراه/عام)
    @get:PropertyName("active") @set:PropertyName("active")
    var active: Boolean = true,
    var iconName: String = "",
    var orderIndex: Int = 0
)

/** ملف PDF مرتبط بمادة */
data class PdfDocument(
    var id: String = "",
    var subjectId: String = "",
    var title: String = "",
    var storageUrl: String = "",
    var coverImageUrl: String = "",
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
    var imageUrl: String = "",
    var likesCount: Int = 0,
    var commentsCount: Int = 0,
    var createdAt: Long = System.currentTimeMillis()
)

data class Comment(
    var id: String = "",
    var postId: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var authorPhotoUrl: String = "",
    var content: String = "",
    var imageUrl: String = "",
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
    var type: String = "text", // "text" أو "image"
    var imageUrl: String = "",
    var sentAt: Long = System.currentTimeMillis(),
    var seen: Boolean = false
)

/** صفحة عامة (من نحن، الخصوصية، روابط...) - محتواها كامل قابل للتعديل من لوحة التحكم */
data class AppPage(
    var id: String = "",
    var title: String = "",
    var type: String = "text", // "text" أو "link"
    var content: String = "",  // نص الصفحة (إلا type = text)
    var url: String = "",      // رابط خارجي (إلا type = link)
    var iconName: String = "",
    @get:PropertyName("active") @set:PropertyName("active")
    var active: Boolean = true,
    var orderIndex: Int = 0
)
data class UserProfile(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var photoUrl: String = "",
    var university: String = "",
    var level: String = "",
    var isOnline: Boolean = false,
    var lastSeen: Long = System.currentTimeMillis()
)

/** بانر/إعلان كيتحكم فيه الأدمين، كيبان للمستخدم ملي يحل التطبيق */
data class AppAnnouncement(
    var type: String = "promo", // "promo" أو "update"
    var title: String = "",
    var message: String = "",
    var imageUrl: String = "",
    var linkUrl: String = "",
    var active: Boolean = false,
    var updatedAt: Long = 0
)

/** طلب صداقة معلق (بحال فيسبوك: طلب → قبول/رفض) */
data class FriendRequest(
    var fromUid: String = "",
    var fromName: String = "",
    var fromPhoto: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

/** مجموعة/مجتمع فالمنتدى (بحال مجموعات واتساب) */
data class ForumGroup(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var iconUrl: String = "",
    var creatorUid: String = "",
    var creatorName: String = "",
    var memberUids: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis()
)

/** رسالة داخل مجموعة (فيها اسم المرسل، بخلاف الدردشة الفردية) */
data class GroupMessage(
    var id: String = "",
    var senderUid: String = "",
    var senderName: String = "",
    var senderPhoto: String = "",
    var text: String = "",
    var type: String = "text", // "text" أو "image"
    var imageUrl: String = "",
    var sentAt: Long = System.currentTimeMillis()
)

/** فيديو تعليمي (يوتيوب) كيتحكم فيه الأدمين */
data class VideoLesson(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var youtubeId: String = "",
    var thumbnailUrl: String = "",
    @get:PropertyName("active") @set:PropertyName("active")
    var active: Boolean = true,
    var orderIndex: Int = 0,
    var createdAt: Long = System.currentTimeMillis()
)
