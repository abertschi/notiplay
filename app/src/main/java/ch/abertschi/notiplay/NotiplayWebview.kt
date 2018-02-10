package ch.abertschi.notiplay

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.os.Build
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.DisplayMetrics
import android.view.*
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.view.animation.Animation
import android.view.animation.Transformation
import android.webkit.WebView
import android.widget.Toast
import org.jetbrains.anko.padding
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


/**
 * Created by abertschi on 04.02.18.
 */
class NotiplayWebview(context: Context) : WebView(context) {

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var showFloatingWindow: Boolean = true

    private var allowScroll: Boolean = false
    private var scaleFactor = 1f

    private var _viewPortHeight: Int = 0
    private var _viewPortWidth: Int = 0
    private var viewPortInitOrientation: Int = 0

    private var layoutParams: WindowManager.LayoutParams? = null
    private var storedLayoutParamsX: Int = 0
    private var storedLayoutParamsY: Int = 0
    private var storedLayoutParamsWidth: Int = 0
    private var storedLayoutParamsHeight: Int = 0
    private var actionScalingActive = false

    private var isFullScreen: Boolean = false

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f

    private var actionUpX: Int = 0
    private var actionUpY: Int = 0

    private var lastTapMs = System.currentTimeMillis()
    private val doubleTapThresholdMs = 400

    private val widthScale = (864.0 / 1652.0)

    var onFloatingWindowAction: (() -> Unit)? = null
    var onFullScreenAction: (() -> Unit)? = null

    var isAlphaActive = false


//    fun toggleFullScreen() {
//        if (isFullScreen) launchFloatingWindow()
//        else launchFullScreen()
//    }

    fun makeVisible(state: Boolean) {
        showFloatingWindow = state
        layoutParams?.run {
            if (state) {
                this.height = storedLayoutParamsHeight
                this.width = storedLayoutParamsWidth
            } else {
                this.height = 0
                this.width = 0
            }
        }
        windowManager.updateViewLayout(this, layoutParams)
    }

    fun loadLayout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams = WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT)
        } else {
            layoutParams = WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT)
        }

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        _viewPortHeight = displayMetrics.heightPixels
        _viewPortWidth = displayMetrics.widthPixels

        viewPortInitOrientation = resources.configuration.orientation

        layoutParams?.run {
            var width = 0
            if (_viewPortHeight < _viewPortWidth) {
                // wide
                width = (_viewPortHeight / 2.0).roundToInt()
            } else {
                // portrait mode
                width = (_viewPortWidth / 2.0).roundToInt()
            }

            layoutParams?.gravity = Gravity.LEFT or Gravity.TOP
            layoutParams?.x = _viewPortWidth
            layoutParams?.y = 0
            showFloatingWindow = true
            layoutParams?.width = if (showFloatingWindow) width else 0
            layoutParams?.height = if (showFloatingWindow) (width * widthScale).toInt() else 0
            storedLayoutParamsHeight = this.height
            storedLayoutParamsWidth = this.width
            padding = 0
            horizontalMargin = 0f
            verticalMargin = 0f

        }

        this.layoutTransition = LayoutTransition()

        setOnTouchListener { v, event ->

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val t = System.currentTimeMillis()
                    val delta = t - lastTapMs
                    if (delta < doubleTapThresholdMs) {
                        isFullScreen = if (!isFullScreen) {
                            onFullScreenAction?.invoke()
                            true
                        } else {
                            onFloatingWindowAction?.invoke()
                            true
                        }
                        return@setOnTouchListener true
                    }
                    lastTapMs = t
                    lastTouchX = event.x
                    lastTouchY = event.y
                    println(getViewCorrectedViewPortSize().first)
                }
                MotionEvent.ACTION_MOVE -> {
                    val w = layoutParams!!.width
                    val h = layoutParams!!.height
                    val screenSize = getViewCorrectedViewPortSize()
                    val padding = 5


                    if (!isFullScreen) {
                        isAlphaActive = true
                        if (event.rawX - event.x <= padding) {
                            val a = (event.x) / w
                            this@NotiplayWebview.alpha = a
                        } else if ((screenSize.first - (event.rawX + w - event.x).absoluteValue
                                        <= padding)) {
                            val a = (w - event.x) / w
                            println("got " + a)
                            this@NotiplayWebview.alpha = a

                        } else if (event.rawY - event.y <= padding) {
                            this@NotiplayWebview.alpha = event.y / h

                        } else if ((screenSize.second - (event.rawY + h - event.y).absoluteValue
                                        <= padding)) {
                            this@NotiplayWebview.alpha = (h - event.y) / h
                        } else {
                            this@NotiplayWebview.alpha = 1f
                            isAlphaActive = false
                        }
                    }

                    if (actionScalingActive) {
                        return@setOnTouchListener true
                    } else {
                        layoutParams?.y = (event.rawY - lastTouchY).toInt()
                        layoutParams?.x = (event.rawX - lastTouchX).toInt()
                        windowManager.updateViewLayout(this@NotiplayWebview, layoutParams)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isAlphaActive){
                        makeVisible(false)
                        Toast.makeText(this.context.applicationContext, "Floating Window disabled", Toast.LENGTH_SHORT)
                    } else {
                        alignFloatingWindow()
                    }

                    actionScalingActive = false
                }
            }
            return@setOnTouchListener !isFullScreen
        }
        windowManager.addView(this, layoutParams)
        alignFloatingWindow()
    }


    private fun alignFloatingWindow() {
        layoutParams?.run {
            actionUpX = if (x < 0) 0 else x
            actionUpY = if (y < 0) 0 else y
        }

        val anim = ValueAnimator.ofFloat(0.0f, 1.0f)
        anim.startDelay = 500
        anim.duration = 1000
        anim.interpolator = FastOutSlowInInterpolator()

        anim.addUpdateListener {
            val viewHeight = layoutParams!!.height
            val viewWidth = layoutParams!!.width

            val p = getViewCorrectedViewPortSize()
            val screenHeight = p.second
            val screenWidth = p.first
            val h = (screenHeight / 3.0).roundToInt()
            val w = (screenWidth / 2.0).roundToInt()

            val padding = getStatusBarHeight() / 2
            val targetY = when (actionUpY + viewHeight / 2) {
                in 0..h -> padding + getStatusBarHeight()
                in (h + 1)..(2 * h) -> padding + (screenHeight / 2) - (viewHeight / 2)
                else -> screenHeight - padding - viewHeight
            }
            val targetX = when (actionUpX + viewWidth / 2) {
                in 0..w -> padding
                else -> screenWidth - padding - viewWidth
            }
            val fraction = it.animatedFraction
            var xSign = 1
            var ySign = 1

            if (targetX <= actionUpX) xSign = -1
            if (targetY <= actionUpY) ySign = -1

            val dx = (actionUpX - targetX).absoluteValue * xSign * fraction
            val dy = (actionUpY - targetY).absoluteValue * ySign * fraction

            layoutParams?.run {
                x = actionUpX + dx.roundToInt()
                y = actionUpY + dy.roundToInt()
                windowManager.updateViewLayout(this@NotiplayWebview, this)
            }
        }
        anim.start()
    }

    // width, height
    fun getViewCorrectedViewPortSize(): Pair<Int, Int> {
        var p: Pair<Int, Int>?
        if (resources.configuration.orientation == viewPortInitOrientation) {
            p = Pair(_viewPortWidth, _viewPortHeight)
        } else {
            p = Pair(_viewPortHeight, _viewPortWidth)
        }
        return p
    }


    fun requestFullScreen() {
        storedLayoutParamsX = layoutParams!!.x
        storedLayoutParamsY = layoutParams!!.y
        storedLayoutParamsWidth = layoutParams!!.width
        storedLayoutParamsHeight = layoutParams!!.height
    }

    fun confirmFullScreen() {
        println("confirmed fullscreen")
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val width = displayMetrics.heightPixels
        val height = displayMetrics.widthPixels

        isFullScreen = true
        this.allowScroll = true

        layoutParams?.x = 0
        layoutParams?.y = 0
        layoutParams?.height = width
        layoutParams?.width = height
        layoutParams?.verticalMargin = 0f
        layoutParams?.horizontalMargin = 0f
        this.scaleX = 1f
        this.scaleY = 1f

        windowManager.updateViewLayout(this, layoutParams)
    }

    fun requestFloatingWindow() {

    }

    fun confirmFloatingWindow() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        isFullScreen = false
        allowScroll = false

//        val i = Intent(context, HorizontalFullscreenActivity::class.java)
//        i.addFlags(FLAG_ACTIVITY_SINGLE_TOP)
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        i.action = HorizontalFullscreenActivity.ACTION_QUIT_ACTIVITY
//        startActivity(context, i, null)
//        // activity for result?

        layoutParams!!.x = storedLayoutParamsX
        layoutParams!!.y = storedLayoutParamsY
        layoutParams!!.width = (storedLayoutParamsWidth).toInt()
        layoutParams!!.height = (storedLayoutParamsHeight).toInt()
        println(layoutParams!!.width)
        println(layoutParams!!.height)
        println(layoutParams!!.x)
        println(layoutParams!!.y)
        windowManager.updateViewLayout(this, layoutParams)

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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
//        canvas?.saveLayerAlpha(0f, 0f, canvas!!.getWidth().toFloat(), canvas!!.getHeight().toFloat(), 10, Canvas.ALL_SAVE_FLAG);
//        println("ondraw")
        canvas?.run {
            //            canvas.save()
            canvas.scale(scaleFactor, scaleFactor)
//            canvas.restore()
        }


    }


    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val x = e.x
            val y = e.y
            if (!isFullScreen) {
                isFullScreen = true
                onFullScreenAction?.invoke()
            }
            return true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            super.onScaleEnd(detector)
            println("scale ended")
            println("${layoutParams?.width} / ${layoutParams?.height}")

//            this@NotiplayWebview.scaleX = 1f
//            this@NotiplayWebview.scaleY = 1f
            this@NotiplayWebview.invalidate()
            layoutParams?.run {
                //                width =
//                height =
//                windowManager.updateViewLayout(this@NotiplayWebview, layoutParams)
                println("${layoutParams?.width} / ${layoutParams?.height}")
//                scaleX = 1f
//                scaleY = 1f
//                width = width * scaleFactor
            }
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
//            return false
            if (isFullScreen) return false
            actionScalingActive = true
            scaleFactor *= detector.scaleFactor
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 1.0f))
//            layoutParams?.s


            var newWidth = (storedLayoutParamsWidth * scaleFactor).toInt()
            var newHeight = (newWidth * widthScale).toInt()

            this@NotiplayWebview?.layoutParams?.run {


                width = newWidth
                height = newHeight
                windowManager.updateViewLayout(this@NotiplayWebview, this)
            }

//            this@NotiplayWebview.scaleX = scaleFactor
//            this@NotiplayWebview.scaleY = scaleFactor
            this@NotiplayWebview.pivotX = 0f
            this@NotiplayWebview.pivotY = 0f
            println(scaleFactor)
            return true
        }
    }


    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    inner class ResizeAnimation(var view: View, val targetHeight: Int, var startHeight: Int, var targetWidth: Int, var startWidth: Int) : Animation() {

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val newHeight = (startHeight + targetHeight * interpolatedTime).toInt()
            val newWidth = (startHeight + targetHeight * interpolatedTime).toInt()
            //to support decent animation, change new heigt as Nico S. recommended in comments
            //int newHeight = (int) (startHeight+(targetHeight - startHeight) * interpolatedTime);
            view.layoutParams.height = newHeight
            view.requestLayout()
        }

        override fun initialize(width: Int, height: Int, parentWidth: Int, parentHeight: Int) {
            super.initialize(width, height, parentWidth, parentHeight)
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    //    val targetX: Int, var startX: Int, var targetY: Int, var startY: Int
    inner class MoveAnimation(var view: NotiplayWebview) : Animation() {

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            println("apply trns")
//            val newX = (startX + targetX * interpolatedTime).toInt()
//            val newY = (startY + targetY * interpolatedTime).toInt()
//            //to support decent animation, change new heigt as Nico S. recommended in comments
//            //int newHeight = (int) (startHeight+(targetHeight - startHeight) * interpolatedTime);
//            view.layoutParams?.x = newX
//            view.layoutParams?.y = newY
//            view.requestLayout()
//            windowManager.updateViewLayout(view, view!!.layoutParams)
        }

        override fun initialize(width: Int, height: Int, parentWidth: Int, parentHeight: Int) {
            println("###INIT")
            super.initialize(width, height, parentWidth, parentHeight)
        }

        override fun willChangeBounds(): Boolean {
            println("###WILLCHANGE")
            return true
        }
    }
}


//    override fun onDraw(canvas: Canvas?) {
//        super.onDraw(canvas)
//
//        canvas?.save()
//        canvas?.scale(scaleFactor, scaleFactor)
//        canvas?.restore()
//        println("drawing canvas " + scaleFactor)
//
//    }