package com.etudiantsdroitmaroc.app.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.ChatMessage
import com.etudiantsdroitmaroc.app.databinding.ItemMessageBinding
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(private var messages: List<ChatMessage>) :
    RecyclerView.Adapter<MessageAdapter.VH>() {

    private val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    inner class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val message = messages[position]
        holder.binding.tvMessageText.text = message.text

        val isMine = message.senderUid == myUid
        holder.binding.rootContainer.gravity = if (isMine) Gravity.END else Gravity.START
        holder.binding.tvMessageText.setBackgroundResource(
            if (isMine) R.drawable.bg_bubble_me else R.drawable.bg_bubble_other
        )
        holder.binding.tvMessageText.setTextColor(
            holder.itemView.context.getColor(if (isMine) R.color.white else R.color.text_primary)
        )
    }

    override fun getItemCount() = messages.size

    fun updateData(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
