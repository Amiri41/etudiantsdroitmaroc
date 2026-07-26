package com.etudiantsdroitmaroc.app.ui.videos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.VideoSubject
import com.etudiantsdroitmaroc.app.databinding.ItemSubjectBinding

class VideoSubjectAdapter(
    private var subjects: List<VideoSubject>,
    private val onClick: (VideoSubject) -> Unit
) : RecyclerView.Adapter<VideoSubjectAdapter.VH>() {

    inner class VH(val binding: ItemSubjectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSubjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
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

    fun updateData(newSubjects: List<VideoSubject>) {
        subjects = newSubjects
        notifyDataSetChanged()
    }
}
