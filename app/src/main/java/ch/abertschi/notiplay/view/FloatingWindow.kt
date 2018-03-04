package ch.abertschi.notiplay.view

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.PixelFormat
import android.os.Build
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.DisplayMetrics
import android.view.*
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.jetbrains.anko.padding
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


/**
 * Created by abertschi on 04.02.18.
 * // todo: replace this by generic view which contains webview (or anything else)
 */
class FloatingWindow(context: Context, val controller: FloatingWindowController) : InterceptTouchFrameLayout(context) {

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var showFloatingWindow: Boolean = true

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

    private var actionUpX: Int = 0
    private var actionUpY: Int = 0

    private var lastTapMs = System.currentTimeMillis()
    private val doubleTapThresholdMs = 400

    private val widthScale = (864.0 / 1652.0)

    var onDoubleTab: (() -> Unit)? = null

    private lateinit var currentChildView: View

    var isAlphaActive = false


    fun toggleVisible() {
        makeVisible(!showFloatingWindow)
    }


    fun makeVisible(state: Boolean) {
        showFloatingWindow = state

        layoutParams?.run {
            if (state) {
                applyLayout(stateActive)
            } else {
                applyLayout(stateInvisible)
            }
        }
        windowManager.updateViewLayout(this, layoutParams)
    }

    init {
        super.setOnInterceptTouchEventListener(TouchListener())
    }


    fun setChildWindow(view: View) {
        currentChildView = view
        this.removeAllViews()
        this.addView(view)
        windowManager.updateViewLayout(this, layoutParams)
    }

    fun stopView() {
        windowManager?.removeView(this)
    }

    fun applyLayout(stateComposition: StateComposition) {
        layoutParams?.run {
            layoutParams?.gravity = Gravity.LEFT or Gravity.TOP
            layoutParams?.x = stateComposition.x
            layoutParams?.y = stateComposition.y
            layoutParams?.width = stateComposition.width
            layoutParams?.height = stateComposition.height
            padding = 0
            horizontalMargin = 0f
            verticalMargin = 0f
        }
        this.allowScroll = stateComposition.canScroll
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
                0, 0,
                true,
                true,
                false,
                false)

        stateHalfScreen = StateComposition(State.HALF_SCREEN, _viewPortHeight / 3, _viewPortWidth,
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
        windowManager.addView(this, layoutParams)
        alignFloatingWindow()
    }


    private fun alignFloatingWindow() {
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
                windowManager.updateViewLayout(this@FloatingWindow, this)
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


    fun storeLayoutParams(stateComposition: StateComposition) {
        if (stateComposition.state == State.HALF_SCREEN) return
        stateComposition.x = layoutParams!!.x
        stateComposition.y = layoutParams!!.y
    }

    fun setSizeToHalfScreen() {
        applyLayout(stateHalfScreen)
        stateActive = stateHalfScreen
        windowManager.updateViewLayout(this, layoutParams)
    }

    fun setSizeToFullScreen() {
        applyLayout(stateFullscreen)
        stateActive = stateFloatScreen
        windowManager.updateViewLayout(this, layoutParams)
    }


    fun setSizeToFloatingWindow(alignFloatingWindows: Boolean = true) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        applyLayout(stateFloatScreen)
        stateActive = stateFloatScreen
        windowManager.updateViewLayout(this, layoutParams)

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

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return (result / 3.0 * 2).toInt()
    }

    private inner class TouchListener : OnInterceptTouchEventListener {
        override fun onInterceptTouchEvent(view: InterceptTouchFrameLayout?, ev: MotionEvent?,
                                           disallowIntercept: Boolean): Boolean {
            return onTouchEvent(view, ev)
        }

        override fun onTouchEvent(view: InterceptTouchFrameLayout?, event: MotionEvent?): Boolean {
            when (event!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    val t = System.currentTimeMillis()
                    val delta = t - lastTapMs
                    // dont register double tab when touched lower 20% of screen
                    if (delta < doubleTapThresholdMs &&
                            (stateActive.state != State.SMALL_FLOATING ||
                                    (1.0 * event.y / stateActive.height) <.8)) {
                        onDoubleTab?.invoke()
                        return true
                    }
                    lastTapMs = t
                    lastTouchX = event.x
                    lastTouchY = event.y
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
                        setSizeToFloatingWindow(alignFloatingWindows = false)
                        return true
                    }

                    if (!stateActive.canMove) return false
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
//                            this@FloatingWindow.alpha = (h - event.y) / h
//                        } else {
//                            this@FloatingWindow.alpha = 1f
//                            isAlphaActive = false
//                        }
//                    }
//                    if (actionScalingActive) {
//                        return true
//                    } else {
                        stateActive.y = (event.rawY - lastTouchY).toInt()
                        stateActive?.x = (event.rawX - lastTouchX).toInt()
                        applyLayout(stateActive)
//                        windowManager.updateViewLayout(this, layoutParams)
                        windowManager.updateViewLayout(this@FloatingWindow, layoutParams)
//                    }
                }
                MotionEvent.ACTION_UP -> {
//                    if (isAlphaActive) {
//                        makeVisible(false)
//                    } else {
                        if (stateActive.canAlignToGrid)
                            alignFloatingWindow()
//                    }
//                    actionScalingActive = false
                }
            }
            return !stateActive.canDelegateTouch
        }

    }
}