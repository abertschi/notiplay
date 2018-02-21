package ch.abertschi.notiplay.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import ch.abertschi.notiplay.NotiObserver
import ch.abertschi.notiplay.YoutubePlayer
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.toast

/**
 * Created by abertschi on 10.02.18.
 */
class PlaybackManager(val playbackService: PlaybackService, val metadataListener: MetadataListener,
                      val playbackListener: PlaybackListener) : AnkoLogger {

    val youtubePlayer: YoutubePlayer = YoutubePlayer(playbackService)
    var videoIdOfCurrentVideo: String? = "" // remove?

    val metadataManager = MetadataManager(metadataListener)

    private var booted = false

    init {
        val youtubeCallback: NotiObserver = object : NotiObserver {

            var lastKnownPlaybackState: Long = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN

            override fun onPlayerReady() {
                playbackListener?.onPlaybackStarted()
                youtubePlayer?.playerPlay()
            }

            override fun onPlayerStateChange(s: NotiObserver.PlayerState) {
                var playbackState = 0
                when (s) {
                    NotiObserver.PlayerState.PLAYING -> {
                        playbackState = PlaybackStateCompat.STATE_PLAYING
                    }
                    NotiObserver.PlayerState.BUFFERING -> {
                        playbackState = PlaybackStateCompat.STATE_BUFFERING
                    }
                    NotiObserver.PlayerState.UNSTARTED -> {
                        playbackState = PlaybackStateCompat.STATE_CONNECTING
                    }
                    NotiObserver.PlayerState.PAUSED -> {
                        playbackState = PlaybackStateCompat.STATE_PAUSED
                    }
                    else -> {
                        error { "unknown sate in youtube iframe player received" }
                    }
                }
                lastKnownPlaybackState = playbackState.toLong()
                val pStateCompat = PlaybackStateCompat.Builder()
                        .setState(playbackState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                                1.0f, SystemClock.elapsedRealtime()).build()

                playbackListener.onPlaybackChanged(pStateCompat)
            }

            override fun onPlaybackQualityChange(quality: String) {

            }

            override fun onPlaybackRateChange(rate: Int) {

            }

            override fun onErrorCode(code: NotiObserver.ErrorCode) {
                val pStateCompat = PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.ERROR_CODE_NOT_SUPPORTED,
                                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                                1.0f, SystemClock.elapsedRealtime())
                        .setErrorMessage(code.code, code.toString()).build()
                playbackListener.onPlaybackChanged(pStateCompat)


            }

            override fun onPlaybackPosition(seconds: Int) {


            }

            override fun onPlaybackPositionUpdate(seconds: Int) {
                println(seconds.toLong())
                val pStateCompat = PlaybackStateCompat.Builder()
                        .setState(lastKnownPlaybackState.toInt(), seconds.toLong() * 1000,
                                1.0f, SystemClock.elapsedRealtime()).build()
                playbackListener.onPlaybackChanged(pStateCompat)
            }

            override fun onVideoData(title: String, thumbail: String, duration: Int, loop: Boolean, videoId: String) {
//                val hash = "${videoId}${title}"
//                if (hash != metaDataHash) {
//                    val m = MediaMetadataCompat.Builder()
//                            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
//                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, videoId)
//                            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "subtitle")
//                            .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, "https://www.google.ch/imgres?imgurl=https%3A%2F%2Fwww.w3schools.com%2Fhowto%2Fimg_fjords.jpg&imgrefurl=https%3A%2F%2Fwww.w3schools.com%2Fhowto%2Fhowto_js_image_magnifier_glass.asp&docid=k0i2ftK0GIzuDM&tbnid=TVEPc8yBbrThFM%3A&vet=10ahUKEwiC-Iz95qDZAhUFzRQKHWhDCmIQMwi7ASgAMAA..i&w=600&h=400&bih=667&biw=1344&q=image&ved=0ahUKEwiC-Iz95qDZAhUFzRQKHWhDCmIQMwi7ASgAMAA&iact=mrc&uact=8")
//                            .build()
//                    metadataListener.onMetadataChanged(m)
//                }
            }
        }
        youtubePlayer.addEventObserver(youtubeCallback)
    }


    private val mediaSessionCallback = object : MediaSessionCompat.Callback(), AnkoLogger {

        override fun onPause() {
            super.onPause()
            youtubePlayer.playerPause()
        }

        override fun onPlay() {
            super.onPlay()
            youtubePlayer.playerPlay()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
//            youtubePlayer.playerNextVideo() // TODO: Metadata
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
//            youtubePlayer.playerPreviousVideo() // TODO: Metadata
        }

        override fun onStop() {
            youtubePlayer.startWebView()
            youtubePlayer.playerStop()
            playbackListener.onPlaybackStoped()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            youtubePlayer.seekToPosition(pos.toInt())
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
                youtubePlayer.playerPause()

            } else if (action == PlaybackNotificationManager.ACTION_SHOW_VIDEO_PLAYER) {
                youtubePlayer?.toggleVisible()
                playbackService.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                playbackService?.toast("Toggling playback window")

            }
        }
    }

    fun startPlaybackWithVideoId(id: String) {
        videoIdOfCurrentVideo = id
        metadataManager.setVideoId(id)
        metadataManager.fetchMetadata()

        if (!booted) {
            booted = true
            youtubePlayer.startWebView()
        }
        youtubePlayer.playVideoById(id)
    }

    fun getMediaSessionCallback(): MediaSessionCompat.Callback = mediaSessionCallback

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