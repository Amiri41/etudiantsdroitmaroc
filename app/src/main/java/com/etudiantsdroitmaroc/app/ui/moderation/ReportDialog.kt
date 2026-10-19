package com.etudiantsdroitmaroc.app.ui.moderation

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import com.etudiantsdroitmaroc.app.R
import com.etudiantsdroitmaroc.app.databinding.DialogReportBinding
import com.etudiantsdroitmaroc.app.ui.common.AppDialog
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.etudiantsdroitmaroc.app.data.remote.ChatRepository
import com.etudiantsdroitmaroc.app.data.remote.ModerationRepository
import kotlinx.coroutines.launch

/** أسباب الإبلاغ الجاهزة - كنعرضوها كلائحة اختيار بسيطة */
private val REPORT_REASONS = arrayOf(
    "سبام أو إعلان غير مرغوب فيه",
    "محتوى مسيء أو خطاب كراهية",
    "تحرش أو تهديد",
    "محتوى غير لائق",
    "معلومة كاذبة أو مضللة",
    "سبب آخر"
)

/** كنعرض dialog بسيط للإبلاغ عن محتوى (منشور/تعليق/رسالة/مستخدم) */
object ReportDialog {

    fun show(
        context: Context,
        scope: LifecycleCoroutineScope,
        targetType: String,
        targetId: String,
        targetOwnerUid: String,
        targetOwnerName: String
    ) {
        val repository = ModerationRepository()
        val b = DialogReportBinding.inflate(LayoutInflater.from(context))
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context).setView(b.root).create()

        // أسباب الإبلاغ كبطاقات اختيار (الأول محدد)
        val density = context.resources.displayMetrics.density
        REPORT_REASONS.forEachIndexed { index, reason ->
            val radio = RadioButton(context).apply {
                id = View.generateViewId()
                text = reason
                textSize = 15f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setPadding((8 * density).toInt(), 0, (8 * density).toInt(), 0)
                buttonTintList = ColorStateList.valueOf(0xFF1F63D6.toInt())
                setBackgroundResource(if (index == 0) R.drawable.bg_option_selected else R.drawable.bg_option)
                isChecked = index == 0
            }
            val lp = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT, (50 * density).toInt()
            ).apply { topMargin = (8 * density).toInt() }
            b.rgReasons.addView(radio, lp)
        }
        b.rgReasons.setOnCheckedChangeListener { group, checkedId ->
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                child.setBackgroundResource(
                    if (child.id == checkedId) R.drawable.bg_option_selected else R.drawable.bg_option
                )
            }
        }

        b.btnReportCancel.setOnClickListener { dialog.dismiss() }
        b.btnReportSend.setOnClickListener {
            val checkedId = b.rgReasons.checkedRadioButtonId
            val index = (0 until b.rgReasons.childCount).firstOrNull { b.rgReasons.getChildAt(it).id == checkedId } ?: 0
            val reason = REPORT_REASONS[index]
            val details = b.etReportDetails.text?.toString()?.trim().orEmpty()
            dialog.dismiss()
            scope.launch {
                val result = repository.submitReport(
                    targetType, targetId, targetOwnerUid, targetOwnerName, reason, details
                )
                if (result.isSuccess) {
                    Toast.makeText(context, "تم إرسال الإبلاغ، غادي نراجعوه ✅", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "فشل إرسال الإبلاغ، عاود حاول", Toast.LENGTH_SHORT).show()
                }
            }
        }
        AppDialog.styleWindow(context, dialog)
    }

    /** كنعرض تأكيد قبل حظر مستخدم */
    fun confirmBlock(
        context: Context,
        scope: LifecycleCoroutineScope,
        targetUid: String,
        targetName: String,
        onBlocked: (() -> Unit)? = null
    ) {
        AppDialog.Builder(context)
            .setTitle("حظر $targetName")
            .setMessage("ملي تحظر هاد المستخدم، ماغاديش تشوف منشوراته ولا رسائله، وهو ماغاديش يقدر يبعتلك رسائل جداد.")
            .setPositiveButton("حظر") { _, _ ->
                scope.launch {
                    val repository = ChatRepository()
                    try {
                        repository.blockUser(targetUid)
                        Toast.makeText(context, "تم حظر $targetName", Toast.LENGTH_SHORT).show()
                        onBlocked?.invoke()
                    } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
                        Toast.makeText(context, "فشل الحظر، عاود حاول", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
