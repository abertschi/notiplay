package ch.abertschi.notiplay.playback.yt

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import ch.abertschi.notiplay.NotiObserver
import ch.abertschi.notiplay.NotiRunnable
import ch.abertschi.notiplay.view.FloatingWindow
import org.jetbrains.anko.AnkoLogger
import java.nio.charset.Charset
import java.util.*


/**
 * Created by abertschi on 26.01.18.
 */

class YoutubePlayer(val context: Context) : NotiRunnable, AnkoLogger {


    override fun getView(): View {
        return youtubeWebView!!
    }

    override fun getViewController(): NotiRunnable.ViewController {
        return null!!
    }

    override fun resetPlayer() {
        execJs("window.location.reload( true );")
    }

//    override fun toggleVisible() {
//        floatingWindow?.toggleVisible()
//    }

    private val handler = Handler(Looper.getMainLooper())
    private var observers: MutableList<NotiObserver> = ArrayList()
    private var onCloseCallback: (() -> Unit)? = null

    private val webAsset: String = "notiplay.html"

    private var windowManager: WindowManager? = null
    private var floatingWindow: FloatingWindow? = null
    private var youtubeWebView: YoutubeWebView? = null
    private var webViewClient: RequestHandler? = null


    init {


    }

    @SuppressLint("ClickableViewAccessibility")
    fun startWebView() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        youtubeWebView = YoutubeWebView(this.context)
        webViewClient = RequestHandler(youtubeWebView!!)

//        this.floatingWindow = FloatingWindow(context)

//        floatingWindow?.onDoubleTab = this::requestFullScreen
//        floatingWindow?.onFloatingWindowAction = this::requestFloatingWindow


        youtubeWebView?.let {
            it.webViewClient = webViewClient
            it.addJavascriptInterface(WebInterface(context, observers), "NotiPlay")
            configureWebView(it.settings)
            it.setInitialScale(1)
            it.loadData(assetToString(webAsset), "text/html", null)
        }

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

    override fun playerStop() {
        execJs("playerStop();")
//        stopWebView()
        youtubeWebView?.destroy()
//        webViewClient?.sto
    }

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


    override fun removeEventObserver(o: NotiObserver) {
        observers.remove(o)
    }

    override fun addEventObserver(o: NotiObserver) {
        observers.add(o)
    }


}