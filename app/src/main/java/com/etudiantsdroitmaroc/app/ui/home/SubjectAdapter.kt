package com.etudiantsdroitmaroc.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.Subject
import com.etudiantsdroitmaroc.app.databinding.ItemSubjectBinding
import com.etudiantsdroitmaroc.app.utils.ImageLoader

class SubjectAdapter(
    private var subjects: List<Subject>,
    private val onClick: (Subject) -> Unit
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    inner class SubjectViewHolder(val binding: ItemSubjectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = ItemSubjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjects[position]
        holder.binding.tvSubjectName.text = subject.name

        if (subject.iconUrl.isNotEmpty()) {
            holder.binding.ivIcon.setPadding(0, 0, 0, 0)
            ImageLoader.load(holder.binding.ivIcon, subject.iconUrl, R.drawable.ic_law_scale)
        } else {
            val padding = (30 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.binding.ivIcon.setPadding(padding, padding, padding, padding)
            holder.binding.ivIcon.setImageResource(R.drawable.ic_law_scale)
        }

        holder.binding.root.setOnClickListener { onClick(subject) }
    }

    override fun getItemCount() = subjects.size

    /** DiffUtil بدل notifyDataSetChanged - كيقلل الفليكر ملي كيتبدل المحتوى */
    fun updateData(newSubjects: List<Subject>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = subjects.size
            override fun getNewListSize() = newSubjects.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return subjects[oldItemPosition].id == newSubjects[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return subjects[oldItemPosition] == newSubjects[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        subjects = newSubjects
        diffResult.dispatchUpdatesTo(this)
    }
}
