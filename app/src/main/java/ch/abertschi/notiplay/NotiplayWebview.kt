package ch.abertschi.notiplay

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.webkit.WebView



/**
 * Created by abertschi on 04.02.18.
 */
class NotiplayWebview(context: Context?) : WebView(context) {

    var allowScroll: Boolean = false
    private var scaleDetector: ScaleGestureDetector? = null
    var scaleFactor = 1f


    init {
//        scaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    public override fun overScrollBy(deltaX: Int, deltaY: Int, scrollX: Int, scrollY: Int,
                                     scrollRangeX: Int, scrollRangeY: Int, maxOverScrollX: Int,
                                     maxOverScrollY: Int, isTouchEvent: Boolean): Boolean {
        if (allowScroll) return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent)
        return true
    }

    override fun scrollTo(x: Int, y: Int) {
        if (allowScroll) return super.scrollTo(x, y)
    }

    override fun computeScroll() {
        if (allowScroll) return super.computeScroll()
        
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Let the ScaleGestureDetector inspect all events.
        return true
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.save()
        canvas?.scale(scaleFactor, scaleFactor)
        canvas?.restore()
        println("drawing canvas " + scaleFactor)

    }
}