package ch.abertschi.notiplay.intercept

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
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
        scrolPerformed =true

    }

    fun onOriginPlayerInForeground(state: Boolean, context: Context) {
        println("player in foregrond: " + state)
        if (!state && originPlayerInForeground && currentState != State.PLAYING) {
            // player is put to background
            showHeadsUp(context)
            println("Player can launch here: $videoUrl at $seekPos")
        }
        originPlayerInForeground = state

    }

    fun updateVideoUrl(url: String, context: Context) {
        if (videoUrl != url) {
            videoUrl = url
        }
    }

    fun updateSeekPosition(seconds: Int, context: Context) {
        seekPos = seconds
        resetState()

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

//        println(" DURATION: " + getDuration())

    }

    private fun resetState() {
//        println("resetting state")
        duration = 0
        currentState = State.RESET
        scrolPerformed = false

    }

    fun onPlaybackPause(hash: String, context: Context) {
//        println("pausing playback counter")

        if (currentState == State.PAUSED && startStopHash == hash) return
//        println("currenetState: $currentState, startStopHash: $startStopHash, hash: $hash")

        currentState = State.PAUSED
        if (startStopHash != hash) {
            startStopHash = hash
            resetState()
        }
        val now = System.currentTimeMillis()
        duration += ((now - lastStarted) / 1000)

//        println(" DURATION: " + getDuration())
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


    fun showHeadsUp(context: Context) {
        //build notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context)
        }
        val b = NotificationCompat.Builder(context, CHANNEL_ID)
//            b.setDefaults(Notification.DEFAULT_VIBRATE or  Notification.DEFAULT_SOUND)
                .setSmallIcon(R.drawable.abc_cab_background_internal_bg)
                .setContentTitle("Continue playback in background?")
//                .setContentText("Tomorrow will be your birthday.")
                // TODO :set sound to void

                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "Yes", null)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "Always Yes", null)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "Never", null)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, b.build())

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