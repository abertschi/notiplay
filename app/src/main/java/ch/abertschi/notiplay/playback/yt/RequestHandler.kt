package ch.abertschi.notiplay.playback.yt

import android.annotation.TargetApi
import android.os.Build
import android.os.Handler
import android.os.Looper.getMainLooper
import android.webkit.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.mortbay.log.Log.warn

/**
 * Created by abertschi on 23.02.18.
 */
class RequestHandler(val webView: WebView) : WebViewClient(), AnkoLogger {

    var debug: Boolean = true

    var onStop: (() -> Unit)? = null

    val handler = Handler(getMainLooper())

    private val httpClient = OkHttpClient()
    private val jqueryUrl = "http://code.jquery.com/jquery-3.3.1.min.js"
    private var googleYtV3VideoApi = "https://www.googleapis.com/youtube/v3/"

    private val blockedUrls = listOf<String>(
            "https://www.youtube.com/signin?context=popup",
            "https://www.youtube.com/share_ajax?action_get_share_info=",
            "https://www.youtube.com/watch?time_continue=",
            "photo.jpg", "https://googleads")

    private val httpHeaders = "accept, authorization, Content-Type, X-Walltime-Ms, " +
            "X-Restrict-Formats-Hint, X-Bandwidth-Est, X-Bandwidth-Est3, content-length"

    private var blockingRequests = false

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

            handler.post {
                info { "stop loading " + url }
                webView?.stopLoading()
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
            onStop?.invoke()
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