package com.etudiantsdroitmaroc.app.ui.lessons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.PdfDocument
import com.etudiantsdroitmaroc.app.data.model.VideoLesson
import com.etudiantsdroitmaroc.app.databinding.ItemLessonCardBinding

/** بطاقة موحدة: PDF أو فيديو (الصورة، العنوان، الوصف) */
data class LessonCard(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val pdf: PdfDocument? = null,
    val video: VideoLesson? = null
)

class LessonCardAdapter(
    private var items: List<LessonCard>,
    private val isDownloaded: (String) -> Boolean,
    private val onOpen: (LessonCard) -> Unit,
    private val onDownload: (PdfDocument) -> Unit
) : RecyclerView.Adapter<LessonCardAdapter.VH>() {

    inner class VH(val binding: ItemLessonCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemLessonCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val card = items[position]
        val b = holder.binding

        b.marqueeTitle.setTitle(card.title)

        // الصورة
        val placeholder = if (card.video != null) 0 else R.drawable.ic_pdf
        if (card.imageUrl.isNotBlank()) {
            val req = Glide.with(b.ivThumbnail).load(card.imageUrl).centerCrop()
            if (placeholder != 0) req.placeholder(placeholder).error(placeholder)
            req.into(b.ivThumbnail)
        } else if (placeholder != 0) {
            Glide.with(b.ivThumbnail).clear(b.ivThumbnail)
            b.ivThumbnail.setImageResource(placeholder)
        } else {
            Glide.with(b.ivThumbnail).clear(b.ivThumbnail)
            b.ivThumbnail.setImageDrawable(null)
        }

        // فيديو: أيقونة التشغيل / PDF: حالة التحميل
        b.ivPlay.visibility = if (card.video != null) View.VISIBLE else View.GONE
        if (card.pdf != null) {
            val downloaded = isDownloaded(card.pdf.id)
            b.ivDownloadStatus.visibility = View.VISIBLE
            b.ivDownloadStatus.setImageResource(if (downloaded) R.drawable.ic_check_circle else R.drawable.ic_download)
            b.ivDownloadStatus.setOnClickListener {
                if (!downloaded) onDownload(card.pdf) else onOpen(card)
            }
        } else {
            b.ivDownloadStatus.visibility = View.GONE
            b.ivDownloadStatus.setOnClickListener(null)
        }

        // الوصف: 4 أسطر كحد أقصى مع "..." والضغط عليه كيفتحو كامل (أو كيصغرو)
        val tv = b.tvDescription
        if (card.description.isBlank()) {
            tv.visibility = View.GONE
        } else {
            tv.visibility = View.VISIBLE
            tv.text = card.description
            tv.maxLines = COLLAPSED_LINES
            tv.ellipsize = android.text.TextUtils.TruncateAt.END
            tv.setOnClickListener {
                val expanded = tv.maxLines == Int.MAX_VALUE
                tv.maxLines = if (expanded) COLLAPSED_LINES else Int.MAX_VALUE
                tv.ellipsize = if (expanded) android.text.TextUtils.TruncateAt.END else null
            }
        }

        b.root.setOnClickListener { onOpen(card) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<LessonCard>) {
        items = newItems
        notifyDataSetChanged()
    }

    companion object {
        private const val COLLAPSED_LINES = 4
    }
}
