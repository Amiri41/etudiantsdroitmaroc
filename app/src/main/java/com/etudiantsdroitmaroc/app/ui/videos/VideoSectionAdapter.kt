package com.etudiantsdroitmaroc.app.ui.videos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.data.model.VideoLesson
import com.etudiantsdroitmaroc.app.databinding.ItemVideoSectionBinding

data class VideoSection(
    val title: String,
    val videos: List<VideoLesson>
)

class VideoSectionAdapter(
    private var sections: List<VideoSection>,
    private val onClick: (VideoLesson) -> Unit
) : RecyclerView.Adapter<VideoSectionAdapter.VH>() {

    inner class VH(val binding: ItemVideoSectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemVideoSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val section = sections[position]
        holder.binding.tvSectionTitle.text = section.title
        holder.binding.rvSectionVideos.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.binding.rvSectionVideos.adapter = VideoAdapter(section.videos, onClick)
    }

    override fun getItemCount() = sections.size

    fun updateData(newSections: List<VideoSection>) {
        sections = newSections
        notifyDataSetChanged()
    }
}
