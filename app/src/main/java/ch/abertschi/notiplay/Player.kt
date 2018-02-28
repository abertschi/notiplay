package ch.abertschi.notiplay

import android.view.View
import ch.abertschi.notiplay.playback.yt.WebObserver

/**
 * Created by abertschi on 25.01.18.
 */
interface Player {

    interface ViewController {

    }

    fun getState(): Int
    fun playVideoById(videoId: String, seekPosition: Int = 0)

    fun playerPause()

    fun playerPlay()

    fun playerStop()

    fun getView(): View
    fun getViewController(): ViewController

    fun playerNextVideo()
    fun playerPreviousVideo()

    fun resetPlayer()

    fun seekForward(seek: Int = 30)
    fun seekBackward(seek: Int = 30)
    fun seekToPosition(seconds: Int)

    fun addEventObserver(o: WebObserver)
    fun removeEventObserver(o: WebObserver)

    @Deprecated("")
    fun getPlaybackPosition()

//    @Deprecated("")
//    fun toggleWebview()

    @Deprecated("")
    fun setLoopMode(loopWhenEnd: Boolean)

    @Deprecated("")
    fun getVideoData()

//    @Deprecated("")
//    fun toggleVisible()

//    @Deprecated("")
//    fun confirmFullscreen()
//    @Deprecated("")
//    fun setFullscreen(state: Boolean)

    interface Callback {
        fun upatePlaybackState(state: Int)
        fun updatePlaybackPosition(seconds: Int)
        fun onPaybackEnd()
        fun onPlayerReady()
        fun onError(code: Int, msg: String)
    }
}
