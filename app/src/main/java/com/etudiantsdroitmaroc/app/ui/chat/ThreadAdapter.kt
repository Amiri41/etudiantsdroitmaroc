package com.etudiantsdroitmaroc.app.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.data.model.ChatThread
import com.etudiantsdroitmaroc.app.databinding.ItemChatThreadBinding
import com.google.firebase.auth.FirebaseAuth

class ThreadAdapter(
    private var threads: List<ChatThread>,
    private val onClick: (ChatThread) -> Unit
) : RecyclerView.Adapter<ThreadAdapter.VH>() {

    private val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    inner class VH(val binding: ItemChatThreadBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemChatThreadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val thread = threads[position]
        val otherUid = thread.participantUids.firstOrNull { it != myUid } ?: ""
        val otherName = thread.participantNames[otherUid] ?: "طالب"

        holder.binding.tvName.text = otherName
        holder.binding.tvLastMessage.text = thread.lastMessage
        holder.binding.root.setOnClickListener { onClick(thread) }
    }

    override fun getItemCount() = threads.size

    fun updateData(newThreads: List<ChatThread>) {
        threads = newThreads
        notifyDataSetChanged()
    }
}
