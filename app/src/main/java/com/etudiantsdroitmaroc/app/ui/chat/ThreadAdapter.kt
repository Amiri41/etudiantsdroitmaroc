package com.etudiantsdroitmaroc.app.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.ChatThread
import com.etudiantsdroitmaroc.app.databinding.ItemChatThreadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ThreadAdapter(
    private var threads: List<ChatThread>,
    private val onClick: (ChatThread) -> Unit
) : RecyclerView.Adapter<ThreadAdapter.VH>() {

    private val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private data class UserCache(val photoUrl: String, val isOnline: Boolean)
    private val userCache = HashMap<String, UserCache>()

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
        holder.binding.tvTime.text = formatRelativeTime(thread.lastMessageAt)
        holder.binding.root.setOnClickListener { onClick(thread) }

        val unread = thread.unreadCounts[myUid] ?: 0L
        if (unread > 0) {
            holder.binding.tvUnreadBadge.visibility = View.VISIBLE
            holder.binding.tvUnreadBadge.text = if (unread > 99) "99+" else unread.toString()
            holder.binding.tvName.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            holder.binding.tvUnreadBadge.visibility = View.GONE
        }

        holder.binding.ivAvatar.setImageResource(R.drawable.ic_profile)
        holder.binding.onlineDot.visibility = View.GONE

        val cached = userCache[otherUid]
        if (cached != null) {
            applyUserInfo(holder, cached)
        } else if (otherUid.isNotEmpty()) {
            Firebase.firestore.collection("users").document(otherUid).get()
                .addOnSuccessListener { doc ->
                    val info = UserCache(
                        photoUrl = doc.getString("photoUrl") ?: "",
                        isOnline = doc.getBoolean("isOnline") ?: false
                    )
                    userCache[otherUid] = info
                    if (holder.bindingAdapterPosition == position) {
                        applyUserInfo(holder, info)
                    }
                }
        }
    }

    private fun applyUserInfo(holder: VH, info: UserCache) {
        if (info.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView).load(info.photoUrl).into(holder.binding.ivAvatar)
        }
        holder.binding.onlineDot.visibility = if (info.isOnline) View.VISIBLE else View.GONE
    }

    private fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        val diffMinutes = (now.timeInMillis - timestamp) / 60000

        return when {
            diffMinutes < 1 -> "الآن"
            diffMinutes < 60 -> "منذ ${diffMinutes} د"
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) &&
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) ->
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(then.time)
            diffMinutes < 60 * 24 * 2 -> "أمس"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(then.time)
        }
    }

    override fun getItemCount() = threads.size

    fun updateData(newThreads: List<ChatThread>) {
        threads = newThreads
        notifyDataSetChanged()
    }
}
