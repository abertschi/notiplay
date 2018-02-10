package ch.abertschi.notiplay.player

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat

/**
 * Created by abertschi on 10.02.18.
 */
class PlaybackService : Service() {
    lateinit var mediaSession: MediaSessionCompat
    lateinit var playbackNotifications: PlaybackNotificationManager


    override fun onCreate() {
        mediaSession = MediaSessionCompat(applicationContext, "notiplay_session")
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {

        })
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
}