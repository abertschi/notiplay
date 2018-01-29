package ch.abertschi.notiplay

import android.annotation.TargetApi
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.FileObserver.DELETE
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by abertschi on 26.01.18.
 */

class WebViewDrawer(val context: Context) : NotiRunnable {

    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private val webAsset: String = "notiplay.html"
    private var observers: MutableList<NotiObserver> = ArrayList()
    private var videoId: String? = null
    private var onCloseCallback: (() -> Unit)? = null

    val httpClient = OkHttpClient()
    val jqueryUrl = "http://code.jquery.com/jquery-3.3.1.min.js"
    var googleYtV3VideoApi = "https://www.googleapis.com/youtube/v3/"

    var debug: Boolean = true


    fun setOnCloseCallback(c: (() -> Unit)) {
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
        params.width = if (debug) 1280 else 0
        params.height = if (debug) 960 else 0

        this.webView = WebView(context)

        webView?.webViewClient = object : WebViewClient() {

            // Handle API until level 21
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                if (url.contains(jqueryUrl)) {
                    return super.shouldInterceptRequest(view, url)
                } else {
                    return getNewResponse(url)
                }
            }


            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()
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
        windowManager.addView(webView, params)

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