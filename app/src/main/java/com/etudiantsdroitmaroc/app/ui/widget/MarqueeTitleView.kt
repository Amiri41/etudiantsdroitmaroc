package com.etudiantsdroitmaroc.app.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.TextDirectionHeuristics
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.etudiantsdroitmaroc.app.R

/**
 * عنوان فسطر واحد:
 * - إلا كان قصير وكيدخل فالعرض: كيبقى ثابت.
 * - إلا كان كبر من العرض: كيتحرك أوتوماتيك من اليمين لليسار بشكل مستمر (ticker).
 */
class MarqueeTitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val textView = TextView(context).apply {
        setSingleLine(true)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
    }
    private var animator: ValueAnimator? = null

    init {
        addView(textView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    fun setTitle(title: String) {
        stopMarquee()
        textView.text = title
        textView.translationX = 0f
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // نقيسو النص بعرضو الطبيعي (بلا تحديد) باش نعرفو واش كبر من العرض
        textView.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = textView.measuredHeight + paddingTop + paddingBottom
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val innerLeft = paddingLeft
        val innerRight = (right - left) - paddingRight
        val innerWidth = innerRight - innerLeft
        val textWidth = textView.measuredWidth
        val textHeight = textView.measuredHeight
        val t = paddingTop

        if (textWidth <= innerWidth) {
            // قصير: ثابت، مصطف حسب اتجاه النص (عربي = اليمين)
            val rtl = TextDirectionHeuristics.FIRSTSTRONG_LTR
                .isRtl(textView.text, 0, textView.text.length)
            val l = if (rtl) innerRight - textWidth else innerLeft
            textView.layout(l, t, l + textWidth, t + textHeight)
            textView.translationX = 0f
            stopMarquee()
        } else {
            textView.layout(innerLeft, t, innerLeft + textWidth, t + textHeight)
            startMarquee(innerWidth, textWidth)
        }
    }

    private fun startMarquee(containerWidth: Int, textWidth: Int) {
        if (animator?.isRunning == true) return
        if (!isAttachedToWindow) return
        val density = resources.displayMetrics.density
        val from = containerWidth.toFloat()          // يدخل من اليمين
        val to = -textWidth.toFloat()                // ويخرج من اليسار
        val distance = from - to
        val durationMs = (distance / (45f * density) * 1000f).toLong().coerceAtLeast(3000L)
        animator = ValueAnimator.ofFloat(from, to).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { textView.translationX = it.animatedValue as Float }
            start()
        }
    }

    private fun stopMarquee() {
        animator?.cancel()
        animator = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestLayout()
    }

    override fun onDetachedFromWindow() {
        stopMarquee()
        super.onDetachedFromWindow()
    }

    override fun dispatchDraw(canvas: Canvas) {
        // نقصو كل ما خرج من حدود العنوان (داخل الـ padding)
        canvas.save()
        canvas.clipRect(paddingLeft, 0, width - paddingRight, height)
        super.dispatchDraw(canvas)
        canvas.restore()
    }
}
