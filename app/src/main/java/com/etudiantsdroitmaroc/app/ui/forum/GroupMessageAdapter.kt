package com.etudiantsdroitmaroc.app.ui.forum

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.GroupMessage
import com.etudiantsdroitmaroc.app.databinding.ItemGroupMessageBinding
import com.google.firebase.auth.FirebaseAuth

class GroupMessageAdapter(
    private var messages: List<GroupMessage>,
    private val isAdmin: Boolean,
    private val onLongPressDelete: (GroupMessage) -> Unit
) : RecyclerView.Adapter<GroupMessageAdapter.VH>() {

    private val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    inner class VH(val binding: ItemGroupMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGroupMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val message = messages[position]
        val isMine = message.senderUid == myUid

        holder.binding.tvSenderName.text = if (isMine) "أنت" else message.senderName
        holder.binding.tvSenderName.visibility = if (isMine) View.GONE else View.VISIBLE

        holder.binding.rootContainer.gravity = if (isMine) Gravity.END else Gravity.START

        val bubble = holder.binding.tvMessageText.parent as View
        bubble.setBackgroundResource(if (isMine) R.drawable.bg_bubble_me else R.drawable.bg_bubble_other)

        if (message.type == "image" && message.imageUrl.isNotEmpty()) {
            holder.binding.ivMessageImage.visibility = View.VISIBLE
            holder.binding.tvMessageText.visibility = View.GONE
            Glide.with(holder.itemView).load(message.imageUrl).into(holder.binding.ivMessageImage)
        } else {
            holder.binding.ivMessageImage.visibility = View.GONE
            holder.binding.tvMessageText.visibility = View.VISIBLE
            holder.binding.tvMessageText.text = message.text
        }

        // الأدمين (منشئ المجموعة) يقدر يحيد أي رسالة؛ العضو يقدر يحيد رسالته هو فقط
        if (isAdmin || isMine) {
            bubble.setOnLongClickListener {
                onLongPressDelete(message)
                true
            }
        } else {
            bubble.setOnLongClickListener(null)
        }
    }

    override fun getItemCount() = messages.size

    fun updateData(newMessages: List<GroupMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
