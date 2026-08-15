package com.etudiantsdroitmaroc.app.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

/**
 * تحميل موحّد للصور: كاش على القرص + تصغير تلقائي حسب حجم الـ ImageView
 * + thumbnail خفيف يبان بسرعة قبل الصورة الكاملة. هادشي كيقلل بزاف
 * من التهنيق (jank) ملي كيدوز المستخدم فلائحة فيها بزاف ديال الصور
 * (المنشورات، الرسائل، صور البروفايل).
 */
object ImageLoader {

    private val defaultOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)

    fun load(imageView: ImageView, url: String?, placeholderRes: Int? = null) {
        if (url.isNullOrEmpty()) return
        var request = Glide.with(imageView.context)
            .load(url)
            .apply(defaultOptions)
            .thumbnail(0.2f)
            .centerCrop()

        if (placeholderRes != null) {
            request = request.placeholder(placeholderRes)
        }

        request.into(imageView)
    }
}
