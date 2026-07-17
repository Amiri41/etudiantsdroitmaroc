package com.etudiantsdroitmaroc.app.ui.videos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.VideoLesson
import com.etudiantsdroitmaroc.app.databinding.ItemVideoCardBinding

class VideoAdapter(
    private var videos: List<VideoLesson>,
    private val onClick: (VideoLesson) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VH>() {

    inner class VH(val binding: ItemVideoCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemVideoCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val video = videos[position]
        holder.binding.tvVideoTitle.text = video.title
        val thumb = video.thumbnailUrl.ifEmpty { "https://img.youtube.com/vi/${video.youtubeId}/hqdefault.jpg" }
        Glide.with(holder.itemView).load(thumb).into(holder.binding.ivThumbnail)
        holder.binding.root.setOnClickListener { onClick(video) }
    }

    override fun getItemCount() = videos.size

    fun updateData(newVideos: List<VideoLesson>) {
        videos = newVideos
        notifyDataSetChanged()
    }
}
