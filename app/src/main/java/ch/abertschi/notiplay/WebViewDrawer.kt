package ch.abertschi.notiplay

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Looper.getMainLooper
import android.support.v4.content.ContextCompat.startActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.webkit.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.HashMap




/**
 * Created by abertschi on 26.01.18.
 */

class WebViewDrawer(val context: Context) : NotiRunnable {

    private val handler = Handler(Looper.getMainLooper())
    private var webView: NotiplayWebview? = null
    private val webAsset: String = "notiplay.html"
    private var observers: MutableList<NotiObserver> = ArrayList()
    private var videoId: String? = null
    private var onCloseCallback: (() -> Unit)? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    val httpClient = OkHttpClient()
    val jqueryUrl = "http://code.jquery.com/jquery-3.3.1.min.js"
    var googleYtV3VideoApi = "https://www.googleapis.com/youtube/v3/"

    var debug: Boolean = true


    fun setOnCloseCallback(c: (() -> Unit)) {
        this.onCloseCallback = c
    }

    private var storedLayoutParamsX: Int = 0
    private var storedLayoutParamsY: Int = 0
    private var storedLayoutParamsWidth: Int = 0
    private var storedLayoutParamsHeight: Int = 0


    private var isFullScreen: Boolean = false

    fun toggleFullScreen() {
        if (isFullScreen) launchFloatingWindow()
        else launchFullScreen()
    }

    fun launchFullScreen() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        isFullScreen = true
        webView?.allowScroll = true

        storedLayoutParamsX = layoutParams!!.x
        storedLayoutParamsY = layoutParams!!.y
        storedLayoutParamsWidth = layoutParams!!.width
        storedLayoutParamsHeight = layoutParams!!.height

        layoutParams?.x = 0
        layoutParams?.y = 0
        layoutParams?.height = width
        layoutParams?.width = height
        windowManager.updateViewLayout(webView, layoutParams)

        val dialogIntent = Intent(this@WebViewDrawer.context, HorizontalFullscreenActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this@WebViewDrawer.context, dialogIntent, null)

    }

    fun launchFloatingWindow() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        isFullScreen = false
        webView?.allowScroll = false

        val i = Intent(this@WebViewDrawer.context, HorizontalFullscreenActivity::class.java)
        i.addFlags (FLAG_ACTIVITY_SINGLE_TOP)
        i.action = HorizontalFullscreenActivity.ACTION_QUIT_ACTIVITY
        startActivity(this@WebViewDrawer.context, i, null)

        layoutParams!!.x = storedLayoutParamsX
        layoutParams!!.y = storedLayoutParamsY
        layoutParams!!.width = storedLayoutParamsWidth
        layoutParams!!.height = storedLayoutParamsHeight
        windowManager.updateViewLayout(webView, layoutParams)
    }


    var lastTouchX: Float = 0f
    var lastTouchY: Float = 0f
    var activePointerId: Int = 0
    var moveDy: Float = 0f
    var moveDx: Float = 0f
    var counter = 0


    private var scaleDetector = ScaleGestureDetector(context, ScaleListener())

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            webView?.let {
                it.scaleFactor *= detector.scaleFactor
                // Don't let the object get too small or too large.
                it.scaleFactor = Math.max(0.1f, Math.min(it.scaleFactor, 5.0f))
                println("scaling: " + it.scaleFactor)
                it.invalidate()
            }

            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return false
        }

        // event when double tap occurs
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val x = e.x
            val y = e.y
            if (!isFullScreen) launchFullScreen()


            Log.d("Double Tap", "Tapped at: ($x,$y)")

            return true
        }
    }

    private lateinit var gestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility")
    fun loadWebView() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams = WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT)
        } else {
            layoutParams = WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT)
        }


        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        println("h: " + height)
        println("w: " + width)


        layoutParams?.gravity = Gravity.LEFT or Gravity.TOP
        layoutParams?.x = 0
        layoutParams?.y = 0
        layoutParams?.width = if (debug) width else 0
        layoutParams?.height = if (debug) height / 3 else 0
//        params.width = height
//        params.height = width


        this.webView = NotiplayWebview(context)
        gestureDetector = GestureDetector(context, GestureListener())

        webView?.setOnLongClickListener(object : View.OnLongClickListener {

            override fun onLongClick(v: View): Boolean {
                if (!isFullScreen) launchFullScreen()
                return true

            }
        })

        webView?.setOnTouchListener(object : View.OnTouchListener {

            override fun onTouch(v: View, event: MotionEvent): Boolean {
//                scaleDetector = ScaleGestureDetectorCompat.
                scaleDetector.onTouchEvent(event)
                return false

                val frameX = layoutParams?.x
                val frameY = layoutParams?.y
                val frameWidth = layoutParams?.width
                val frameHeight = layoutParams?.height

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {

                        lastTouchX = event.x
                        lastTouchY = event.y

//                        moveDx = lastTouchY - frameY
//                        moveDy = lastTouchX - frameX


                        activePointerId = event.getPointerId(0)
                    }
                    MotionEvent.ACTION_MOVE -> {
//                        val x = event.x
//                        val y = event.y
//
//                        val dx = x - lastTouchX
//                        val dy = y - lastTouchY
//
//                        posX += dx
//                        posY += dy
//
//                        lastTouchY = y
//                        lastTouchX = x

//                        params.y = event.rawY.toInt() //
//                        params.x = event.rawX.toInt() //


                        layoutParams?.y = (event.rawY - lastTouchY).toInt()
                        layoutParams?.x = (event.rawX - lastTouchX).toInt()
//                            v.invalidate()
                        windowManager.updateViewLayout(webView, layoutParams)
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
//                        if (event.actionIndex == event.getPointerId(0))
                    }
                }


//                println("%d, %f, %f".format(event.action, event.rawX, event.rawY))
                return gestureDetector.onTouchEvent(event)
            }
        })


        // Get teh view params
//                val paramNameTV = callerNameTv.getLayoutParams() as WindowManager.LayoutParams
//update the view layout

        webView?.webViewClient = object : WebViewClient() {

            // Handle API until level 21
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                if (url.contains(jqueryUrl)) {
                    return super.shouldInterceptRequest(view, url)
                } else {
                    return getNewResponse(url)
                }
            }

            var found = false
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()
                println("###URL: " + url)

                val stopLoading = listOf<String>("https://www.youtube.com/signin?context=popup",
                        "https://www.youtube.com/share_ajax?action_get_share_info=",
                        "https://www.youtube.com/watch?time_continue=",
                        "photo.jpg", "https://googleads")
                stopLoading.forEach({
                    if (url.contains(it)) {
                        found = true
                        return@forEach
                    }
                })
                if (found) {
                    val handler = Handler(getMainLooper())
                    handler.post {
                        println("STOPING LOADING ==========")
                        webView?.stopLoading()
                        found = false
                    }
                    return null
                }

                if (url.contains(jqueryUrl)) {
                    return super.shouldInterceptRequest(view, request)
                } else {
                    return getNewResponse(url)
                }
            }

            private fun getNewResponse(url: String): WebResourceResponse? {
                if (url.contains("/endProcess")) {
                    windowManager.removeView(webView)
                    webView?.post { webView?.destroy() }
                    onCloseCallback?.invoke()
                    return WebResourceResponse("bgsType", "someEncoding", null)
                }

                val u = url.trim { it <= ' ' }

                var origin = ""
                if (url.startsWith(googleYtV3VideoApi)) {
                    origin = "null"
                } else {
                    origin = "https://www.youtube.com"
                }
                try {
                    val request = Request.Builder()
                            .url(u)
                            .addHeader("Origin", origin)
                            .addHeader("origin", origin)
                            .addHeader("Referer", origin)
                            .addHeader("Access-Control-Allow-Credentials", "false")
                            .addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                            .build()


                    val response = httpClient.newCall(request).execute()
                    val headers = HashMap<String, String>()
                    val allowHeaders = "accept, authorization, Content-Type, X-Walltime-Ms, " +
                            "X-Restrict-Formats-Hint, X-Bandwidth-Est, X-Bandwidth-Est3, content-length"
                    headers.put("Connection", "close")
                    headers.put("Access-Control-Allow-Methods", "POST,GET,OPTIONS,PUT,DELETE")
                    headers.put("Access-Control-Max-Age", "1200");
                    headers.put("Access-Control-Allow-Origin", origin)
                    headers.put("Access-Control-Allow-Credentials", "true")
                    headers.put("Access-Control-Expose-Headers", allowHeaders)
                    headers.put("Access-Control-Allow-Headers", allowHeaders)

                    return WebResourceResponse(null,
                            response.header("content-encoding", "utf-8")
                            , 200,
                            "Ok",
                            headers,
                            response.body()?.byteStream())

                } catch (e: Exception) {
                    System.err.println(u + e)
                    return null
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest,
                                         error: WebResourceError) {
                Log.d("Error", "loading web view: request: $request error: $error")
            }

            override fun onPageFinished(view: WebView, url: String) {
            }

        }
        webView?.addJavascriptInterface(WebInterface(context, observers), "NotiPlay")

        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.javaScriptEnabled = true
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.builtInZoomControls = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowFileAccess = true
        webSettings.pluginState = WebSettings.PluginState.ON
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false


//        webView?.rotation = 90f
        windowManager.addView(webView, layoutParams)

        var html = loadWebpage(webAsset)
        webView?.loadData(html, "text/html", null)
    }


    fun loadWebpage(webAsset: String): String {
        val stream = context.assets.open(webAsset)
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        val str = buffer.toString(Charset.defaultCharset())
        return str
    }


    override fun removeEventObserver(o: NotiObserver) {
        observers.remove(o)
    }

    override fun addEventObserver(o: NotiObserver) {
        observers.add(o)
    }

    override fun playVideoById(videoId: String, seekPosition: Int) {
        execJs("playWithVideoId(\"${videoId}\");")
    }

    override fun playerPause() = execJs("playerPause();")

    override fun playerPlay() = execJs("playerPlay();")

    override fun playerStop() = execJs("playerStop();")

    override fun seekForward(seek: Int) = execJs("seekForward(${seek});")

    override fun seekBackward(seek: Int) = execJs("seekBackward(${seek});")

    override fun seekToPosition(seconds: Int) = execJs("seekTo(${seconds});")

    override fun getPlaybackPosition() = execJs("getPlaybackPosition();")

    override fun playerPreviousVideo() = execJs("playerPreviousVideo();")

    override fun playerNextVideo() = execJs("playerNextVideo();")

    override fun getVideoData() = execJs("getVideoData();")

    fun execJs(command: String) {
        handler.post {
            println("javascript:${command}")
            webView!!.loadUrl("javascript:${command}")
        }

    }

    override fun setLoopMode(loopWhenEnd: Boolean) {
        if (loopWhenEnd) execJs("setLoopVideo(true);")
        else execJs("setLoopVideo(false);")
    }

    override fun toggleWebview() {
    }

    override fun setFullscreen(state: Boolean) {
        if (state) execJs("requestFullscreen();")
        else execJs("exitFullscreen();")
    }

}