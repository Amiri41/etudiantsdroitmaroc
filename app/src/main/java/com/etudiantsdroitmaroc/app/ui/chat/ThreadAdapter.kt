package com.etudiantsdroitmaroc.app.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.ChatThread
import com.etudiantsdroitmaroc.app.databinding.ItemChatThreadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ThreadAdapter(
    private var threads: List<ChatThread>,
    private val onClick: (ChatThread) -> Unit
) : RecyclerView.Adapter<ThreadAdapter.VH>() {

    private val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val photoCache = HashMap<String, String>()

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

        holder.binding.ivAvatar.setImageResource(R.drawable.ic_profile)
        val cachedUrl = photoCache[otherUid]
        if (cachedUrl != null) {
            if (cachedUrl.isNotEmpty()) {
                Glide.with(holder.itemView).load(cachedUrl).into(holder.binding.ivAvatar)
            }
        } else if (otherUid.isNotEmpty()) {
            Firebase.firestore.collection("users").document(otherUid).get()
                .addOnSuccessListener { doc ->
                    val url = doc.getString("photoUrl") ?: ""
                    photoCache[otherUid] = url
                    if (url.isNotEmpty() && holder.bindingAdapterPosition == position) {
                        Glide.with(holder.itemView).load(url).into(holder.binding.ivAvatar)
                    }
                }
        }
    }

    override fun getItemCount() = threads.size

    fun updateData(newThreads: List<ChatThread>) {
        threads = newThreads
        notifyDataSetChanged()
    }
}
