package ch.abertschi.notiplay.playback.yt

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat.startActivity
import android.view.WindowManager
import android.webkit.WebSettings
import ch.abertschi.notiplay.NotiObserver
import ch.abertschi.notiplay.NotiRunnable
import ch.abertschi.notiplay.view.FloatingWindow
import ch.abertschi.notiplay.view.HorizontalFullscreenActivity
import org.jetbrains.anko.AnkoLogger
import java.nio.charset.Charset
import java.util.*


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

    private var windowManager: WindowManager? = null
    private var floatingWindow: FloatingWindow? = null
    private var youtubeWebView: YoutubeWebView? = null
    private var webViewClient: RequestHandler? = null


    @SuppressLint("ClickableViewAccessibility")
    fun startWebView() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        this.floatingWindow = FloatingWindow(context)

        floatingWindow?.onFullScreenAction = this::requestFullScreen
        floatingWindow?.onFloatingWindowAction = this::requestFloatingWindow

        youtubeWebView = YoutubeWebView(this.context)
        webViewClient = RequestHandler(youtubeWebView!!)

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


}