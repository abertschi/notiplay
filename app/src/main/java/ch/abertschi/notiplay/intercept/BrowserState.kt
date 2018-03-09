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

/**
 * Created by abertschi on 08.03.18.
 */
class BrowserState {

    private val CHANNEL_ID: String = "channid"

    private var duration: Long = 0
    private var lastStarted: Long = 0

    private var startStopHash = ""
    private var currentState = State.RESET
    private var originPlayerInForeground = false

    private var videoTitle: String? = null

    private constructor() {
    }

    companion object {
        var GET: BrowserState = BrowserState()
    }

    private var videoUrl: String? = null
    private var seekPos = 0

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

        showNotification(context)
//        println("player in foregrond: " + state)
//        if (!state && originPlayerInForeground) {
//            // player is put to background
//            showNotification(context)
//            println("Player can launch here: $videoUrl at $seekPos")
//        }
        originPlayerInForeground = state

    }

    fun updateVideoUrl(url: String, context: Context) {
        if (videoUrl != url) {
            videoUrl = url
        }
    }

    fun updateSeekPosition(seconds: Int, context: Context) {
        seekPos = seconds
        showNotification(context)
        resetState()

    }

    fun updateVideoUrl(url: String?) {
        if (url == null) return
        this.videoUrl = url
    }

    fun updateVideoTitle(title: String?) {
        this.videoTitle = title
    }

    fun onPlaybackStart(hash: String, context: Context) {
        println("starting playback counter")
        if (currentState == State.PLAYING && startStopHash == hash) return
        currentState = State.PLAYING

        if (startStopHash != hash) {
            startStopHash = hash
            resetState()
        }
        val now = System.currentTimeMillis()
        lastStarted = now

        showNotification(context)
        println(" DURATION: " + getDuration())

    }

    private fun resetState() {
        println("resetting state")
        duration = 0
        currentState = State.RESET
        scrolPerformed = false

    }

    fun onPlaybackPause(hash: String, context: Context) {
        showNotification(context)
        println("pausing playback counter")

        if (currentState == State.PAUSED && startStopHash == hash) return
//        println("currenetState: $currentState, startStopHash: $startStopHash, hash: $hash")

        currentState = State.PAUSED
        if (startStopHash != hash) {
            startStopHash = hash
            resetState()
        }
        val now = System.currentTimeMillis()
        duration += ((now - lastStarted) / 1000)

        println(" DURATION: " + getDuration())
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
        return duration + add
    }


    fun showNotification(context: Context) {
        //build notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context)
        }

        val notificationId = 188

//        val playbackIntent = Intent(context, PlaybackService::class.java)
//
//        val playPendingIntent = PendingIntent.getService(context, notificationId,
//                playbackIntent, PendingIntent.FLAG_CANCEL_CURRENT)


        val intent = Intent(context, PlayIntentService::class.java)
        intent.putExtra(PlaybackService.EXTRA_VIDEO_ID, videoUrl)
        intent.action = PlaybackService.ACTION_INIT_WITH_ID
        intent.putExtra(PlaybackService.EXTRA_SEEK_POS, this.seekPos.toLong())
        intent.putExtra(PlaybackService.EXTRA_PLAYBACK_STATE, "play")

        val pendingIntent = PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_CANCEL_CURRENT)


//        val view = RemoteViews("ch.abertschi.notiplay", R.layout.notification)
//        view.setOnClickPendingIntent(R.id.notification_closebtn_ib, playPendingIntent)
//        builder.setContent(view)


        var msg1 = "Continue playback at " + this.seekPos + " seconds"

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
                .setContentIntent(pendingIntent)
                // TODO :set sound to void
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "PLAY", pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, b.build())

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(CHANNEL_ID,
                    "headsup controls",
                    NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "notification option for playback control"
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

}