package com.etudiantsdroitmaroc.app.ui.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.ChatMessage
import com.etudiantsdroitmaroc.app.databinding.ItemMessageBinding
import com.etudiantsdroitmaroc.app.utils.ImageLoader
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(
    private var messages: List<ChatMessage>,
    private val onReportClick: (ChatMessage) -> Unit = {}
) :
    RecyclerView.Adapter<MessageAdapter.VH>() {

    private val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    inner class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val message = messages[position]
        val isMine = message.senderUid == myUid
        holder.binding.rootContainer.gravity = if (isMine) Gravity.END else Gravity.START

        if (message.type == "image" && message.imageUrl.isNotEmpty()) {
            holder.binding.tvMessageText.visibility = android.view.View.GONE
            holder.binding.ivMessageImage.visibility = android.view.View.VISIBLE
            ImageLoader.load(holder.binding.ivMessageImage, message.imageUrl)
        } else {
            holder.binding.ivMessageImage.visibility = android.view.View.GONE
            holder.binding.tvMessageText.visibility = android.view.View.VISIBLE
            holder.binding.tvMessageText.text = message.text
            holder.binding.tvMessageText.setBackgroundResource(
                if (isMine) R.drawable.bg_bubble_me else R.drawable.bg_bubble_other
            )
            holder.binding.tvMessageText.setTextColor(
                holder.itemView.context.getColor(if (isMine) R.color.white else R.color.text_primary)
            )
        }

        if (!isMine) {
            holder.binding.root.setOnLongClickListener {
                onReportClick(message)
                true
            }
        }
    }

    override fun getItemCount() = messages.size

    /**
     * كنستعملو DiffUtil بدل notifyDataSetChanged() باش ملي توصل رسالة جديدة
     * (بحال فالشات المباشر) ما نعاودوش نبنيو كل اللائحة من الصفر - غير
     * العنصر الجديد كيتزاد، وهادشي كيقلل التهنيق بزاف فالمحادثات الطويلة.
     */
    fun updateData(newMessages: List<ChatMessage>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = messages.size
            override fun getNewListSize() = newMessages.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition].id == newMessages[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition] == newMessages[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }
}
