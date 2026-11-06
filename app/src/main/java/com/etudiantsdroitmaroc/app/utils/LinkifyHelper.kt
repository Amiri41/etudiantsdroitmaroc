package com.etudiantsdroitmaroc.app.utils

import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.util.Linkify
import android.view.MotionEvent
import android.widget.TextView

/**
 * كيخلي الروابط (http/https/www) قابلة للضغط فالمنشورات، التعليقات والرسائل.
 * الضغط خارج الرابط كيمشي للعنصر (item) كيف كان، ماشي كيتبلوكا.
 */
object LinkifyHelper {

    private val linkOnlyMovement = object : LinkMovementMethod() {
        override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
            val action = event.action
            if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_DOWN) return false
            val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            val layout = widget.layout ?: return false
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val links = buffer.getSpans(off, off, ClickableSpan::class.java)
            if (links.isEmpty()) return false
            if (action == MotionEvent.ACTION_UP) links[0].onClick(widget)
            return true
        }
    }

    fun setText(tv: TextView, text: CharSequence?) {
        val t = text?.toString().orEmpty()
        tv.text = t
        val maybeLink = t.contains("http", ignoreCase = true) || t.contains("www.", ignoreCase = true)
        if (maybeLink && Linkify.addLinks(tv, Linkify.WEB_URLS)) {
            tv.movementMethod = linkOnlyMovement
            tv.isClickable = false       // باش الضغط خارج الرابط يوصل للعنصر
            tv.isLongClickable = false
            tv.setLinkTextColor(tv.currentTextColor)   // نفس لون النص (مع خط تحت) باش يبقى واضح فأي خلفية
        } else {
            tv.movementMethod = null
        }
    }
}
