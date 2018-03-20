package ch.abertschi.notiplay.playback.yt

import android.webkit.WebView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*


/**
 * Created by abertschi on 09.03.18.
 */
class DownloadManager : AnkoLogger, RequestHandler.DownloadHandle {

    override fun onPageinished(view: WebView?, url: String?) {

    }

    private var lastValidAudioUrl: String? = null
    private var lastValidVideoUrl: String? = null


    override fun onVideoUrlFetch(contentType: String, url: String) {
        var splits = url.split("&")
        var newUrlComp = ""
        val ignore = listOf<String>("range", "rn", "rbuf")
        for (split in splits) {
            var ig = false
            for (i in ignore) {
                if (split.startsWith(i)) {
                    ig = true
                    break
                }
            }
            if (!ig) {
                newUrlComp += split + "&"
            }
        }
        var newUrl = newUrlComp


        info { contentType + " " + newUrl }
        if (contentType.startsWith("video/")) {
            lastValidVideoUrl = newUrl
            if (contentType == "video/mp4") {

            }

        } else if (contentType.startsWith("audio/")) {
            lastValidAudioUrl = newUrl

        }
    }

    fun download(videoId: String, url: String) {

    }

    @Throws(UnsupportedEncodingException::class)
    fun splitQuery(url: URL): Map<String, List<String>> {
        val queryPairs: HashMap<String, LinkedList<String>> = LinkedHashMap<String, LinkedList<String>>()
        val pairs = url.getQuery().split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            val key = if (idx > 0) URLDecoder.decode(pair.substring(0, idx), "UTF-8") else pair
            if (!queryPairs.containsKey(key)) {
                queryPairs[key] = LinkedList<String>()
            }
            val value = if (idx > 0 && pair.length > idx + 1) URLDecoder.decode(pair.substring(idx + 1), "UTF-8") else null
            value?.run {
                queryPairs[key]!!.add(value)
            }
        }
        return queryPairs
    }

    inner class QueryString() {

        var query = ""
            private set

        init {
        }

        fun fromMap(map: Map<String, List<String>>) {
            for (entry in map) {
                for (value in entry.value) {
                    encode(entry.key, value)
                }
            }
        }

        fun add(name: String, value: String) {
            query += "&"
            encode(name, value)
        }

        private fun encode(name: String, value: String) {
            try {
                query += URLEncoder.encode(name, "UTF-8")
                query += "="
                query += URLEncoder.encode(value, "UTF-8")
            } catch (ex: UnsupportedEncodingException) {
                throw RuntimeException("Broken VM does not support UTF-8")
            }

        }

        override fun toString(): String {
            return query
        }

    }

}