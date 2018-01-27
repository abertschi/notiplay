package ch.abertschi.notiplay

import android.annotation.TargetApi
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.Charset
import java.util.*

/**
 * Created by abertschi on 26.01.18.
 */

class WebViewDrawer(val context: Context) : NotiRunnable {

    override fun toggleWebview() {
    }

    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private val webAsset: String = "notiplay.html"
    private var observers: MutableList<NotiObserver> = ArrayList()
    private var videoId: String? = null
    private var onCloseCallback: (() -> Unit)? = null

    var debug: Boolean = false


    fun setOnCloseCallback(c: (() -> Unit)){
        this.onCloseCallback = c
    }


    fun loadWebView() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 400
        params.width = if (debug) 640 else 0
        params.height = if (debug) 480 else 0

        this.webView = WebView(context)

        webView?.webViewClient = object : WebViewClient() {

            // Handle API until level 21
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                return getNewResponse(url)
            }


            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()

                if (url.contains("/endProcess")) {
                    windowManager.removeView(webView)
                    webView?.post { webView?.destroy() }
                    onCloseCallback?.invoke()
                    return WebResourceResponse("bgsType", "someEncoding", null)
                }

                return getNewResponse(url)
            }

            private fun getNewResponse(url: String): WebResourceResponse? {

                try {
                    val httpClient = OkHttpClient()

                    val request = Request.Builder()
                            .url(url.trim { it <= ' ' })
                            .addHeader("Referer", "https://www.youtube.com/watch?v=${videoId}")
                            .addHeader("User Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                            .build()
                    println("newresponse: " + url)
                    val response = httpClient.newCall(request).execute()

                    return WebResourceResponse(
                            null,
                            response.header("content-encoding", "utf-8"),
                            response.body()?.byteStream()
                    )

                } catch (e: Exception) {
                    return null
                }

            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest,
                                         error: WebResourceError) {
                Log.d("Error", "loading web view: request: $request error: $error")
            }

            override fun onPageFinished(view: WebView, url: String) {
                //view.loadUrl("javascript:(function() { document.getElementById('ytplayer').click(); })()");
            }

        }
        webView?.addJavascriptInterface(WebInterface(context, observers), "NotiPlay")

        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.javaScriptEnabled = true
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.builtInZoomControls = true
        webSettings.pluginState = WebSettings.PluginState.ON
        windowManager.addView(webView, params)

        var html = loadWebpage()
        webView?.loadData(html, "text/html", null)

//        timer.schedule(timerTask { seekForward() }, 10000)
//        timer.schedule(timerTask { seekForward(100) }, 11000)
//        timer.schedule(timerTask { seekForward(10) }, 12000)
//        timer.schedule(timerTask { seekBackward(40) }, 14000)
//        timer.schedule(timerTask { seekBackward(5) }, 16000)
    }


    private fun loadWebpage(): String {
        val stream = context.assets.open(webAsset)
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        return buffer.toString(Charset.defaultCharset())
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

    override fun playerPreviousVideo() = execJs("playerNextVideo();")


    override fun playerNextVideo() = execJs("playerPreviousVideo();")
    override fun getVideoData() = execJs("getVideoData();")


    fun execJs(command: String) {
        handler.post {
            println("javascript:${command}")
            webView!!.loadUrl("javascript:${command}")
        }

    }
}