package com.etudiantsdroitmaroc.app.utils

/**
 * رسائل أخطاء مفهومة للمستخدم بدل النصوص التقنية (بحال "Job was cancelled").
 * الخطأ الحقيقي كيتسجل فـ Logcat باش المطور يقدر يشوفو.
 */
object ErrorUtils {
    fun friendly(e: Throwable): String {
        android.util.Log.w("EDM", "handled error: ${e.javaClass.simpleName}: ${e.message}", e)
        val msg = e.message.orEmpty()
        val name = e.javaClass.simpleName
        return when {
            e is com.etudiantsdroitmaroc.app.utils.SlowConnectionException ->
                "الاتصال ضعيف والسيرفر ما جاوبش، عاود المحاولة"
            e is java.io.IOException ||
                name.contains("Network", ignoreCase = true) ||
                msg.contains("UNAVAILABLE", ignoreCase = true) ||
                msg.contains("offline", ignoreCase = true) ||
                msg.contains("Unable to resolve host", ignoreCase = true) ->
                "تحقق من الاتصال بالإنترنت وعاود المحاولة"
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "ما عندكش الصلاحية لهاد العملية"
            msg.contains("FAILED_PRECONDITION", ignoreCase = true) || msg.contains("requires an index", ignoreCase = true) ->
                "الاستعلام محتاج index فـ Firebase (شوف Logcat: فيه رابط الإنشاء)"
            else -> "وقع مشكل مؤقت، عاود المحاولة"
        }
    }
}
