package ch.abertschi.notiplay

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.os.Build
import android.support.v4.content.ContextCompat.startActivity
import android.util.DisplayMetrics
import android.view.*
import android.view.WindowManager.LayoutParams.*
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import org.jetbrains.anko.padding




/**
 * Created by abertschi on 04.02.18.
 */
class NotiplayWebview(context: Context) : WebView(context) {

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var allowScroll: Boolean = false
    var showFloatingWindow = true
    private var scaleFactor = 1f

    private var layoutParams: WindowManager.LayoutParams? = null
    private var storedLayoutParamsX: Int = 0
    private var storedLayoutParamsY: Int = 0
    private var storedLayoutParamsWidth: Int = 0
    private var storedLayoutParamsHeight: Int = 0
    private var actionScalingActive = false

    private var isFullScreen: Boolean = false

    private var scaleDetector = ScaleGestureDetector(context, ScaleListener())

    var lastTouchX: Float = 0f
    var lastTouchY: Float = 0f

    var dx: Float = 0f
    var dy: Float = 0f

    fun toggleFullScreen() {
        if (isFullScreen) launchFloatingWindow()
        else launchFullScreen()
    }

    fun loadLayout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams = WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or FLAG_NOT_TOUCH_MODAL or FLAG_NOT_TOUCHABLE,
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
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        layoutParams?.run {
            layoutParams?.gravity = Gravity.LEFT or Gravity.TOP
            layoutParams?.x = 0
            layoutParams?.y = 0
            layoutParams?.width = if (showFloatingWindow) width else 0
            layoutParams?.height = if (showFloatingWindow) height  else 0
//            storedLayoutParamsHeight =
//            storedLayoutParamsWidth = 500
            padding = 0
            horizontalMargin = 0f
            verticalMargin = 0f
        }
//        this.

        val parent = LinearLayout(context)

        val l = RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or FLAG_NOT_TOUCH_MODAL or FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT)
        l.width = width

        l.height = height / 3
//        parent.layoutParams
//        parent.backgroundColor
        parent.addView(this, l)




        setOnLongClickListener {
            if (!isFullScreen) launchFullScreen()
            true
        }


        setOnTouchListener { v, event ->
            scaleDetector.onTouchEvent(event)
//
//            val frameX = layoutParams?.x
//            val frameY = layoutParams?.y
//            val frameWidth = layoutParams?.width
//            val frameHeight = layoutParams?.height

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    dx = v.getX() - event.getRawX();
                    dx = v.getY() - event.getRawY();
                }
                MotionEvent.ACTION_MOVE -> {
                    if (actionScalingActive) {
                        return@setOnTouchListener true
                    } else {
                        layoutParams?.y = (event.rawY - lastTouchY).toInt()
                        layoutParams?.x = (event.rawX - lastTouchX).toInt()

                        var x = event.rawX - lastTouchX
                        var y = event.rawY - lastTouchY
//                        windowManager.updateViewLayout(this@NotiplayWebview, layoutParams)

//                        this@NotiplayWebview.animate()
//                                .x(x)
//                                .y(y)
//                                .setDuration(0)
//                                .start()
                    }
                }
                MotionEvent.ACTION_UP -> {
//                    layoutParams?.x = 0
//                    layoutParams?.y = 0
//                    windowManager.updateViewLayout(this@NotiplayWebview, layoutParams)
                    actionScalingActive = false
                }
            }
            return@setOnTouchListener false
        }
        windowManager.addView(parent, layoutParams)
    }

    fun launchFullScreen() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        isFullScreen = true
        this.allowScroll = true

        storedLayoutParamsX = layoutParams!!.x
        storedLayoutParamsY = layoutParams!!.y
        storedLayoutParamsWidth = layoutParams!!.width
        storedLayoutParamsHeight = layoutParams!!.height

        layoutParams?.x = 0
        layoutParams?.y = 0
        layoutParams?.height = width
        layoutParams?.width = height
        layoutParams?.verticalMargin = 0f
        layoutParams?.horizontalMargin = 0f

        windowManager.updateViewLayout(this, layoutParams)

        val dialogIntent = Intent(context, HorizontalFullscreenActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(context, dialogIntent, null)

    }

    fun launchFloatingWindow() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        isFullScreen = false
        allowScroll = false

        val i = Intent(context, HorizontalFullscreenActivity::class.java)
        i.addFlags(FLAG_ACTIVITY_SINGLE_TOP)
        i.action = HorizontalFullscreenActivity.ACTION_QUIT_ACTIVITY
        startActivity(context, i, null)

        layoutParams!!.x = storedLayoutParamsX
        layoutParams!!.y = storedLayoutParamsY
        layoutParams!!.width = (storedLayoutParamsWidth * scaleFactor).toInt()
        layoutParams!!.height = (storedLayoutParamsHeight * scaleFactor).toInt()
        windowManager.updateViewLayout(parent as View, layoutParams)
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
            if (!isFullScreen) launchFullScreen()
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
            layoutParams?.run {
//                width = (storedLayoutParamsWidth * scaleFactor).toInt()
//                height = (storedLayoutParamsHeight * scaleFactor).toInt()
//                windowManager.updateViewLayout(this@NotiplayWebview, layoutParams)
                println("${layoutParams?.width} / ${layoutParams?.height}")
//                scaleX = 1f
//                scaleY = 1f
//                width = width * scaleFactor
            }
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
//            return false
            actionScalingActive = true
            scaleFactor *= detector.scaleFactor
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 1.0f))
//            layoutParams?.s
            this@NotiplayWebview.scaleX = scaleFactor
            this@NotiplayWebview.scaleY = scaleFactor
            this@NotiplayWebview.pivotX = 0f
            this@NotiplayWebview.pivotY = 0f

            println("${layoutParams?.width} / ${layoutParams?.height}")
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