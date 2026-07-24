package com.etudiantsdroitmaroc.app.ui.videos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.data.model.Chapter
import com.etudiantsdroitmaroc.app.data.model.VideoSubject
import com.etudiantsdroitmaroc.app.databinding.ItemChapterRowBinding

/** فصل فيديو مع مواد الفيديو ديالو - شجرة مستقلة تماما عن شجرة PDF */
data class VideoChapterWithSubjects(
    val chapter: Chapter,
    val subjects: List<VideoSubject>
)

class VideoChapterRowAdapter(
    private var rows: List<VideoChapterWithSubjects>,
    private val onSubjectClick: (VideoSubject) -> Unit
) : RecyclerView.Adapter<VideoChapterRowAdapter.VH>() {

    inner class VH(val binding: ItemChapterRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemChapterRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.binding.tvChapterTitle.text = row.chapter.name

        val adapter = VideoSubjectAdapter(row.subjects) { onSubjectClick(it) }
        holder.binding.rvChapterSubjects.layoutManager = LinearLayoutManager(
            holder.binding.root.context, LinearLayoutManager.HORIZONTAL, false
        )
        holder.binding.rvChapterSubjects.adapter = adapter
    }

    override fun getItemCount() = rows.size

    fun updateData(newRows: List<VideoChapterWithSubjects>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
