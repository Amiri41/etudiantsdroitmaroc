package com.etudiantsdroitmaroc.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.Subject
import com.etudiantsdroitmaroc.app.databinding.ItemSubjectBinding

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
            Glide.with(holder.itemView).load(subject.iconUrl)
                .placeholder(R.drawable.ic_law_scale)
                .into(holder.binding.ivIcon)
        } else {
            val padding = (30 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.binding.ivIcon.setPadding(padding, padding, padding, padding)
            holder.binding.ivIcon.setImageResource(R.drawable.ic_law_scale)
        }

        holder.binding.root.setOnClickListener { onClick(subject) }
    }

    override fun getItemCount() = subjects.size

    fun updateData(newSubjects: List<Subject>) {
        subjects = newSubjects
        notifyDataSetChanged()
    }
}
