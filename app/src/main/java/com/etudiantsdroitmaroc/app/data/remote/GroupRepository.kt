package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.ForumGroup
import com.etudiantsdroitmaroc.app.data.model.GroupMessage
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class GroupRepository {

    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val myUid get() = auth.currentUser?.uid.orEmpty()

    suspend fun createGroup(name: String, description: String, iconUrl: String): String {
        val docRef = firestore.collection("forum_groups").document()
        val group = ForumGroup(
            id = docRef.id,
            name = name,
            description = description,
            iconUrl = iconUrl,
            creatorUid = myUid,
            creatorName = auth.currentUser?.displayName ?: "",
            memberUids = listOf(myUid)
        )
        docRef.set(group).await()
        return docRef.id
    }

    suspend fun getGroups(): List<ForumGroup> {
        val snapshot = firestore.collection("forum_groups")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.toObjects()
    }

    suspend fun joinGroup(groupId: String) {
        firestore.collection("forum_groups").document(groupId)
            .update("memberUids", FieldValue.arrayUnion(myUid))
            .await()
    }

    suspend fun leaveGroup(groupId: String) {
        firestore.collection("forum_groups").document(groupId)
            .update("memberUids", FieldValue.arrayRemove(myUid))
            .await()
    }

    fun isMember(group: ForumGroup): Boolean = group.memberUids.contains(myUid)

    suspend fun getGroupMessages(groupId: String): List<GroupMessage> {
        val snapshot = firestore.collection("forum_groups").document(groupId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .get().await()
        return snapshot.toObjects()
    }

    suspend fun sendGroupTextMessage(groupId: String, text: String) {
        val user = auth.currentUser ?: return
        val docRef = firestore.collection("forum_groups").document(groupId)
            .collection("messages").document()
        val message = GroupMessage(
            id = docRef.id,
            senderUid = user.uid,
            senderName = user.displayName ?: "",
            senderPhoto = user.photoUrl?.toString() ?: "",
            text = text,
            type = "text"
        )
        docRef.set(message).await()
    }

    suspend fun sendGroupImageMessage(groupId: String, imageUrl: String) {
        val user = auth.currentUser ?: return
        val docRef = firestore.collection("forum_groups").document(groupId)
            .collection("messages").document()
        val message = GroupMessage(
            id = docRef.id,
            senderUid = user.uid,
            senderName = user.displayName ?: "",
            senderPhoto = user.photoUrl?.toString() ?: "",
            type = "image",
            imageUrl = imageUrl
        )
        docRef.set(message).await()
    }

    suspend fun deleteGroupMessage(groupId: String, messageId: String) {
        firestore.collection("forum_groups").document(groupId)
            .collection("messages").document(messageId).delete().await()
    }

    fun isGroupAdmin(group: ForumGroup): Boolean = group.creatorUid == myUid

    suspend fun getGroup(groupId: String): ForumGroup? {
        return firestore.collection("forum_groups").document(groupId).get().await()
            .toObject(ForumGroup::class.java)
    }

    suspend fun getGroupMembers(groupId: String): List<UserProfile> {
        val group = getGroup(groupId) ?: return emptyList()
        return group.memberUids.mapNotNull { uid ->
            try {
                firestore.collection("users").document(uid).get().await()
                    .toObject(UserProfile::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /** الأدمين (منشئ المجموعة) فقط يقدر يعدل الاسم/الوصف/الصورة */
    suspend fun updateGroupInfo(groupId: String, name: String, description: String, iconUrl: String) {
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "description" to description
        )
        if (iconUrl.isNotEmpty()) updates["iconUrl"] = iconUrl
        firestore.collection("forum_groups").document(groupId).update(updates).await()
    }

    /** الأدمين كيقدر يحيد أي عضو (حظر من المجموعة) */
    suspend fun removeMember(groupId: String, uid: String) {
        firestore.collection("forum_groups").document(groupId)
            .update("memberUids", FieldValue.arrayRemove(uid))
            .await()
    }

    /** حذف المجموعة كاملة (الأدمين فقط) - كيمسح الرسائل والمجموعة نفسها */
    suspend fun deleteGroup(groupId: String) {
        val messages = firestore.collection("forum_groups").document(groupId)
            .collection("messages").get().await()
        val batch = firestore.batch()
        messages.documents.forEach { batch.delete(it.reference) }
        batch.delete(firestore.collection("forum_groups").document(groupId))
        batch.commit().await()
    }
}
