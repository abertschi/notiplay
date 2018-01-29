package ch.abertschi.notiplay

/**
 * Created by abertschi on 25.01.18.
 */
interface NotiRunnable {

    fun playVideoById(videoId: String, seekPosition: Int = 0)

    fun playerPause()

    fun playerPlay()

    fun playerStop()

    fun playerNextVideo()
    fun playerPreviousVideo()

    fun seekForward(seek: Int = 30)
    fun seekBackward(seek: Int = 30)
    fun seekToPosition(seconds: Int)

    fun addEventObserver(o: NotiObserver)
    fun removeEventObserver(o: NotiObserver)
    fun getPlaybackPosition()

    fun toggleWebview()
    fun setLoopMode(loopWhenEnd: Boolean)
    fun getVideoData()

    fun setFullscreen(state: Boolean)
}
