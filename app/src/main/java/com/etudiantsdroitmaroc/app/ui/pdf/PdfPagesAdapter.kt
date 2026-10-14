package com.etudiantsdroitmaroc.app.ui.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.util.LruCache
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** كيعرض صفحات PDF بـ android.graphics.pdf.PdfRenderer (بلا مكتبات native) */
class PdfPagesAdapter(
    private val renderer: PdfRenderer,
    private val scope: CoroutineScope,
    private val renderDispatcher: CoroutineDispatcher,
    private val pageWidthPx: Int
) : RecyclerView.Adapter<PdfPagesAdapter.VH>() {

    class VH(val image: ImageView) : RecyclerView.ViewHolder(image) {
        var job: Job? = null
    }

    private val cache = object : LruCache<Int, Bitmap>(
        minOf(Runtime.getRuntime().maxMemory() / 6, 64L * 1024 * 1024).toInt()
    ) {
        override fun sizeOf(key: Int, value: Bitmap) = value.byteCount
    }

    // نسبة أول صفحة كتقدير لطول الصفحات قبل ما تتعرض
    private val firstRatio: Float = run {
        synchronized(renderer) {
            val p = renderer.openPage(0)
            val r = p.height.toFloat() / p.width.toFloat()
            p.close()
            r
        }
    }

    override fun getItemCount(): Int = renderer.pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val iv = ImageView(parent.context)
        iv.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = (6 * parent.resources.displayMetrics.density).toInt() }
        iv.adjustViewBounds = true
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        iv.setBackgroundColor(Color.WHITE)
        return VH(iv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.job?.cancel()
        val cached = cache.get(position)
        if (cached != null) {
            holder.image.setImageBitmap(cached)
            return
        }
        holder.image.setImageDrawable(null)
        holder.image.minimumHeight = (firstRatio * holder.image.resources.displayMetrics.widthPixels).toInt()
        holder.job = scope.launch {
            val bmp = withContext(renderDispatcher) { renderPage(position) } ?: return@launch
            cache.put(position, bmp)
            if (holder.bindingAdapterPosition == position) {
                holder.image.minimumHeight = 0
                holder.image.setImageBitmap(bmp)
            }
        }
    }

    override fun onViewRecycled(holder: VH) {
        holder.job?.cancel()
        holder.image.setImageDrawable(null)
        super.onViewRecycled(holder)
    }

    private fun renderPage(index: Int): Bitmap? = synchronized(renderer) {
        try {
            val page = renderer.openPage(index)
            val ratio = page.height.toFloat() / page.width.toFloat()
            val bmp = Bitmap.createBitmap(pageWidthPx, (pageWidthPx * ratio).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bmp
        } catch (e: Exception) {
            null
        }
    }
}
