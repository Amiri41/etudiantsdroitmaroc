package com.etudiantsdroitmaroc.app.ui.pdf

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView

/** كيكبّر ويصغّر (pinch) والضغط المزدوج على الـ RecyclerView اللي جواه */
class ZoomLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var scale = 1f
    private var tx = 0f
    private val maxScale = 4f

    private val child: RecyclerView?
        get() = if (childCount > 0) getChildAt(0) as? RecyclerView else null

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomTo((scale * detector.scaleFactor).coerceIn(1f, maxScale), detector.focusX, detector.focusY)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onDoubleTap(e: MotionEvent): Boolean {
            zoomTo(if (scale > 1.2f) 1f else 2.5f, e.x, e.y)
            return true
        }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (scale > 1f && !scaleDetector.isInProgress) {
                tx -= dx
                apply()
            }
            return false
        }
    })

    private fun zoomTo(newScale: Float, fx: Float, fy: Float) {
        val rv = child ?: return
        val old = scale
        if (newScale == old) return
        // نخلي النقطة اللي تحت الصبع ثابتة
        val contentX = (fx - tx) / old
        tx = fx - newScale * contentX
        rv.scrollBy(0, (fy / old - fy / newScale).toInt())
        scale = newScale
        apply()
    }

    private fun apply() {
        val rv = child ?: return
        val minTx = width - width * scale
        tx = if (scale <= 1f) 0f else tx.coerceIn(minTx, 0f)
        rv.pivotX = 0f
        rv.pivotY = 0f
        rv.scaleX = scale
        rv.scaleY = scale
        rv.translationX = tx
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        gestureDetector.onTouchEvent(ev)
        return scaleDetector.isInProgress
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(ev)
        gestureDetector.onTouchEvent(ev)
        return true
    }
}
