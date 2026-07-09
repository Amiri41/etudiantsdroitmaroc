package com.etudiantsdroitmaroc.app.ui.forum

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.Post
import com.etudiantsdroitmaroc.app.databinding.ItemPostBinding

class PostAdapter(private var posts: List<Post>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.binding.tvAuthorName.text = post.authorName
        holder.binding.tvContent.text = post.content
        val relativeTime = DateUtils.getRelativeTimeSpanString(post.createdAt)
        holder.binding.tvMeta.text = "$relativeTime · ${post.likesCount} إعجاب · ${post.commentsCount} تعليق"

        if (post.authorPhotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView).load(post.authorPhotoUrl).into(holder.binding.ivAuthorPhoto)
        }
    }

    override fun getItemCount() = posts.size

    fun updateData(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
