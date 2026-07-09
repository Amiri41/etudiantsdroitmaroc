package com.etudiantsdroitmaroc.app.ui.subject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.PdfDocument
import com.etudiantsdroitmaroc.app.databinding.ItemPdfBinding

class PdfAdapter(
    private var pdfs: List<PdfDocument>,
    private val isDownloaded: (String) -> Boolean,
    private val onOpen: (PdfDocument) -> Unit,
    private val onDownload: (PdfDocument) -> Unit
) : RecyclerView.Adapter<PdfAdapter.PdfViewHolder>() {

    inner class PdfViewHolder(val binding: ItemPdfBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val binding = ItemPdfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PdfViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        val pdf = pdfs[position]
        holder.binding.tvPdfTitle.text = pdf.title

        val downloaded = isDownloaded(pdf.id)
        holder.binding.ivDownloadStatus.setImageResource(
            if (downloaded) R.drawable.ic_check_circle else R.drawable.ic_download
        )

        holder.binding.root.setOnClickListener { onOpen(pdf) }
        holder.binding.ivDownloadStatus.setOnClickListener {
            if (!downloaded) onDownload(pdf) else onOpen(pdf)
        }
    }

    override fun getItemCount() = pdfs.size

    fun updateData(newPdfs: List<PdfDocument>) {
        pdfs = newPdfs
        notifyDataSetChanged()
    }
}
