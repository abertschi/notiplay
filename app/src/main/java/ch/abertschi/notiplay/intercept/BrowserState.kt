package ch.abertschi.notiplay.intercept

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import ch.abertschi.notiplay.PlaybackService
import ch.abertschi.notiplay.R
import ch.abertschi.notiplay.getVideoIdFromUrl
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * Created by abertschi on 08.03.18.
 */
class BrowserState : AnkoLogger {

    private val CHANNEL_ID: String = "channid"

    private var _duration: Long = 0
    private var lastStarted: Long = 0

    private var startStopHash = ""
    private var currentState = State.INIT
    private var originPlayerInForeground = false

    private var videoTitle: String? = null

    private val notificationId = 188

    private constructor() {
    }

    companion object {
        var GET: BrowserState = BrowserState()
    }

    private var videoUrl: String? = null
//    private var seekPos = 0

    enum class State {
        PLAYING,
        PAUSED,
        INIT
    }

    private var scrolPerformed = false

    fun onScrollPerformed() {
        scrolPerformed = true

    }

    fun onOriginPlayerInForeground(state: Boolean, context: Context) {
        if (!state && this.originPlayerInForeground) {
            showNotification(context)
        }

//        info("player in foregrond: " + state)
//        if (!state && originPlayerInForeground) {
//            // player is put to background
//            showNotification(context)
//            info("Player can launch here: $videoUrl at $seekPos")
//        }
        originPlayerInForeground = state

    }

    fun updateVideoUrl(url: String?, context: Context) {
        if (url == null) return

        if (videoUrl != url) {
            videoUrl = url
            videoTitle = null
            showNotification(context)

//            if (currentState == State.INIT) {
//
//            }

        }
    }

    fun updateSeekPosition(seconds: Int, context: Context) {
        //resetState(context)
//        seekPos = seconds
        _duration = seconds.toLong()
        info { "--- updating counter, duration: " + _duration }
        info(" DURATION: " + getDuration())
        if (currentState == State.PLAYING) {
            lastStarted = System.currentTimeMillis()
        }
    }

    fun updateVideoTitle(title: String?, context: Context) {
        if (title == null) return

        if (title != videoTitle) {
            _duration = 0
            showNotification(context)
            // resetState(context)
        }
        this.videoTitle = title
    }

    fun getPlaybackState() = this.currentState

    fun onPlaybackStart(hash: String, context: Context) {
        info("starting playback counter")
        info { "--- onplayback start: hash: " + hash }
        if (currentState == State.PLAYING && startStopHash == hash) return
        currentState = State.PLAYING

        if (startStopHash != hash) {
            startStopHash = hash
            _duration = 0
            BrowserAccessibilityService.INSTANCE?.performSwypeUpIfInValidApp()
            // resetState(context)
        }

        val now = System.currentTimeMillis()
        lastStarted = now
        info { "--- starting counter, duration: " + _duration }

        showNotification(context)
        info(" DURATION: " + getDuration())

    }

//    private fun resetState(context: Context) {
//        info { "--- resetting counter" }
//        info("resetting state")
//        _duration = 0
//        currentState = State.RESET
//        scrolPerformed = false
//        removeNotification(context)
//        lastStarted = System.currentTimeMillis()
//    }

    fun onPlaybackPause(hash: String, context: Context) {
        showNotification(context)
        info("pausing playback counter")

        if (currentState == State.PAUSED && startStopHash == hash) return
//        info("currenetState: $currentState, startStopHash: $startStopHash, hash: $hash")

        currentState = State.PAUSED
        if (startStopHash != hash) {
            //startStopHash = hash
            // resetState(context)
            //_duration = 0
        }
        val now = System.currentTimeMillis()
        _duration += ((now - lastStarted) / 1000)

        info { "--- stopping counter, ${now - lastStarted}: duration: ${_duration}" }

        info(" DURATION: " + getDuration())
    }

    fun onPlaybackStop(hash: String, context: Context) {
        // currentState = State.RESET
    }

    fun getDuration(): Long {
        var add: Long = 0
        if (currentState == State.PLAYING) {
            val now = System.currentTimeMillis()
            add = ((now - lastStarted) / 1000)
        }
        return _duration + add
    }

    fun removeNotification(context: Context) {

    }

    fun getCurrentStateHash(context: Context): String {
        return this.startStopHash
    }

    fun cleanNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

    }


    fun showNotification(context: Context) {
        if (this.videoUrl == null) return
        // if (this.videoTitle == null) return
        val videoId = getVideoIdFromUrl(videoUrl!!) ?: return


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context)
        }

        val playIntent = Intent(context, PlayIntentService::class.java)
        playIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, videoId)
        playIntent.action = PlaybackService.ACTION_INIT_WITH_ID
        playIntent.putExtra(PlaybackService.EXTRA_SEEK_POS, getDuration())
        playIntent.putExtra(PlayIntentService.EXTRA_HASH, this.startStopHash)
        playIntent.putExtra(PlaybackService.EXTRA_SHOW_UI, true)
        playIntent.putExtra(PlaybackService.EXTRA_PLAYBACK_STATE, "play")
        playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val audioOnlyIntent = Intent(context, PlayIntentService::class.java)
        audioOnlyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        audioOnlyIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, videoId)
        audioOnlyIntent.action = PlaybackService.ACTION_INIT_WITH_ID
        audioOnlyIntent.putExtra(PlaybackService.EXTRA_SEEK_POS, getDuration())
        audioOnlyIntent.putExtra(PlayIntentService.EXTRA_HASH, this.startStopHash)
        audioOnlyIntent.putExtra(PlaybackService.EXTRA_SHOW_UI, false)
        audioOnlyIntent.putExtra(PlaybackService.EXTRA_PLAYBACK_STATE, "play")

        val playPendingIntent =
                PendingIntent.getService(context, 5, playIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val audioOnlyPendingIntent =
                PendingIntent.getService(context, 10, audioOnlyIntent, PendingIntent.FLAG_CANCEL_CURRENT)


        var title = ""
        var subtitle = ""
        if (this.videoTitle != null) {
            title = this.videoTitle!!
            subtitle = "Recently played"
        } else {
            title = "Load video with Notiplay"
            subtitle = "Playback video in background"
        }

        val b = NotificationCompat.Builder(context, CHANNEL_ID)
//            b.setDefaults(Notification. or  Notification.DEFAULT_SOUND)
                .setSmallIcon(R.drawable.abc_cab_background_internal_bg)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(playPendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.notification_icon_background,
                        "PLAY", playPendingIntent)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "AUDIO ONLY", audioOnlyPendingIntent)

//                .addAction(R.drawable.common_google_signin_btn_icon_dark,
//                        "DOWNLOAD", audioOnlyPendingIntent)


        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, b.build())

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(CHANNEL_ID,
                    "Notiplay utils",
                    NotificationManager.IMPORTANCE_MIN)
            notificationChannel.setShowBadge(false)
            notificationChannel.enableLights(false)
            notificationChannel.description = "transition from Youtube to Notiplay"
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }

}