package ch.abertschi.notiplay.player

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

/**
 * Created by abertschi on 10.02.18.
 */
class PlaybackService : Service(), PlaybackManager.MetadataListener, PlaybackManager.PlaybackListener {

    companion object {
        val ACTION_INIT_WITH_ID = "action_init_with_id"
        val EXTRA_VIDEO_ID = "extra_video_id"
    }

    lateinit var mediaSession: MediaSessionCompat
    lateinit var playbackNotifications: PlaybackNotificationManager
    private var currentVideoId: String = ""

    private var playbackManager: PlaybackManager = PlaybackManager(this, this, this)


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
        super.onStartCommand(intent, flags, startId)
        if (intent != null && intent.action == ACTION_INIT_WITH_ID) {
            intent.getStringExtra(EXTRA_VIDEO_ID)?.run {
                playVideoId(this)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun playVideoId(id: String) {
        currentVideoId = id
        playbackManager.startPlaybackWithVideoId(id)
    }

    fun getSessionToken() = mediaSession.sessionToken

    override fun onMetadataChanged(metadata: MediaMetadataCompat) {
        mediaSession.setMetadata(metadata)
    }

    override fun onPlaybackChanged(state: PlaybackStateCompat) {
        mediaSession.setPlaybackState(state)
    }

    override fun onPlaybackStarted() {
    }

    override fun onPlaybackStoped() {
    }

    override fun onVideoIdChanged(id: String) {
        playVideoId(id)
    }
}