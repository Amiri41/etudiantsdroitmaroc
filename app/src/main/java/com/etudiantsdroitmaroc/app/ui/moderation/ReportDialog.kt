package com.etudiantsdroitmaroc.app.ui.moderation

import android.app.AlertDialog
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
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

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        val padding = (16 * context.resources.displayMetrics.density).toInt()
        layout.setPadding(padding, padding, padding, padding)

        val reasonList = android.widget.ListView(context)
        reasonList.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_single_choice, REPORT_REASONS)
        reasonList.choiceMode = android.widget.ListView.CHOICE_MODE_SINGLE
        reasonList.setItemChecked(0, true)
        reasonList.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (220 * context.resources.displayMetrics.density).toInt()
        )
        layout.addView(reasonList)

        val detailsInput = EditText(context)
        detailsInput.hint = "تفاصيل إضافية (اختياري)"
        layout.addView(detailsInput)

        AlertDialog.Builder(context)
            .setTitle("الإبلاغ عن هذا المحتوى")
            .setView(layout)
            .setPositiveButton("إرسال الإبلاغ") { _, _ ->
                val selected = reasonList.checkedItemPosition
                val reason = if (selected >= 0) REPORT_REASONS[selected] else REPORT_REASONS[0]
                val details = detailsInput.text?.toString()?.trim().orEmpty()
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
            .setNegativeButton("إلغاء", null)
            .show()
    }

    /** كنعرض تأكيد قبل حظر مستخدم */
    fun confirmBlock(
        context: Context,
        scope: LifecycleCoroutineScope,
        targetUid: String,
        targetName: String,
        onBlocked: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
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
                        Toast.makeText(context, "فشل الحظر، عاود حاول", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
