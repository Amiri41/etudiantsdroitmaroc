package com.etudiantsdroitmaroc.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.data.model.Chapter
import com.etudiantsdroitmaroc.app.data.model.Subject
import com.etudiantsdroitmaroc.app.databinding.ItemChapterRowBinding

/** فصل مع لائحة المواد ديالو، باش يتعرض صف بصف (بحال Netflix) */
data class ChapterWithSubjects(
    val chapter: Chapter,
    val subjects: List<Subject>
)

class ChapterRowAdapter(
    private var rows: List<ChapterWithSubjects>,
    private val onSubjectClick: (Subject) -> Unit
) : RecyclerView.Adapter<ChapterRowAdapter.ChapterRowViewHolder>() {

    inner class ChapterRowViewHolder(val binding: ItemChapterRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterRowViewHolder {
        val binding = ItemChapterRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChapterRowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChapterRowViewHolder, position: Int) {
        val row = rows[position]
        holder.binding.tvChapterTitle.text = row.chapter.name

        val subjectAdapter = SubjectAdapter(row.subjects) { onSubjectClick(it) }
        holder.binding.rvChapterSubjects.layoutManager = LinearLayoutManager(
            holder.binding.root.context, LinearLayoutManager.HORIZONTAL, false
        )
        holder.binding.rvChapterSubjects.adapter = subjectAdapter
    }

    override fun getItemCount() = rows.size

    fun updateData(newRows: List<ChapterWithSubjects>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
