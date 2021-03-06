package ch.abertschi.notiplay.playback.yt

import android.content.Context
import android.webkit.JavascriptInterface
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * Created by abertschi on 25.01.18.
 */
class WebInterface(val acontext: Context, var observers: List<WebObserver>): AnkoLogger {

    @JavascriptInterface
    fun videoSrc(src: String) {
        observers.forEach { it.onVideoSrc(src) }
    }

    @JavascriptInterface
    fun onPlayerReady() {
        info { "ON PLAYER READY WEBINTERFACEf" }
        observers.forEach { it.onPlayerReady() }
    }

    @JavascriptInterface
    fun onPlayerStateChange(state: Int) {
        val s: WebObserver.PlayerState = WebObserver.PlayerState.toPlayerState(state)
        observers.forEach { it.onPlayerStateChange(s) }
    }

    @JavascriptInterface
    fun onPlaybackQualityChange(quality: String) {
        observers.forEach { it.onPlaybackQualityChange(quality) }
    }

    @JavascriptInterface
    fun onPlaybackRateChange(rate: Int) = observers.forEach { it.onPlaybackRateChange(rate) }


    @JavascriptInterface
    fun onErrorCode(code: Int) {
        val eCode = WebObserver.ErrorCode.toErrorEnum(code)
        observers.forEach { it.onErrorCode(eCode) }
    }

    @JavascriptInterface
    fun onPlaybackPosition(seconds: Int) {
        observers.forEach { it.onPlaybackPosition(seconds) }
    }

    @JavascriptInterface
    fun onPlaybackEndReached() {
        observers.forEach { it.onPlaybackEndReached()}
    }

    @JavascriptInterface
    fun onPlaybackPositionUpdate(seconds: Int) {
        observers.forEach { it.onPlaybackPositionUpdate(seconds) }
    }

    @JavascriptInterface
    fun onVideoData(title: String, thumbnail: String, duration: Int,
                    isLoop: Boolean, videoId: String) {
        observers.forEach { it.onVideoData(title, thumbnail, duration, isLoop, videoId) }
    }

    @JavascriptInterface
    fun onPlaybackPositionStatus(status: String) {
//        observers.forEach { it.onVideoData(title, thumbnail) }
    }
}
