package com.etudiantsdroitmaroc.app.utils

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * كنطلبو من المستخدم يقيّم التطبيق عبر النافذة الرسمية ديال Google Play
 * (بلا ما نخرجوه من التطبيق). كنطلبوها غير بعد استعمال حقيقي، وماشي كل مرة.
 */
object ReviewHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_ACTION_COUNT = "positive_action_count"
    private const val KEY_REVIEW_ASKED = "review_already_asked"
    private const val THRESHOLD = 5 // بعد 5 أفعال إيجابية (تحميل PDF، مشاهدة فيديو...) نطلبو التقييم

    /** كنزيدو نقطة كل مرة المستخدم كيدير فعل إيجابي (تحميل، مشاهدة فيديو...) */
    fun recordPositiveAction(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyAsked = prefs.getBoolean(KEY_REVIEW_ASKED, false)
        if (alreadyAsked) return

        val count = prefs.getInt(KEY_ACTION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_ACTION_COUNT, count).apply()

        if (count >= THRESHOLD && context is Activity) {
            requestReview(context)
        }
    }

    private fun requestReview(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo)
                // نعلمو بلي طلبنا، باش ماعادش نزعجو المستخدم مرة أخرى
                prefs.edit().putBoolean(KEY_REVIEW_ASKED, true).apply()
            }
        }
    }
}
