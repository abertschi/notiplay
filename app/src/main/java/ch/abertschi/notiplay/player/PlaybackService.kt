package ch.abertschi.notiplay.player

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat

/**
 * Created by abertschi on 10.02.18.
 */
class PlaybackService : Service(), PlaybackManager.MetadataListener {

    lateinit var mediaSession: MediaSessionCompat
    lateinit var playbackNotifications: PlaybackNotificationManager

    private var playbackManager: PlaybackManager = PlaybackManager(this, this)


    override fun onCreate() {
        mediaSession = MediaSessionCompat(applicationContext, "notiplay_session")
        mediaSession.setCallback(playbackManager.getMediaSessionCallback())

        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        playbackNotifications = PlaybackNotificationManager(this)
        playbackNotifications.startNotifications()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

//    fun getMediaController() = mediaController

    fun getSessionToken() = mediaSession.sessionToken

    fun togglePlayerWindow() {
        playbackManager?.togglePlayerWindow()
    }

    fun showFromOrigin() {
    }

    override fun onVideoIdChanged(id: String) {
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat) {
//        mediaSession?.controller?.
    }
}