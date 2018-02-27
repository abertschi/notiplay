package ch.abertschi.notiplay.playback.yt

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import ch.abertschi.notiplay.Player
import ch.abertschi.notiplay.view.FloatingWindow
import org.jetbrains.anko.AnkoLogger
import java.nio.charset.Charset
import java.util.*


/**
 * Created by abertschi on 26.01.18.
 */

class YoutubePlayer(val context: Context, val playbackCallback: Callback) : Player, WebObserver, AnkoLogger {
    override fun getState(): Int {
        return state
    }


    override fun getView(): View {
        return youtubeWebView!!
    }

    override fun getViewController(): Player.ViewController {
        return null!!
    }

    override fun resetPlayer() {
        execJs("window.location.reload( true );")
    }

//    override fun toggleVisible() {
//        floatingWindow?.toggleVisible()
//    }

    private val handler = Handler(Looper.getMainLooper())
    private var observers: MutableList<WebObserver> = ArrayList()
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


    override fun removeEventObserver(o: WebObserver) {
        observers.remove(o)
    }

    override fun addEventObserver(o: WebObserver) {
        observers.add(o)
    }


    override fun onPlayerReady() {
//        playbackListener?.onPlaybackStarted()
//        youtubePlayer?.playerPlay()
//        playerPlay()
        playbackCallback.onPlayerReady()
    }

    override fun onPlaybackEndReached() {
        playbackCallback?.onPaybackEnd()
//        val pStateCompat = PlaybackStateCompat.Builder()
//                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS,
//                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
//                        1.0f, SystemClock.elapsedRealtime()).build()
//
//        playbackListener.onPlaybackChanged(pStateCompat)

    }

    private var state: Int = PlaybackStateCompat.STATE_NONE

    override fun onPlayerStateChange(s: WebObserver.PlayerState) {
        var playbackState = 0
        when (s) {
            WebObserver.PlayerState.PLAYING -> {
                playbackState = PlaybackStateCompat.STATE_PLAYING
            }
            WebObserver.PlayerState.BUFFERING -> {
                playbackState = PlaybackStateCompat.STATE_BUFFERING
            }
            WebObserver.PlayerState.UNSTARTED -> {
                playbackState = PlaybackStateCompat.STATE_CONNECTING
            }
            WebObserver.PlayerState.PAUSED -> {
                playbackState = PlaybackStateCompat.STATE_PAUSED
            }
            else -> {
                error { "unknown sate in youtube iframe player received" }
            }
        }
//        lastKnownPlaybackState = playbackState.toLong()
//        val pStateCompat = PlaybackStateCompat.Builder()
//                .setState(playbackState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
//                        1.0f, SystemClock.elapsedRealtime()).build()
//
//        playbackListener.onPlaybackChanged(pStateCompat)
        state = playbackState
        playbackCallback?.upatePlaybackState(playbackState)
    }

    override fun onPlaybackQualityChange(quality: String) {

    }

    override fun onPlaybackRateChange(rate: Int) {

    }

    override fun onErrorCode(code: WebObserver.ErrorCode) {
//        val pStateCompat = PlaybackStateCompat.Builder()
//                .setState(PlaybackStateCompat.ERROR_CODE_NOT_SUPPORTED,
//                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
//                        1.0f, SystemClock.elapsedRealtime())
//                .setErrorMessage(code.code, code.toString()).build()
//        playbackListener.onPlaybackChanged(pStateCompat)
        playbackCallback.onError(code.code, code.toString())


    }

    override fun onPlaybackPosition(seconds: Int) {


    }

    override fun onPlaybackPositionUpdate(seconds: Int) {

//        val pStateCompat = PlaybackStateCompat.Builder()
//                .setState(lastKnownPlaybackState.toInt(), seconds.toLong() * 1000,
//                        1.0f, SystemClock.elapsedRealtime()).build()
//        playbackListener.onPlaybackChanged(pStateCompat)
        playbackCallback?.updatePlaybackPosition(seconds)
    }

    override fun onVideoData(title: String, thumbail: String, duration: Int, loop: Boolean, videoId: String) {
//                val hash = "${videoId}${title}"
//                if (hash != metaDataHash) {
//                    val m = MediaMetadataCompat.Builder()
//                            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
//                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, videoId)
//                            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "subtitle")
//                            .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, "https://www.google.ch/imgres?imgurl=https%3A%2F%2Fwww.w3schools.com%2Fhowto%2Fimg_fjords.jpg&imgrefurl=https%3A%2F%2Fwww.w3schools.com%2Fhowto%2Fhowto_js_image_magnifier_glass.asp&docid=k0i2ftK0GIzuDM&tbnid=TVEPc8yBbrThFM%3A&vet=10ahUKEwiC-Iz95qDZAhUFzRQKHWhDCmIQMwi7ASgAMAA..i&w=600&h=400&bih=667&biw=1344&q=image&ved=0ahUKEwiC-Iz95qDZAhUFzRQKHWhDCmIQMwi7ASgAMAA&iact=mrc&uact=8")
//                            .build()
//                    metadataListener.onMetadataChanged(m)
//                }
    }

    interface Callback {
        fun upatePlaybackState(state: Int)
        fun updatePlaybackPosition(seconds: Int)
        fun onPaybackEnd()
        fun onPlayerReady()
        fun onError(code: Int, msg: String)
    }

}