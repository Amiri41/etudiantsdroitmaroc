package com.etudiantsdroitmaroc.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.databinding.ItemNavButtonBinding

data class NavItem(val id: String, val title: String)

/** أزرار عمودية عريضة (بحال صفحة "حسابي")، والعنوان الطويل كيتحرك marquee */
class NavButtonAdapter(
    private val onClick: (NavItem) -> Unit
) : ListAdapter<NavItem, NavButtonAdapter.VH>(Diff) {

    class VH(val b: ItemNavButtonBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemNavButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.b.root.text = item.title
        holder.b.root.isSelected = true
        holder.b.root.setOnClickListener { onClick(item) }
    }

    companion object Diff : DiffUtil.ItemCallback<NavItem>() {
        override fun areItemsTheSame(a: NavItem, b: NavItem) = a.id == b.id
        override fun areContentsTheSame(a: NavItem, b: NavItem) = a == b
    }
}
