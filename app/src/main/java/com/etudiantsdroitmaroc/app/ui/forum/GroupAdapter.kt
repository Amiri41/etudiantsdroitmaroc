package com.etudiantsdroitmaroc.app.ui.forum

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.ForumGroup
import com.etudiantsdroitmaroc.app.databinding.ItemGroupCardBinding

class GroupAdapter(
    private var groups: List<ForumGroup>,
    private val isMember: (ForumGroup) -> Boolean,
    private val onOpen: (ForumGroup) -> Unit,
    private val onJoin: (ForumGroup) -> Unit
) : RecyclerView.Adapter<GroupAdapter.VH>() {

    inner class VH(val binding: ItemGroupCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGroupCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val group = groups[position]
        holder.binding.tvGroupName.text = group.name
        holder.binding.tvGroupDescription.text = group.description

        if (group.iconUrl.isNotEmpty()) {
            Glide.with(holder.itemView).load(group.iconUrl).into(holder.binding.ivGroupIcon)
        }

        val member = isMember(group)
        holder.binding.btnJoinGroup.text = if (member) "دخول" else "انضمام"

        holder.binding.btnJoinGroup.setOnClickListener {
            if (member) onOpen(group) else onJoin(group)
        }
        holder.binding.root.setOnClickListener {
            if (member) onOpen(group)
        }
    }

    override fun getItemCount() = groups.size

    fun updateData(newGroups: List<ForumGroup>) {
        groups = newGroups
        notifyDataSetChanged()
    }
}
