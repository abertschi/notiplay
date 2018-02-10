package ch.abertschi.notiplay.player

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * Created by abertschi on 10.02.18.
 */
class PlaybackManager(playbackService: PlaybackService, metadataListener: MetadataListener): AnkoLogger {

    init {
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback(), AnkoLogger {

        override fun onPause() {
            super.onPause()
            this.info ("onPause from PlaybackManager")
        }

        override fun onPlay() {
            super.onPlay()
            info("onPause from PlaybackManager")
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
        }

        override fun onStop() {
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            super.onCustomAction(action, extras)
        }
    }

    fun getMediaSessionCallback(): MediaSessionCompat.Callback = mediaSessionCallback

    fun togglePlayerWindow() {
    }


    interface MetadataListener {
        fun onVideoIdChanged(id: String)
        fun onMetadataChanged(metadata: MediaMetadataCompat)
    }
}