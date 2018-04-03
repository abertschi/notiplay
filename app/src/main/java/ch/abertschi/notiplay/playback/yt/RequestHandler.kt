package ch.abertschi.notiplay.playback.yt

import android.annotation.TargetApi
import android.os.Build
import android.os.Handler
import android.os.Looper.getMainLooper
import android.support.annotation.Nullable
import android.webkit.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.mortbay.log.Log.warn
import java.io.InputStream

/**
 * Created by abertschi on 23.02.18.
 */
class RequestHandler(val webView: WebView) : WebViewClient(), AnkoLogger {

    var onStop: (() -> Unit)? = null
    var userAgentIphone4 = "Mozilla/5.0(iPad; U; CPU iPhone OS 3_2 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Version/4.0.4 Mobile/7B314 Safari/531.21.10"
    var userAgentIphone5 = "Mozilla/5.0 (iPhone; CPU iPhone OS 5_0 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3"
    var useragent = userAgentIphone4
    var userAgentIpad = "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) CriOS/30.0.1599.12 Mobile/11A465 Safari/8536.25 (3B92C18B-D9DE-4CB7-A02A-22FD2AF17C8F"
    val handler = Handler(getMainLooper())

    private var fetchDownload = true

    var fixOrigin: Boolean = true

    private var downloadHandle: DownloadHandle? = null


    fun setDownloadInterceptor(h: DownloadHandle) {
        downloadHandle = h
    }

    fun enableDownloadInterceptor(state: Boolean) = fetchDownload


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
            return getNewResponse(url, null)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    @org.jetbrains.annotations.Nullable
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?):
            WebResourceResponse? {
        val url = request?.url.toString()
//        debug { "loading " + url }
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
            getNewResponse(url, request)
        }
    }

    private fun getNewResponse(url: String, req: WebResourceRequest?): WebResourceResponse? {
        if (url.contains("/endProcess")) {
            onStop?.invoke()
            return WebResourceResponse("bgsType", "someEncoding", null)
        }

        info { "#intercepting: " + url }


        val address = url.trim { it <= ' ' }
        // if (!url.contains("youtube")) return null

        var origin = if (url.startsWith(googleYtV3VideoApi)) {
            "null"
        } else if (url.startsWith("https://fonts.gst")) {
            "https://fonts.gstatic.com"
        } else {
            "https://www.youtube.com"
        }
        try {
            val b = Request.Builder()
                    .url(address)

            b.addHeader("User-Agent",
                    useragent)
//


            if (fixOrigin) {
                b.addHeader("Origin", origin)
                        .addHeader("origin", origin)
                        .addHeader("Referer", origin)
                        .addHeader("Access-Control-Allow-Credentials",
                                "false")
            }

            if (req?.method == "POST") {
                 b.post(RequestBody.create(null, ""))
            }
            val request = b.build()

            val response = httpClient.newCall(request).execute()
            val headers = HashMap<String, String>()


            headers.run {
                put("Connection", "close")
                put("Access-Control-Allow-Methods", "POST,GET,OPTIONS,PUT,DELETE")
                put("Access-Control-Max-Age", "1200")
                put("Access-Control-Allow-Origin", origin)
                put("Access-Control-Allow-Credentials", "true")
                put("Access-Control-Expose-Headers", httpHeaders)
                put("User-Agent", useragent)
                put("Access-Control-Allow-Headers", httpHeaders)
            }

            val contentType = response.headers().get("Content-Type")
            var contentTypeRes: String? = null

            contentType?.run {
                val contentTypeArry = contentType.split(";")
                try {

                    contentTypeRes = contentTypeArry[0]
                    info { "CONTENT TYPE RES: " + contentTypeRes }
                } catch (e: Exception) {
                }
            }

            if (fetchDownload && contentTypeRes != null) {
                if (contentTypeRes!!.startsWith("audio/")) {
                    downloadHandle?.onVideoUrlFetch(contentTypeRes!!, url)
                } else if (contentTypeRes!!.startsWith("video/")) {
                    downloadHandle?.onVideoUrlFetch(contentTypeRes!!, url)
                }
            }

            var stream: InputStream? = response.body()?.byteStream()
            if (!contentTypeRes!!.contains("video") && !contentTypeRes!!.contains("audio")
            && !contentTypeRes!!.contains("text/html")) {
                val str = response.body()!!.string()
                stream = str.byteInputStream()

//                println("=== response ====")
//                println(str)
//                System.out.flush()
//                println("=== response ====")
            }

            return WebResourceResponse(contentTypeRes,
                    response.header("content-encoding", "utf-8")
                    , 200,
                    "Ok",
                    headers,
                    stream)

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

    override fun onPageFinished(view: WebView?, url: String?) {
        println("ON PAGE FINISHEDDD")
        super.onPageFinished(view, url)
        downloadHandle?.onPageinished(view, url)
    }

    interface DownloadHandle {
        fun onVideoUrlFetch(contentType: String, url: String)
        fun onPageinished(view: WebView?, url: String?)
    }
}