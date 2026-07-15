package com.etudiantsdroitmaroc.app.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.FriendRequest
import com.etudiantsdroitmaroc.app.databinding.ItemFriendRequestBinding

class FriendRequestAdapter(
    private var requests: List<FriendRequest>,
    private val onAccept: (FriendRequest) -> Unit,
    private val onDecline: (FriendRequest) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.VH>() {

    inner class VH(val binding: ItemFriendRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFriendRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val req = requests[position]
        holder.binding.tvName.text = req.fromName
        if (req.fromPhoto.isNotEmpty()) {
            Glide.with(holder.itemView).load(req.fromPhoto).into(holder.binding.ivAvatar)
        }
        holder.binding.btnAccept.setOnClickListener { onAccept(req) }
        holder.binding.btnDecline.setOnClickListener { onDecline(req) }
    }

    override fun getItemCount() = requests.size

    fun updateData(newRequests: List<FriendRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}
