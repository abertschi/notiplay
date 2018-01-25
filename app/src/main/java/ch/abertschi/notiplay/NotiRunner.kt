package ch.abertschi.notiplay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Handler
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.timerTask
import android.os.Looper


/**
 * Created by abertschi on 07.01.18.
 */

class NotiRunner : Service(), NotiRunnable {

    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private val webAsset: String = "notiplay.html"
    private var observers: MutableList<NotiObserver> = ArrayList()

    var debug: Boolean = true

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

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
        params.y = 0
        params.width = if (debug) 1000 else 0
        params.height = if (debug) 1000 else 0

        this.webView = WebView(this)

        webView?.webViewClient = object : WebViewClient() {

            override fun onReceivedError(view: WebView, request: WebResourceRequest,
                                         error: WebResourceError) {
                Log.d("Error", "loading web view: request: $request error: $error")
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest):
                    WebResourceResponse? {

                if (request.url.toString().contains("/endProcess")) {
                    windowManager.removeView(webView)
                    webView?.post { webView?.destroy() }
                    stopSelf()
                    return WebResourceResponse("bgsType", "someEncoding", null)
                } else {
                    return null
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                //view.loadUrl("javascript:(function() { document.getElementById('ytplayer').click(); })()");
            }

        }
        webView?.addJavascriptInterface(WebInterface(this, observers), "NotiPlay")

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

        //val timer
        val timer = Timer()
        timer.schedule(timerTask { playVideoById("ESPJoAPhkB4") }, 3000)
        timer.schedule(timerTask { seekForward() }, 10000)
        timer.schedule(timerTask { seekForward(100) }, 11000)
        timer.schedule(timerTask { seekForward(10) }, 12000)
        timer.schedule(timerTask { seekBackward(40) }, 14000)
        timer.schedule(timerTask { seekBackward(5) }, 16000)

        return Service.START_STICKY
    }


    fun loadWebpage(): String {
        val stream = this.assets.open(webAsset)
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        return buffer.toString(Charset.defaultCharset())
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStart(intent: Intent, startId: Int) {
        super.onStart(intent, startId)
        Log.i(TAG, "onStart")
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

    fun execJs(command: String) {
        handler.post {
            System.out.println("playing with id")
            webView!!.loadUrl("javascript:${command}")
        }

    }
}
