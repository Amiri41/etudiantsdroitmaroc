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

    suspend fun createPost(content: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("خاصك تسجل الدخول"))
        return try {
            val post = Post(
                authorUid = user.uid,
                authorName = user.displayName ?: "",
                authorPhotoUrl = user.photoUrl?.toString() ?: "",
                content = content
            )
            val docRef = firestore.collection("posts").document()
            post.id = docRef.id
            docRef.set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(postId: String): List<Comment> {
        val snapshot = firestore.collection("posts").document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.toObjects()
    }

    suspend fun addComment(postId: String, content: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("خاصك تسجل الدخول"))
        return try {
            val comment = Comment(
                postId = postId,
                authorUid = user.uid,
                authorName = user.displayName ?: "",
                content = content
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
