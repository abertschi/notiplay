package ch.abertschi.notiplay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


/**
 * Created by abertschi on 10.02.18.
 */
class PlaybackService : Service(), PlaybackManager.MetadataListener, PlaybackManager.PlaybackListener {

    companion object {
        val ACTION_INIT_WITH_ID = "action_init_with_id"
        val EXTRA_VIDEO_ID = "extra_video_id"
        val EXTRA_SHOW_UI = "extra_show_ui"
        val EXTRA_PLAYBACK_STATE = "extra_playback_state"
        val EXTRA_SEEK_POS = "extra_seek_pos"
    }

    lateinit var mediaSession: MediaSessionCompat
    lateinit var playbackNotifications: PlaybackNotificationManager
    private var currentVideoId: String = ""

    private var playbackManager: PlaybackManager? = null


    override fun onCreate() {
        playbackManager = PlaybackManager(this, this, this)
        mediaSession = MediaSessionCompat(applicationContext, "notiplay_session")
        mediaSession.setCallback(playbackManager!!.getMediaSessionCallback())

        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        playbackNotifications = PlaybackNotificationManager(this)
        playbackNotifications.startNotifications()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        println("START FROM SERVICE: ${intent!!.action}")

        intent?.run {
            if (this.action == ACTION_INIT_WITH_ID) {

                val videoId: String? = intent.getStringExtra(EXTRA_VIDEO_ID)
                val showPlayerUi: Boolean = intent.getBooleanExtra(EXTRA_SHOW_UI, true)
                val seekPos = intent.getLongExtra(EXTRA_SEEK_POS, 0)
                var playbackState = PlaybackManager.PlaybackStartState.PLAY

                intent.getStringExtra(EXTRA_PLAYBACK_STATE)?.run {
                    if (this == "pause") playbackState = PlaybackManager.PlaybackStartState.PAUSE
                }

                if (connCheck == null)
                    checkConnectivity() // TODO

                if (videoId == null) {
                    error("Missing argument videoId. Cannot launch player without it")
                } else {
                    playVideoId(PlaybackManager.StartPlaybackWithVideoIdRequest(videoId!!,
                            startState = playbackState, seekPos = seekPos, showPlayerUi = showPlayerUi))
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun playVideoId(command: PlaybackManager.StartPlaybackWithVideoIdRequest) {
        currentVideoId = command.id
        playbackManager!!.startPlaybackWithVideoId(command)
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
        playVideoId(PlaybackManager.StartPlaybackWithVideoIdRequest(id))
    }

    fun shutdownService() {
        val myService = Intent(this, this::class.java)
        stopService(myService)

        super.stopSelf()
        playbackManager = null


    }

    override fun onDestroy() {
        super.onDestroy()
        connCheck?.dispose()
    }

    private var connCheck: Disposable? = null

    fun checkConnectivity() {
        connCheck = Observable.just(true)
                .delay(5, TimeUnit.SECONDS)
                .repeat()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).map {
            if (isNetworkAvailable()) {
                playbackNotifications.showMessageNoConnectivity(false)
            } else {
                playbackNotifications.showMessageNoConnectivity(true)
            }
        }.subscribe()

    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

}