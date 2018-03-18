package ch.abertschi.notiplay.playback.yt

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.Nullable
import android.webkit.*
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.github.kittinunf.fuel.httpGet
import okhttp3.OkHttpClient
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.wtf


/**
 * Created by abertschi on 14.03.18.
 */
class DownloadPlayer(val context: Context) : AnkoLogger {

    var webView: WebView? = null
    private var httpClient = OkHttpClient()

    init {
        val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))
        httpClient = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build()
//
    }

    fun loadWithVideoId(videoId: String) {
        webView = WebView(context)
        webView?.let {
            configureWebView(it.settings)
            it.setInitialScale(1)
            it.loadUrl("https://m.youtube.com/watch?v=TgA2y-Bgi3c")
        }
        webView?.webViewClient = object : WebViewClient() {

            @Nullable
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?):
                    WebResourceResponse? {
                request!!.requestHeaders?.put("User-Agent", "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) CriOS/30.0.1599.12 Mobile/11A465 Safari/8536.25 (3B92C18B-D9DE-4CB7-A02A-22FD2AF17C8F)")
                info { request?.url }


                try {
                    val url = request!!.url.toString()
                    info { "==== " + url }
                    url.httpGet().response {request, resp, result ->
                        info { "==== HEADERS: " + resp.headers.get("Content-Type") }
                        val mimeTypeHeaders = resp.headers.get("Content-Type")
                        if (mimeTypeHeaders != null && mimeTypeHeaders.isNotEmpty()) {
                            val mt = mimeTypeHeaders[0]
                            if (mt.startsWith("video")) {
                                // a = true
                                // val i = Intent(Intent.ACTION_VIEW)
                                // i.data = Uri.parse(url)
                                //context.startActivity(i)
                                info { "_________" + mt + "\n\n" + url}
                            }
                        }
                    }
                } catch (e: Exception) {
                    wtf(e)
                }

                return super.shouldInterceptRequest(view, request)

            }

//                val url = request?.url

//
////                if (view == null || request == null) return WebResourceResponse("bgsType", "someEncoding", null)
//
//
//                info {
//                    for (entry in request!!.requestHeaders.entries) {
//                        println(entry.key + ": " + entry.value)
//                    } }
//                return if (url != null) getNewResponse(url.toString())!!
//                else super.shouldInterceptRequest(view, request)
//            }
//
//            private fun getNewResponse(url: String): WebResourceResponse? {
//                info { "#intercepting: " + url }
//                try {
//                    val b = Request.Builder()
//                            .url(url)
//                            .addHeader("Referer", "https://m.youtube.com/watch?v=TgA2y-Bgi3c")
////                            .addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
//                            // image/webp,image/apng,image/*,*/*;q=0.8
//                            .addHeader(
//                                    "accept", "image/webp,image/apng,image/*,*/*;q=0.8")
//                            .addHeader("user-agent",
//                                    "Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) CriOS/30.0.1599.12 Mobile/11A465 Safari/8536.25 (3B92C18B-D9DE-4CB7-A02A-22FD2AF17C8F)")
//
//
//
//
////                    var origin = if (url.startsWith("andring")) {
////                        "null"
////                    } else {
////                        "https://www.youtube.com"
////                    }
//
//                    val httpHeaders = "accept, authorization, Content-Type, X-Walltime-Ms, " +
//                            "X-Restrict-Formats-Hint, X-Bandwidth-Est, X-Bandwidth-Est3, content-length"
////
////                    b.addHeader("Origin", origin)
////                            .addHeader("origin", origin)
////                            .addHeader("Referer", origin)
////                            .addHeader("Access-Control-Allow-Credentials",
////                                    "false")
//
//
//                    val request = b.build()
//                    val response = httpClient.newCall(request).execute()
//                    val headers = HashMap<String, String>()
//
//                    headers.run {
//                        put("Connection", "close")
//                        put("Access-Control-Allow-Methods", "POST,GET,OPTIONS,PUT,DELETE")
//                        put("Access-Control-Max-Age", "1200")
//                        // put("Access-Control-Allow-Origin", origin)
//                        put("Access-Control-Allow-Credentials", "true")
//                        put("Access-Control-Expose-Headers", httpHeaders)
//                        put("Access-Control-Allow-Headers", httpHeaders)
//                    }
//
//
//                    return WebResourceResponse(null,
//                            response.header("content-encoding", "utf-8")
//                            , 200,
//                            "Ok",
//                            headers,
//                            response.body()?.byteStream())
//
//                } catch (e: Exception) {
//                    warn("error with " + url)
//                    warn(e)
//                    return null
//                }
//            }

            override fun onPageFinished(view: WebView, url: String) {
                info { "start playing" }
                // webView?.loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()")
            }
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

}