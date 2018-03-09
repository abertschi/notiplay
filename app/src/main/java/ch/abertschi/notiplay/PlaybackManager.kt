package ch.abertschi.notiplay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import ch.abertschi.notiplay.playback.yt.YoutubeMetadata
import ch.abertschi.notiplay.playback.yt.YoutubePlayer
import ch.abertschi.notiplay.view.FloatingWindowController
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.jetbrains.anko.warn


/**
 * Created by abertschi on 10.02.18.
 */
class PlaybackManager(val playbackService: PlaybackService, val metadataListener: MetadataListener,
                      val playbackListener: PlaybackListener) : AnkoLogger, Player.Callback {

    enum class PlaybackStartState {
        PLAY, PAUSE
    }

    val player: YoutubePlayer = YoutubePlayer(playbackService, this)

    var floatingWindowController: FloatingWindowController? = null


    var videoIdOfCurrentVideo: String? = "" // remove?

    val metadataManager = YoutubeMetadata(metadataListener)

    private var tasksOnPlayerReady: ArrayList<(() -> Unit)>? = ArrayList<(() -> Unit)>()

    private var booted = false
    private var playerReady = false

    private val mediaSessionCallback = object : MediaSessionCompat.Callback(), AnkoLogger {

        override fun onPause() {
            super.onPause()
            player.playerPause()
        }

        override fun onPlay() {
            super.onPlay()
            player.playerPlay()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            info { "onSkipToNext" }
//            player.playerNextVideo() // TODO: Metadata
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            info { "onSkipToPrevious" }
            player.seekToPosition(0)
            player.playerPlay()
//            player.playerPreviousVideo() // TODO: Metadata
        }

        override fun onStop() {
            player.playerStop()
            floatingWindowController?.stopFloatingWindow()
            playbackListener.onPlaybackStoped()
            playbackService?.shutdownService()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            player.seekToPosition(pos.toInt())
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            super.onCustomAction(action, extras)
            if (action == PlaybackNotificationManager.ACTION_SHOW_IN_SOURCE_APP) {
                val id = videoIdOfCurrentVideo
                val seconds = (playbackService.mediaSession.controller.playbackState.position / 1000).toInt()
                val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://youtube.com/watch?v=${id}&t=${seconds}"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                playbackService.startActivity(intent)

                playbackService.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                player.playerPause()
                floatingWindowController?.setVisible(false)

            } else if (action == PlaybackNotificationManager.ACTION_SHOW_VIDEO_PLAYER) {
                playbackService.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                floatingWindowController?.run {
                    if (this.isVisible() == true) {
                        playbackService?.toast("Hiding floating window")
                        this.setVisible(false)
                    } else {
                        playbackService?.toast("Showing floating window")
                        this.setVisible(true)
                    }
                }
            }
        }
    }

    data class StartPlaybackWithVideoIdRequest(val id: String,
                                               val startState: PlaybackStartState = PlaybackStartState.PLAY,
                                               val seekPos: Long = 0, val showPlayerUi: Boolean = true)


    fun startPlaybackWithVideoId(request: StartPlaybackWithVideoIdRequest) {
        info { "playing with id: $request.id" }
        videoIdOfCurrentVideo = request.id
        metadataManager.setVideoId(request.id)
        metadataManager.fetchMetadata()

        info { "boot state: $booted" }
        if (!booted) {
            booted = true
            info { "starting webview" }
            player.startWebView()
            floatingWindowController = FloatingWindowController(playbackService, playbackService)
            floatingWindowController?.setVisible(request.showPlayerUi)
            floatingWindowController?.startFloatingWindow(player.getView())
        }

        val cmd = {
            floatingWindowController?.setVisible(request.showPlayerUi) // todo: generalize this
            player.playVideoById(request.id)
            player.seekToPosition(request.seekPos.toInt())
            if (request.startState == PlaybackStartState.PLAY) {
                player.playerPlay()
            }
        }

        if (!playerReady) {
            info { "player not yet ready" }
            tasksOnPlayerReady?.add {
                info { "tasks on player ready: play videoById" }
                cmd.invoke()
            }
        } else {
            info { "playVideoById because player was already ready" }
            cmd.invoke()
        }
    }

    fun getMediaSessionCallback(): MediaSessionCompat.Callback = mediaSessionCallback

    override fun onError(code: Int, msg: String) {
        warn { "error: $code: $msg" }
    }

    override fun upatePlaybackState(state: Int) {
//        if (state == PlaybackStateCompat.STATE_PLAYING && )
        // private hasPlaybackStarted = false


        val pStateCompat = PlaybackStateCompat.Builder()
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1.0f, SystemClock.elapsedRealtime()).build()
        playbackListener.onPlaybackChanged(pStateCompat)
    }

    override fun updatePlaybackPosition(seconds: Int) {
        val pStateCompat = PlaybackStateCompat.Builder()
                .setState(player.getState(), seconds.toLong() * 1000,
                        1.0f, SystemClock.elapsedRealtime()).build()
        playbackListener.onPlaybackChanged(pStateCompat)
    }

    override fun onPaybackEnd() {
        val pStateCompat = PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1.0f, SystemClock.elapsedRealtime()).build()
        playbackListener.onPlaybackChanged(pStateCompat)
    }

    override fun onPlayerReady() {
        info { "player finally ready" }
        playerReady = true
        tasksOnPlayerReady?.forEach {
            it()
        }
        tasksOnPlayerReady = ArrayList()
    }


    interface MetadataListener {
        fun onVideoIdChanged(id: String)
        fun onMetadataChanged(metadata: MediaMetadataCompat)
    }


    interface PlaybackListener {
        fun onPlaybackStarted()
        fun onPlaybackStoped()
        fun onPlaybackChanged(state: PlaybackStateCompat)

        // https://developer.android.com/reference/android/support/v4/media/session/PlaybackStateCompat.Builder.html#addCustomAction(java.lang.String, java.lang.String, int)
        // https://github.com/googlesamples/android-UniversalMusicPlayer/blob/67a35ffefff9cd1c04089284492caa73dde8cae3/mobile/src/main/java/com/example/android/uamp/playback/PlaybackManager.java
    }
}