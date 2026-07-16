package com.etudiantsdroitmaroc.app.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.data.model.UserProfile
import com.etudiantsdroitmaroc.app.databinding.ItemPersonCardBinding

enum class PersonMode { SUGGESTION, FRIEND }

data class PersonItem(
    val profile: UserProfile,
    var status: String = "none" // "none" | "pending" | "friend" (خاص بـ SUGGESTION mode)
)

class PeopleAdapter(
    private var items: List<PersonItem>,
    private val mode: PersonMode,
    private val onClickProfile: (UserProfile) -> Unit,
    private val onPrimaryAction: (PersonItem) -> Unit
) : RecyclerView.Adapter<PeopleAdapter.VH>() {

    inner class VH(val binding: ItemPersonCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPersonCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val profile = item.profile

        holder.binding.tvName.text = profile.name
        val subtitle = listOfNotNull(
            profile.university.takeIf { it.isNotEmpty() },
            profile.level.takeIf { it.isNotEmpty() }
        ).joinToString(" · ")
        holder.binding.tvSubtitle.text = subtitle

        if (profile.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView).load(profile.photoUrl).into(holder.binding.ivAvatar)
        }

        holder.binding.cardRoot.setOnClickListener { onClickProfile(profile) }

        val btn = holder.binding.btnPrimaryAction
        when (mode) {
            PersonMode.FRIEND -> {
                btn.text = "إزالة"
                btn.isEnabled = true
            }
            PersonMode.SUGGESTION -> {
                when (item.status) {
                    "friend" -> { btn.text = "أصدقاء ✓"; btn.isEnabled = false }
                    "pending" -> { btn.text = "طلب مرسل ⏳"; btn.isEnabled = false }
                    else -> { btn.text = "إضافة صديق"; btn.isEnabled = true }
                }
            }
        }
        btn.setOnClickListener { onPrimaryAction(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<PersonItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
