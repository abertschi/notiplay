package ch.abertschi.notiplay.playback.yt

import android.webkit.WebView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


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

}