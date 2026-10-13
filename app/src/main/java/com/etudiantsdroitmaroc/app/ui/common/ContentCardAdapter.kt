package com.etudiantsdroitmaroc.app.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.databinding.ItemContentCardBinding

/** بطاقة محتوى (PDF ولا فيديو): صورة، عنوان (marquee إلا كان طويل)، ووصف فـ4 سطور ماكس */
data class ContentCard(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val placeholderRes: Int,
    val downloaded: Boolean? = null, // null = ما كاينش أيقونة التحميل (فيديو)
    val isVideo: Boolean = false
)

class ContentCardAdapter(
    private val onClick: (ContentCard) -> Unit,
    private val onStatusClick: (ContentCard) -> Unit = {}
) : ListAdapter<ContentCard, ContentCardAdapter.VH>(Diff) {

    class VH(val b: ItemContentCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemContentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = getItem(position)
        val b = holder.b
        b.tvCardTitle.text = c.title
        b.tvCardTitle.isSelected = true
        b.tvCardDesc.text = c.description
        b.tvCardDesc.visibility = if (c.description.isBlank()) View.GONE else View.VISIBLE
        b.ivCardPlay.visibility = if (c.isVideo) View.VISIBLE else View.GONE

        val d = c.downloaded
        if (d == null) {
            b.ivCardStatus.visibility = View.GONE
        } else {
            b.ivCardStatus.visibility = View.VISIBLE
            b.ivCardStatus.setImageResource(if (d) R.drawable.ic_check_circle else R.drawable.ic_download)
        }

        if (c.imageUrl.isBlank()) {
            Glide.with(b.ivCardImage).clear(b.ivCardImage)
            b.ivCardImage.setImageResource(c.placeholderRes)
        } else {
            Glide.with(b.ivCardImage)
                .load(c.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(c.placeholderRes)
                .error(c.placeholderRes)
                .centerCrop()
                .into(b.ivCardImage)
        }

        b.root.setOnClickListener { onClick(c) }
        b.ivCardStatus.setOnClickListener { onStatusClick(c) }
    }

    companion object Diff : DiffUtil.ItemCallback<ContentCard>() {
        override fun areItemsTheSame(a: ContentCard, b: ContentCard) = a.id == b.id
        override fun areContentsTheSame(a: ContentCard, b: ContentCard) = a == b
    }
}
