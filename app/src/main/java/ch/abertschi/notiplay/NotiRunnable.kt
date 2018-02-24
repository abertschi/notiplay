package ch.abertschi.notiplay

import android.view.View

/**
 * Created by abertschi on 25.01.18.
 */
interface NotiRunnable {

    interface ViewController {

    }

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

    fun addEventObserver(o: NotiObserver)
    fun removeEventObserver(o: NotiObserver)

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
}
