package ch.abertschi.notiplay

import android.content.Context
import android.webkit.JavascriptInterface

/**
 * Created by abertschi on 25.01.18.
 */
class WebInterface(val context: Context, var observers: List<NotiObserver>) {

    @JavascriptInterface
    fun hello(): Unit = throw UnsupportedOperationException("yay")

    @JavascriptInterface
    fun onPlayerReady() = observers.forEach { it.onPlayerReady() }

    @JavascriptInterface
    fun onPlayerStateChange(state: Int) {
        val s: NotiObserver.PlayerState = NotiObserver.PlayerState.toPlayerState(state)
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
        val eCode = NotiObserver.ErrorCode.toErrorEnum(code)
        observers.forEach { it.onErrorCode(eCode) }
    }

    @JavascriptInterface
    fun onPlaybackPosition(seconds: Int) {
        observers.forEach { it.onPlaybackPosition(seconds) }
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
