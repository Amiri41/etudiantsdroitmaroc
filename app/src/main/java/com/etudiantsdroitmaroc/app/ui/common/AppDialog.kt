package com.etudiantsdroitmaroc.app.ui.common

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.databinding.DialogAppBinding

/**
 * نافذة موحدة بستيل التطبيق الجديد (هيدر متدرج + أيقونة + أزرار دائرية).
 * نفس واجهة AlertDialog.Builder (setTitle/setMessage/setPositiveButton/setNegativeButton)
 * باش نبدلو الستايل فكل التطبيق بلا ما نعاودو نكتبو المنطق.
 * الزر ديال الحذف/المسح/الحظر كيبان بالأحمر أوتوماتيك.
 */
object AppDialog {

    private val DANGER_WORDS = listOf("حذف", "مسح", "حظر", "خروج", "طرد", "مغادرة", "إزالة")

    class Builder(private val context: Context) {
        private var title: CharSequence = ""
        private var message: CharSequence = ""
        private var iconRes: Int? = null
        private var danger: Boolean? = null
        private var positiveText: CharSequence? = null
        private var negativeText: CharSequence? = null
        private var positive: ((DialogInterface, Int) -> Unit)? = null
        private var negative: ((DialogInterface, Int) -> Unit)? = null

        fun setTitle(t: CharSequence) = apply { title = t }
        fun setMessage(m: CharSequence) = apply { message = m }
        fun setIcon(@DrawableRes res: Int) = apply { iconRes = res }
        fun setDanger(value: Boolean) = apply { danger = value }

        fun setPositiveButton(text: CharSequence, listener: ((DialogInterface, Int) -> Unit)? = null) = apply {
            positiveText = text
            positive = listener
        }

        fun setNegativeButton(text: CharSequence, listener: ((DialogInterface, Int) -> Unit)? = null) = apply {
            negativeText = text
            negative = listener
        }

        fun show(): AlertDialog {
            val b = DialogAppBinding.inflate(LayoutInflater.from(context))
            val dialog = AlertDialog.Builder(context).setView(b.root).create()

            val isDanger = danger ?: DANGER_WORDS.any { positiveText?.contains(it) == true }
            b.tvDialogTitle.text = title
            b.tvDialogMessage.text = message
            b.tvDialogMessage.visibility = if (message.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            b.ivDialogIcon.setImageResource(
                iconRes ?: if (isDanger) R.drawable.ic_warning else R.drawable.ic_info
            )

            val pos = positiveText
            if (pos != null) {
                b.btnDialogPositive.text = pos
                if (isDanger) b.btnDialogPositive.setBackgroundResource(R.drawable.bg_btn_danger)
                b.btnDialogPositive.setOnClickListener {
                    dialog.dismiss()
                    positive?.invoke(dialog, DialogInterface.BUTTON_POSITIVE)
                }
            } else {
                b.btnDialogPositive.visibility = android.view.View.GONE
            }

            val neg = negativeText
            if (neg != null) {
                b.btnDialogNegative.text = neg
                b.btnDialogNegative.setOnClickListener {
                    dialog.dismiss()
                    negative?.invoke(dialog, DialogInterface.BUTTON_NEGATIVE)
                }
            } else {
                b.btnDialogNegative.visibility = android.view.View.GONE
            }

            styleWindow(context, dialog)
            return dialog
        }
    }

    /** خلفية شفافة (باش تبان الزوايا المدورة) وعرض 92% من الشاشة */
    fun styleWindow(context: Context, dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
