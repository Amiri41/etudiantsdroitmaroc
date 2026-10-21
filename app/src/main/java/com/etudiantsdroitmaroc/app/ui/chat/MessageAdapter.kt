package com.etudiantsdroitmaroc.app.ui.chat

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.data.model.ChatMessage
import com.etudiantsdroitmaroc.app.databinding.ItemMessageBinding
import com.etudiantsdroitmaroc.app.utils.ImageLoader
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * رسائل بستيل واتساب: فقاعة بذيل صغير، الوقت وعلامات ✓ داخل الفقاعة، وفواصل تواريخ.
 * - ✓ رمادية = تبعتات، ✓✓ رمادية = وصلات لتطبيق المستقبل، ✓✓ زرقاء = تقراات.
 */
class MessageAdapter(
    private var messages: List<ChatMessage>,
    private val onReportClick: (ChatMessage) -> Unit = {},
    private val onInfoClick: (ChatMessage) -> Unit = {}
) : RecyclerView.Adapter<MessageAdapter.VH>() {

    private val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private var darkBackground = false
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ar"))

    inner class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    /** كنبدلو ألوان الفقاعات حسب الخلفية (فاتحة/داكنة) باش النص يبقى واضح */
    fun setDarkBackground(dark: Boolean) {
        if (darkBackground != dark) {
            darkBackground = dark
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val b = holder.binding
        val message = messages[position]
        val isMine = message.senderUid == myUid
        val dark = darkBackground
        val rtl = b.root.layoutDirection == View.LAYOUT_DIRECTION_RTL

        b.rootContainer.gravity = if (isMine) Gravity.END else Gravity.START

        // فاصل التاريخ
        val showDate = position == 0 || !sameDay(messages[position - 1].sentAt, message.sentAt)
        b.tvDateSeparator.visibility = if (showDate) View.VISIBLE else View.GONE
        if (showDate) b.tvDateSeparator.text = dayLabel(message.sentAt)

        // الفقاعة
        val bubbleColor = when {
            isMine && !dark -> 0xFFD9EAFF.toInt()
            isMine -> 0xFF1F63D6.toInt()
            !dark -> 0xFFFFFFFF.toInt()
            else -> 0xFF1B2A47.toInt()
        }
        val textColor = when {
            isMine && !dark -> 0xFF0A1F3D.toInt()
            !isMine && !dark -> 0xFF1A1A1A.toInt()
            else -> Color.WHITE
        }
        val metaColor = if (dark || (isMine && dark)) 0xFFC9D6EA.toInt() else 0xFF6B7C93.toInt()
        b.bubble.background = bubbleBackground(bubbleColor, isMine, rtl, b.root.resources.displayMetrics.density)

        if (message.type == "image" && message.imageUrl.isNotEmpty()) {
            b.tvMessageText.visibility = View.GONE
            b.ivMessageImage.visibility = View.VISIBLE
            ImageLoader.load(b.ivMessageImage, message.imageUrl)
        } else {
            b.ivMessageImage.visibility = View.GONE
            b.tvMessageText.visibility = View.VISIBLE
            b.tvMessageText.text = message.text
            b.tvMessageText.setTextColor(textColor)
        }

        // الوقت + حالة الرسالة
        b.tvTime.text = timeFormat.format(Date(message.sentAt))
        b.tvTime.setTextColor(metaColor)
        if (isMine) {
            b.ivStatus.visibility = View.VISIBLE
            val seen = message.seen
            val delivered = message.deliveredAt > 0L
            b.ivStatus.setImageResource(
                when {
                    message.pending -> R.drawable.ic_tick_pending
                    seen || delivered -> R.drawable.ic_tick_double
                    else -> R.drawable.ic_tick_single
                }
            )
            val tint = when {
                seen -> if (dark) 0xFF7FD3FF.toInt() else 0xFF1E88E5.toInt()
                else -> metaColor
            }
            b.ivStatus.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        } else {
            b.ivStatus.visibility = View.GONE
        }

        // ضغطة مطولة: رسائل الآخرين ← إبلاغ، رسائلي ← معلومات الرسالة
        b.bubble.setOnLongClickListener {
            if (isMine) onInfoClick(message) else onReportClick(message)
            true
        }
    }

    private fun bubbleBackground(color: Int, isMine: Boolean, rtl: Boolean, density: Float): GradientDrawable {
        val r = 16 * density
        val tail = 4 * density
        // الذيل كيكون فالركن العلوي من جهة المرسل: رسائلي فالنهاية (END)، رسائل الآخرين فالبداية (START)
        val tailOnRight = (isMine && !rtl) || (!isMine && rtl)
        val radii = if (tailOnRight) floatArrayOf(r, r, tail, tail, r, r, r, r)
        else floatArrayOf(tail, tail, r, r, r, r, r, r)
        return GradientDrawable().apply {
            setColor(color)
            cornerRadii = radii
        }
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private fun dayLabel(ts: Long): String {
        val now = System.currentTimeMillis()
        return when {
            sameDay(ts, now) -> "اليوم"
            sameDay(ts, now - 24L * 3600 * 1000) -> "أمس"
            else -> dateFormat.format(Date(ts))
        }
    }

    override fun getItemCount() = messages.size

    /**
     * كنستعملو DiffUtil بدل notifyDataSetChanged() باش ملي توصل رسالة جديدة
     * (بحال فالشات المباشر) ما نعاودوش نبنيو كل اللائحة من الصفر - غير
     * العنصر الجديد كيتزاد، وهادشي كيقلل التهنيق بزاف فالمحادثات الطويلة.
     */
    fun updateData(newMessages: List<ChatMessage>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = messages.size
            override fun getNewListSize() = newMessages.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition].id == newMessages[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition] == newMessages[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }
}
