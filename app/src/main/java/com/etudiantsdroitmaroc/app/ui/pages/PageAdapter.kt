package com.etudiantsdroitmaroc.app.ui.pages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.data.model.AppPage
import com.etudiantsdroitmaroc.app.databinding.ItemPageBinding

class PageAdapter(
    private var pages: List<AppPage>,
    private val onClick: (AppPage) -> Unit
) : RecyclerView.Adapter<PageAdapter.VH>() {

    inner class VH(val binding: ItemPageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val page = pages[position]
        holder.binding.tvPageTitle.text = page.title
        holder.binding.root.setOnClickListener { onClick(page) }
    }

    override fun getItemCount() = pages.size

    fun updateData(newPages: List<AppPage>) {
        pages = newPages
        notifyDataSetChanged()
    }
}
