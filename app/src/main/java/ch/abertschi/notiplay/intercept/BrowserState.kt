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
    private var currentState = State.RESET
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

    private enum class State {
        PLAYING,
        PAUSED,
        RESET
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
        }
    }

    fun updateSeekPosition(seconds: Int, context: Context) {
        resetState(context)
//        seekPos = seconds
        _duration = seconds.toLong()
        showNotification(context)
    }

    fun updateVideoTitle(title: String?, context: Context) {
        if (title == null) return

        if (title != videoTitle) {
            resetState(context)
        }
        this.videoTitle = title
    }

    fun onPlaybackStart(hash: String, context: Context) {
        info("starting playback counter")
        if (currentState == State.PLAYING && startStopHash == hash) return
        currentState = State.PLAYING

        BrowserAccessibilityService.INSTANCE?.performSwypeUpIfInValidApp()

        if (startStopHash != hash) {
            startStopHash = hash
            resetState(context)
        }
        val now = System.currentTimeMillis()
        lastStarted = now

        showNotification(context)
        info(" DURATION: " + getDuration())

    }

    private fun resetState(context: Context) {
        info("resetting state")
        _duration = 0
        currentState = State.RESET
        scrolPerformed = false
        removeNotification(context)

        if (currentState == State.PLAYING) {
            lastStarted = System.currentTimeMillis()
        }

    }

    fun onPlaybackPause(hash: String, context: Context) {
        showNotification(context)
        info("pausing playback counter")

        if (currentState == State.PAUSED && startStopHash == hash) return
//        info("currenetState: $currentState, startStopHash: $startStopHash, hash: $hash")

        currentState = State.PAUSED
        if (startStopHash != hash) {
            startStopHash = hash
            resetState(context)
        }
        val now = System.currentTimeMillis()
        _duration += ((now - lastStarted) / 1000)

        info(" DURATION: " + getDuration())
    }

    fun onPlaybackStop(hash: String, context: Context) {
        currentState = State.RESET
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
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    fun getCurrentStateHash(context: Context): String {
        return this.startStopHash
    }


    fun showNotification(context: Context) {
        if (this.videoUrl == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context)
        }

        val playIntent = Intent(context, PlayIntentService::class.java)
        playIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, getVideoIdFromUrl(videoUrl!!))
        playIntent.action = PlaybackService.ACTION_INIT_WITH_ID
        playIntent.putExtra(PlaybackService.EXTRA_SEEK_POS, getDuration())
        playIntent.putExtra(PlayIntentService.EXTRA_HASH, this.startStopHash)
        playIntent.putExtra(PlaybackService.EXTRA_PLAYBACK_STATE, "play")

        val audioOnlyIntent = Intent(context, PlayIntentService::class.java)
        audioOnlyIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, getVideoIdFromUrl(videoUrl!!))
        audioOnlyIntent.action = PlaybackService.ACTION_INIT_WITH_ID
        audioOnlyIntent.putExtra(PlaybackService.EXTRA_SEEK_POS, getDuration())
        audioOnlyIntent.putExtra(PlayIntentService.EXTRA_HASH, this.startStopHash)
        audioOnlyIntent.putExtra(PlaybackService.EXTRA_SHOW_UI, false)
        audioOnlyIntent.putExtra(PlaybackService.EXTRA_PLAYBACK_STATE, "play")

        val playPendingIntent =
                PendingIntent.getService(context, notificationId, playIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val audioOnlyPendingIntent =
                PendingIntent.getService(context, notificationId, audioOnlyIntent, PendingIntent.FLAG_CANCEL_CURRENT)



        var msg1 = "Continue playback at " + getDuration() + " seconds"

        var title = ""
        var subtitle = ""
        if (this.videoTitle != null) {
            title = this.videoTitle!!
            subtitle = msg1
        } else {
            title = msg1
            subtitle = ""
        }
        val b = NotificationCompat.Builder(context, CHANNEL_ID)
//            b.setDefaults(Notification.DEFAULT_VIBRATE or  Notification.DEFAULT_SOUND)
                .setSmallIcon(R.drawable.abc_cab_background_internal_bg)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(playPendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "PLAY", playPendingIntent)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "AUDIO ONLY", audioOnlyPendingIntent)

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
            notificationChannel.description = "transition from Youtube to Notiplay"
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

}