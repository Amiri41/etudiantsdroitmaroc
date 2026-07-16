package com.etudiantsdroitmaroc.app.ui.forum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.Comment
import com.etudiantsdroitmaroc.app.databinding.ItemCommentBinding

class CommentAdapter(private var comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.VH>() {

    inner class VH(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val comment = comments[position]
        holder.binding.tvAuthorName.text = comment.authorName
        holder.binding.tvContent.text = comment.content

        if (comment.authorPhotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView).load(comment.authorPhotoUrl).into(holder.binding.ivAuthorPhoto)
        }

        if (comment.imageUrl.isNotEmpty()) {
            holder.binding.ivCommentImage.visibility = View.VISIBLE
            Glide.with(holder.itemView).load(comment.imageUrl).into(holder.binding.ivCommentImage)
        } else {
            holder.binding.ivCommentImage.visibility = View.GONE
        }
    }

    override fun getItemCount() = comments.size

    fun updateData(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }
}
