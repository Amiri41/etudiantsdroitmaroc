package com.etudiantsdroitmaroc.app.ui.forum

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.etudiantsdroitmaroc.app.databinding.ItemGroupMemberBinding

class GroupMemberAdapter(
    private var members: List<UserProfile>,
    private val onRemove: (UserProfile) -> Unit
) : RecyclerView.Adapter<GroupMemberAdapter.VH>() {

    inner class VH(val binding: ItemGroupMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val member = members[position]
        holder.binding.tvMemberName.text = member.name
        if (member.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView).load(member.photoUrl).into(holder.binding.ivMemberAvatar)
        }
        holder.binding.btnRemoveMember.setOnClickListener { onRemove(member) }
    }

    override fun getItemCount() = members.size

    fun updateData(newMembers: List<UserProfile>) {
        members = newMembers
        notifyDataSetChanged()
    }
}
