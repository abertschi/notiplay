package ch.abertschi.notiplay.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.PixelFormat
import android.os.Build
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.jetbrains.anko.runOnUiThread
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


/**
 * Created by abertschi on 04.02.18.
 * // todo: replace this by generic view which contains webview (or anything else)
 */
class FloatingWindow(context: Context, val controller: FloatingWindowController) : InterceptTouchFrameLayout(context) {

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//    var showFloatingWindow: Boolean = true

    private var allowScroll: Boolean = false
    private var scaleFactor = 1f

    private var _viewPortHeight: Int = 0
    private var _viewPortWidth: Int = 0
    private var viewPortInitOrientation: Int = 0

    private var layoutParams: WindowManager.LayoutParams? = null

//    private var actionScalingActive = false

    enum class State {
        SMALL_FLOATING,
        HALF_SCREEN,
        INVISIBLE,
        FULLSCREEN

    }

    lateinit var stateFloatScreen: StateComposition
    lateinit var stateHalfScreen: StateComposition
    lateinit var stateInvisible: StateComposition
    lateinit var stateActive: StateComposition
    lateinit var stateFullscreen: StateComposition

    data class StateComposition(val state: State, var height: Int, var width: Int,
                                var x: Int, var y: Int, var canMove: Boolean, var canAlignToGrid: Boolean,
                                var canDelegateTouch: Boolean, var canScroll: Boolean)


    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    var isVisible = true
    private var actionUpX: Int = 0
    private var actionUpY: Int = 0

    private var lastTapMs = System.currentTimeMillis()
    private val doubleTapThresholdMs = 400

    private val widthScale = (864.0 / 1652.0)

    var onDoubleTab: (() -> Unit)? = null

    private lateinit var currentChildView: View

    var isAlphaActive = false


    fun toggleVisible() {
        makeVisible(!isVisible)
    }


    fun makeVisible(state: Boolean) {
        isVisible = state
        applyLayout(stateActive)
        context.runOnUiThread {
            windowManager.updateViewLayout(this@FloatingWindow, layoutParams)
        }

        if (!isVisible) {
            // restore default floating window padding
            stateFloatScreen.x = floatingWindowXMargin()
            stateFloatScreen.y = floatingWindowYMargin()
        }

    }

    init {
        super.setOnInterceptTouchEventListener(TouchListener())
    }


    fun setChildWindow(view: View) {
        currentChildView = view
        this.removeAllViews()
        this.addView(view)
        context.runOnUiThread {
            windowManager.updateViewLayout(this@FloatingWindow, layoutParams)
        }

    }

    fun stopView() {
        context.runOnUiThread {
            windowManager?.removeView(this@FloatingWindow)
        }
    }

    fun applyLayout(stateComposition: StateComposition) {
        println("X: state: ${stateComposition.state} ${stateComposition.x} / ${stateComposition.y}")
        layoutParams?.gravity = Gravity.LEFT or Gravity.TOP
        layoutParams?.x = stateComposition.x
        layoutParams?.y = stateComposition.y
        layoutParams?.width = if (isVisible) stateComposition.width else 0
        layoutParams?.height = if (isVisible) stateComposition.height else 0
        layoutParams?.horizontalMargin = 0f
        layoutParams?.verticalMargin = 0f

        this.allowScroll = stateComposition.canScroll
        visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    fun loadLayout(childView: View) {
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
        var fullscreenWidth = 0
        var fullscreenHeight = 0
        if (viewPortInitOrientation == ORIENTATION_LANDSCAPE) {
            fullscreenWidth = _viewPortWidth
            fullscreenHeight = _viewPortHeight
        } else {
            fullscreenHeight = _viewPortWidth
            fullscreenWidth = _viewPortHeight
        }

        var floatingWidth = 0
        if (_viewPortHeight < _viewPortWidth) {
            // wide
            floatingWidth = (_viewPortHeight / 2.0).roundToInt()

        } else {
            // portrait mode
            floatingWidth = (_viewPortWidth / 2.0).roundToInt()
        }

        stateFloatScreen = StateComposition(State.SMALL_FLOATING,
                (floatingWidth * widthScale).toInt(), floatingWidth,
                floatingWindowXMargin(), floatingWindowYMargin(),
                true,
                true,
                false,
                false)

        stateHalfScreen = StateComposition(State.HALF_SCREEN, _viewPortHeight / 5 * 2, _viewPortWidth,
                0, getStatusBarHeight(),
                false,
                false,
                true,
                true)

        stateInvisible = StateComposition(State.INVISIBLE,
                0, 0,
                0, 0,
                false,
                false,
                false,
                false)

        stateFullscreen = StateComposition(State.FULLSCREEN,
                fullscreenHeight, fullscreenWidth,
                0, 0,
                false,
                false,
                true,
                true)

        applyLayout(stateHalfScreen)
        stateActive = stateHalfScreen


        this.layoutTransition = LayoutTransition()

        childView.layoutParams = FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)

        this.addView(childView)
        currentChildView = childView
        context.runOnUiThread {
            windowManager.addView(this@FloatingWindow, layoutParams)
        }
        // alignFloatingWindow(moveToTopLeft = true)
    }


    private fun floatingWindowXMargin() = getStatusBarHeight() / 2
    private fun floatingWindowYMargin() = getActionBarHeight() + getStatusBarHeight() / 2 // padding + 2 * getStatusBarHeight()

    fun alignFloatingWindow(callBackOnDone: (() -> Unit)? = null,
                                    moveToTopLeft: Boolean = false) {
        layoutParams?.run {
            actionUpX = if (x < 0) 0 else x
            actionUpY = if (y < 0) 0 else y
        }

        val anim = ValueAnimator.ofFloat(0.0f, 1.0f)
        anim.startDelay = 100
        anim.duration = 500
        anim.interpolator = FastOutSlowInInterpolator()

        anim.addUpdateListener {
            val viewHeight = layoutParams!!.height
            val viewWidth = layoutParams!!.width

            val p = getViewCorrectedViewPortSize()
            val screenHeight = p.second
            val screenWidth = p.first
            val h = (screenHeight / 3.0).roundToInt()
            val w = (screenWidth / 2.0).roundToInt()

            var targetY = 0
            var targetX = 0
            val padding = getStatusBarHeight() / 2
            if (moveToTopLeft) {
                targetY = floatingWindowYMargin()
                targetX = floatingWindowXMargin()

            } else {
                targetY = when (actionUpY + viewHeight / 2) {
                    in 0..h -> floatingWindowYMargin()
                    in (h + 1)..(2 * h) -> padding + (screenHeight / 2) - (viewHeight / 2)
                    else -> screenHeight - padding - viewHeight
                }
                targetX = when (actionUpX + viewWidth / 2) {
                    in 0..w -> padding
                    else -> screenWidth - padding - viewWidth
                }
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
                context.runOnUiThread {
                    windowManager.updateViewLayout(this@FloatingWindow, layoutParams)
                }
            }
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                println("animation done")
                callBackOnDone?.invoke()
            }
        })
        anim.start()
        println("animation started")

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


    fun storeLayoutParams(stateComposition: StateComposition) {
        if (stateComposition.state == State.HALF_SCREEN) return
        stateComposition.x = layoutParams!!.x
        stateComposition.y = layoutParams!!.y
    }

    var running = false
    fun setSizeToHalfScreen() {
        if (running) {
            return
        }
        running = true
        alignFloatingWindow({
            applyLayout(stateHalfScreen)
            stateActive = stateHalfScreen
            context.runOnUiThread {
                windowManager.updateViewLayout(this@FloatingWindow, layoutParams)
            }
            running = false
        }, true)
    }

    fun setSizeToFullScreen() {
        applyLayout(stateFullscreen)
        stateActive = stateFullscreen
        context.runOnUiThread {
            windowManager.updateViewLayout(this@FloatingWindow, layoutParams)
        }
    }


    fun setSizeToFloatingWindow(alignFloatingWindows: Boolean = true) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        applyLayout(stateFloatScreen)
        stateActive = stateFloatScreen
        context.runOnUiThread {
            windowManager.updateViewLayout(this@FloatingWindow, layoutParams)
        }

        if (alignFloatingWindows) {
            alignFloatingWindow()
        }
    }

    public override fun overScrollBy(deltaX: Int, deltaY: Int, scrollX: Int, scrollY: Int,
                                     scrollRangeX: Int, scrollRangeY: Int, maxOverScrollX: Int,
                                     maxOverScrollY: Int, isTouchEvent: Boolean): Boolean {
        if (allowScroll) return super.overScrollBy(deltaX, deltaY, scrollX, scrollY,
                scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent)
        return true
    }

    override fun scrollTo(x: Int, y: Int) {
        if (allowScroll) return super.scrollTo(x, y)
    }

    override fun computeScroll() {
        if (allowScroll) return super.computeScroll()
    }


    private fun getActionBarHeight(): Int {
        val tv = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics) + getStatusBarHeight()
        } else {
            return getStatusBarHeight() * 5
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return (result).toInt()
    }

    private inner class GestureDetect : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            println("double tabbed")
            return super.onDoubleTap(e)
        }
    }

    private val doubleTab: GestureDetect = GestureDetect()

    private var hideWindowOnActionUp = false

    private inner class TouchListener : OnInterceptTouchEventListener {
        override fun onInterceptTouchEvent(view: InterceptTouchFrameLayout?, event: MotionEvent?,
                                           disallowIntercept: Boolean): Boolean {
            return onTouchEvent(view, event)
        }


        override fun onTouchEvent(view: InterceptTouchFrameLayout?, event: MotionEvent?): Boolean {

            // TODO: improve this
            if (running) return false
            when (event!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    val t = System.currentTimeMillis()
                    val delta = t - lastTapMs
                    println("delta: $delta / t: $t / lastTabMs: $lastTapMs / state: ${stateActive.state}")

                    // dont register double tab when touched lower 20% of screen
                    // hack TODO

                    lastTouchX = event.x
                    lastTouchY = event.y


                    if (delta > 10 && delta < doubleTapThresholdMs &&
                            (stateActive.state == State.SMALL_FLOATING ||
                                    (1.0 * event.y / stateActive.height) < .8)) {
                        lastTapMs = t
                        onDoubleTab?.invoke()
                        return true
                    }
                    lastTapMs = t


                }
                MotionEvent.ACTION_MOVE -> {
                    val w = layoutParams!!.width
                    val h = layoutParams!!.height
                    val screenSize = getViewCorrectedViewPortSize()
                    val padding = 0

                    val convertToFloatingWindowThreshold = 40

                    if (stateActive.state == State.HALF_SCREEN && event.y - lastTouchY > 100) {
                        stateFloatScreen.y = (event.rawY - stateFloatScreen.height / 2).toInt()
                        stateFloatScreen.x = (event.rawX - stateFloatScreen.width / 2).toInt()
//                        println("GOT HEREEEE")
//                        println("###0")
                        setSizeToFloatingWindow(alignFloatingWindows = false)
                        return true
                    }

                    if (!stateActive.canMove) return false

                    if (event.rawY - event.y <= padding) {
                        hideWindowOnActionUp = true
//                        this@FloatingWindow.alpha = event.y / h
                    } else {
                        hideWindowOnActionUp = false
                    }


//                    isAlphaActive = false
//                    if (!controller.isFullscreen() && controller.allowMoveOut) {
//                        isAlphaActive = true
//                        if (event.rawX - event.x <= padding) {
//                            val a = (event.x) / w
//                            this@FloatingWindow.alpha = a
//                        } else if ((screenSize.first - (event.rawX + w - event.x).absoluteValue
//                                        <= padding)) {
//                            val a = (w - event.x) / w
//                            println("got " + a)
//                            this@FloatingWindow.alpha = a
//
//                        } else if (event.rawY - event.y <= padding) {
//                            this@FloatingWindow.alpha = event.y / h
//
//                        } else if ((screenSize.second - (event.rawY + h - event.y).absoluteValue
//                                        <= padding)) {
//                            this@FlouuhatingWindow.alpha = (h - event.y) / h
//                        } else {
//                            this@FloatingWindow.alpha = 1f
//                            isAlphaActive = false
//                        }
//                    }
//                    if (actionScalingActive) {
//                        return true
//                    } else {


                    if (stateActive!!.state != State.HALF_SCREEN) {
                        // TODO: this is uggly
//                        println("###1")
                        stateActive.y = (event.rawY - lastTouchY).toInt()
                        stateActive?.x = (event.rawX - lastTouchX).toInt()
                    }
//                    println("apply layout to ${stateActive.state} with ${stateActive.y}")
                    applyLayout(stateActive)
//                        windowManager.updateViewLayout(this, layoutParams)
                    context.runOnUiThread {
                        windowManager.updateViewLayout(this@FloatingWindow, layoutParams)
                    }
//                    }
                }
                MotionEvent.ACTION_UP -> {
//                    if (isAlphaActive) {
//                        makeVisible(false)
//                    } else {

                    if (hideWindowOnActionUp) {
                        controller.setVisible(false)
                        controller.showHidingFloatingWindowMessage()
                    }

                    if (stateActive.canAlignToGrid)
                        alignFloatingWindow()
//                    }
//                    actionScalingActive = false
                }
            }
            // hack!!
//            if (stateActive.state == State.HALF_SCREEN && stateActive.y != layoutParams!!.y) {
//                setSizeToHalfScreen()
//                windowManager.updateViewLayout(this@FloatingWindow, layoutParams)
//                this@FloatingWindow.onTouchEvent(event)
//                this@FloatingWindow.performClick()
//                this@FloatingWindow.dispatchTouchEvent(event)
//            }
//            println("state: ${stateActive.state} / x/y : ${stateActive.y} / ${layoutParams!!.y}")

            return !stateActive.canDelegateTouch
        }

    }


}