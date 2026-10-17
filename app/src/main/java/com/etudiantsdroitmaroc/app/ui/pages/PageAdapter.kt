package com.etudiantsdroitmaroc.app.ui.pages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.AppPage
import com.etudiantsdroitmaroc.app.databinding.ItemPageBinding

class PageAdapter(
    private var pages: List<AppPage>,
    private val onClick: (AppPage) -> Unit
) : RecyclerView.Adapter<PageAdapter.VH>() {

    inner class VH(val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root)

    private val bgs = intArrayOf(
        R.drawable.bg_icon_blue, R.drawable.bg_icon_purple, R.drawable.bg_icon_green,
        R.drawable.bg_icon_orange, R.drawable.bg_icon_red
    )

    /** الأيقونة كتتختار من كلمات العنوان، والافتراضي ورقة */
    private fun iconFor(title: String): Int = when {
        title.contains("نحن") -> R.drawable.ic_law_scale
        title.contains("خصوصية") -> R.drawable.ic_verified
        title.contains("قواعد") || title.contains("مجتمع") -> R.drawable.ic_people
        title.contains("شروط") -> R.drawable.ic_description
        title.contains("تواصل") || title.contains("اتصل") -> R.drawable.ic_email
        else -> R.drawable.ic_description
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val page = pages[position]
        val b = holder.binding
        b.tvPageTitle.text = page.title
        b.tvPageSubtitle.text = page.subtitle
        b.tvPageSubtitle.visibility = if (page.subtitle.isBlank()) View.GONE else View.VISIBLE
        b.ivPageIcon.setImageResource(iconFor(page.title))
        b.ivPageIcon.setBackgroundResource(bgs[position % bgs.size])
        b.root.setOnClickListener { onClick(page) }
    }

    override fun getItemCount() = pages.size

    fun updateData(newPages: List<AppPage>) {
        pages = newPages
        notifyDataSetChanged()
    }
}
