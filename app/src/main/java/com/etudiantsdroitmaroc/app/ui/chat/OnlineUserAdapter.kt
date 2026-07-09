package com.etudiantsdroitmaroc.app.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.etudiantsdroitmaroc.app.databinding.ItemOnlineUserBinding

class OnlineUserAdapter(
    private var users: List<UserProfile>,
    private val onClick: (UserProfile) -> Unit
) : RecyclerView.Adapter<OnlineUserAdapter.VH>() {

    inner class VH(val binding: ItemOnlineUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOnlineUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = users[position]
        holder.binding.tvName.text = user.name
        if (user.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView).load(user.photoUrl).into(holder.binding.ivAvatar)
        }
        holder.binding.root.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<UserProfile>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
