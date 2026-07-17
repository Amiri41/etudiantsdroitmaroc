package com.etudiantsdroitmaroc.app.data.remote

import com.etudiantsdroitmaroc.app.data.model.Comment
import com.etudiantsdroitmaroc.app.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ForumRepository {

    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    suspend fun getPosts(): List<Post> {
        val snapshot = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        return snapshot.toObjects()
    }

    suspend fun createPost(content: String, imageUrl: String = ""): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("خاصك تسجل الدخول"))
        return try {
            val post = Post(
                authorUid = user.uid,
                authorName = user.displayName ?: "",
                authorPhotoUrl = user.photoUrl?.toString() ?: "",
                content = content,
                imageUrl = imageUrl
            )
            val docRef = firestore.collection("posts").document()
            post.id = docRef.id
            docRef.set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            firestore.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, newContent: String): Result<Unit> {
        return try {
            firestore.collection("posts").document(postId).update("content", newContent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String): Boolean {
        val user = auth.currentUser ?: return false
        val likeRef = firestore.collection("posts").document(postId)
            .collection("likes").document(user.uid)
        val postRef = firestore.collection("posts").document(postId)

        val existing = likeRef.get().await()
        return if (existing.exists()) {
            likeRef.delete().await()
            postRef.update("likesCount", com.google.firebase.firestore.FieldValue.increment(-1)).await()
            false
        } else {
            likeRef.set(mapOf("likedAt" to System.currentTimeMillis())).await()
            postRef.update("likesCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
            true
        }
    }

    suspend fun isLikedByMe(postId: String): Boolean {
        val user = auth.currentUser ?: return false
        val doc = firestore.collection("posts").document(postId)
            .collection("likes").document(user.uid).get().await()
        return doc.exists()
    }
    suspend fun getComments(postId: String): List<Comment> {
        val snapshot = firestore.collection("posts").document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.toObjects()
    }

    suspend fun addComment(postId: String, content: String, imageUrl: String = ""): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("خاصك تسجل الدخول"))
        return try {
            val comment = Comment(
                postId = postId,
                authorUid = user.uid,
                authorName = user.displayName ?: "",
                authorPhotoUrl = user.photoUrl?.toString() ?: "",
                content = content,
                imageUrl = imageUrl
            )
            val docRef = firestore.collection("posts").document(postId)
                .collection("comments").document()
            comment.id = docRef.id
            docRef.set(comment).await()

            // نزيدو عداد التعليقات فالمنشور الأصلي
            firestore.collection("posts").document(postId)
                .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
