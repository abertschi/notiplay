package ch.abertschi.notiplay.playback.yt

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Looper.getMainLooper
import android.support.v4.content.ContextCompat.startActivity
import android.view.WindowManager
import android.webkit.*
import ch.abertschi.notiplay.NotiObserver
import ch.abertschi.notiplay.NotiRunnable
import ch.abertschi.notiplay.view.FloatingWindow
import ch.abertschi.notiplay.view.HorizontalFullscreenActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.HashMap


/**
 * Created by abertschi on 26.01.18.
 */

class YoutubePlayer(val context: Context) : NotiRunnable, AnkoLogger {

    override fun resetPlayer() {
        execJs("window.location.reload( true );")
    }

    override fun toggleVisible() {
        floatingWindow?.toggleVisible()
    }

    private val handler = Handler(Looper.getMainLooper())
    private var observers: MutableList<NotiObserver> = ArrayList()
    private var onCloseCallback: (() -> Unit)? = null

    private val webAsset: String = "notiplay.html"
    private val httpClient = OkHttpClient()
    private val jqueryUrl = "http://code.jquery.com/jquery-3.3.1.min.js"
    private var googleYtV3VideoApi = "https://www.googleapis.com/youtube/v3/"

    private var blockingRequests = false

    private val blockedUrls = listOf<String>(
            "https://www.youtube.com/signin?context=popup",
            "https://www.youtube.com/share_ajax?action_get_share_info=",
            "https://www.youtube.com/watch?time_continue=",
            "photo.jpg", "https://googleads")

    private val httpHeaders = "accept, authorization, Content-Type, X-Walltime-Ms, " +
            "X-Restrict-Formats-Hint, X-Bandwidth-Est, X-Bandwidth-Est3, content-length"


    var windowManager: WindowManager? = null
    var floatingWindow: FloatingWindow? = null
    var youtubeWebView: YoutubeWebView? = null
    var debug: Boolean = true


    @SuppressLint("ClickableViewAccessibility")
    fun startWebView() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        this.floatingWindow = FloatingWindow(context)



        floatingWindow?.onFullScreenAction = this::requestFullScreen
        floatingWindow?.onFloatingWindowAction = this::requestFloatingWindow

        youtubeWebView = YoutubeWebView(this.context)
        youtubeWebView?.let {
            it.webViewClient = webViewClient
            it.addJavascriptInterface(WebInterface(context, observers), "NotiPlay")
            configureWebView(it.settings)
            it.setInitialScale(1)
            it.loadData(assetToString(webAsset), "text/html", null)
        }
        floatingWindow?.loadLayout(youtubeWebView!!)

    }

    fun stopWebView() {
        windowManager?.removeView(floatingWindow)
        floatingWindow?.post { youtubeWebView?.destroy() }
        onCloseCallback?.invoke()
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(webSettings: WebSettings) {
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.javaScriptEnabled = true
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.builtInZoomControls = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowFileAccess = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
    }

    private fun assetToString(webAsset: String): String {
        val stream = context.assets.open(webAsset)
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        return buffer.toString(Charset.defaultCharset())
    }

    fun setOnCloseCallback(c: (() -> Unit)) {
        this.onCloseCallback = c
    }

    fun execJs(command: String) {
        handler.post {
            println("javascript:${command}")
            youtubeWebView!!.loadUrl("javascript:${command}")
        }
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

    override fun setLoopMode(loopWhenEnd: Boolean) {
        if (loopWhenEnd) execJs("setLoopVideo(true);")
        else execJs("setLoopVideo(false);")
    }

    private var isFullScreen = false

    fun requestFullScreen() {
        isFullScreen = true
        floatingWindow?.requestFullScreen()
        val dialogIntent = Intent(context, HorizontalFullscreenActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        dialogIntent.action = "request_fullscreen"
//        dialogIntent.extras.putString("callback", this::class.java.toString())
//        dialogIntent.extras.putString("call)
        startActivity(context, dialogIntent, null)
    }

    override fun confirmFullscreen() {
        floatingWindow?.confirmFullScreen()
    }

    override fun toggleWebview() {
    }


    fun requestFloatingWindow() {
//        floatingWindow?.requestFullScreen
        isFullScreen = false
        println("state is: " + isFullScreen)
        println("confirm floating window")
        floatingWindow?.requestFloatingWindow()
    }

    fun confirmFloatingWindow() {
        floatingWindow?.confirmFloatingWindow()
    }


    // can not be called by HorizontalView
    // horizontalview needs to use request/confirm methods
    override fun setFullscreen(state: Boolean) {
//        throw UnsupportedOperationException()
        if (state && !isFullScreen) {
            requestFullScreen()
        } else if (isFullScreen) {
            requestFloatingWindow()
        }
    }

    override fun removeEventObserver(o: NotiObserver) {
        observers.remove(o)
    }

    override fun addEventObserver(o: NotiObserver) {
        observers.add(o)
    }

    private var webViewClient: WebViewClient = object : WebViewClient() {

        // Handle API until level 21
        override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
            if (url.contains(jqueryUrl)) {
                return super.shouldInterceptRequest(view, url)
            } else {
                return getNewResponse(url)
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?):
                WebResourceResponse? {
            val url = request?.url.toString()

            info { "loading " + url }
            blockedUrls.forEach({
                if (url.contains(it)) {
                    blockingRequests = true
                    return@forEach
                }
            })
            if (blockingRequests) {
                val handler = Handler(getMainLooper())
                handler.post {
                    info { "stop loading " + url }
                    youtubeWebView?.stopLoading()
                    blockingRequests = false
                }
                return null
            }

            return if (jqueryUrl in url) {
                super.shouldInterceptRequest(view, request)
            } else {
                getNewResponse(url)
            }
        }

        private fun getNewResponse(url: String): WebResourceResponse? {
            if (url.contains("/endProcess")) {
                stopWebView()
                return WebResourceResponse("bgsType", "someEncoding", null)
            }

            info { "#intercepting: " + url }

            val address = url.trim { it <= ' ' }
            var origin = if (url.startsWith(googleYtV3VideoApi)) {
                "null"
            } else {
                "https://www.youtube.com"
            }
            try {
                val request = Request.Builder()
                        .url(address)
                        .addHeader("Origin", origin)
                        .addHeader("origin", origin)
                        .addHeader("Referer", origin)
                        .addHeader("Access-Control-Allow-Credentials",
                                "false")
                        .addHeader("user-agent",
                                "Mozilla/5.0 (iPhone; CPU iPhone OS 6_0 like Mac OS X) " +
                                        "AppleWebKit/536.26 (KHTML, like Gecko) " +
                                        "Version/6.0 Mobile/10A5376e Safari/8536.25")
                        .build()

                val response = httpClient.newCall(request).execute()
                val headers = HashMap<String, String>()

                headers.run {
                    put("Connection", "close")
                    put("Access-Control-Allow-Methods", "POST,GET,OPTIONS,PUT,DELETE")
                    put("Access-Control-Max-Age", "1200")
                    put("Access-Control-Allow-Origin", origin)
                    put("Access-Control-Allow-Credentials", "true")
                    put("Access-Control-Expose-Headers", httpHeaders)
                    put("Access-Control-Allow-Headers", httpHeaders)
                }

                return WebResourceResponse(null,
                        response.header("content-encoding", "utf-8")
                        , 200,
                        "Ok",
                        headers,
                        response.body()?.byteStream())

            } catch (e: Exception) {
                warn("error with " + address)
                warn(e)
                return null
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest,
                                     error: WebResourceError) {
            super.onReceivedError(view, request, error)
            warn("loading web view: request: $request error: $error")
        }
    }
}