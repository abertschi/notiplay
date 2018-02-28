package ch.abertschi.notiplay.intercept

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import ch.abertschi.notiplay.R
import ch.abertschi.notiplay.playback.yt.YoutubeApiWrapper
import ch.abertschi.notiplay.PlaybackService
import org.jetbrains.anko.notificationManager


/**
 * Created by abertschi on 24.02.18.
 */
class YtNotificationListener2 : NotificationListenerService() {


    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    var youtube: YoutubeApiWrapper = YoutubeApiWrapper()

    var enabled = false
    private val CHANNEL_ID: String = "channid"

    private var transportControls: MediaController.TransportControls? = null
    var controller: MediaController? = null
    override fun onNotificationPosted(sbn: StatusBarNotification) {
//        println("ON NOTIFICATION RECEIVED")
//        println("status" + sbn.groupKey)

        if (sbn.groupKey.toString().contains("com.android.chrome")) {
            val extras = sbn.notification.extras
            var playbackTile = extras.get("android.title") as String
            var playbackUsername = extras.get("android.text") as String

            for (key: String in sbn.notification.extras.keySet()) {
                println(key + " -> " + sbn.notification.extras.get(key))
            }
            YtAccessibilityService.that?.active = true

            println("fetching videoId with $playbackUsername and $playbackTile")
//            youtube.getVideoIdBy(playbackUsername, playbackTile)
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(Schedulers.io())
//                    .subscribe(
//                            { n ->
//                                println("found videoId: $n")
//                                n?.run {
//                                    launch(this)
//
//                                }
//
//                            },
//                            { e ->
//                                println("ERROR: ")
//                                println(e)
//                            }
//                    )

            return

            try {
                token = sbn.notification.extras.get("android.mediaSession") as MediaSession.Token
                controller = MediaController(this, token)
                transportControls = controller!!.transportControls
                controller?.registerCallback(object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        super.onMetadataChanged(metadata)
                        println(metadata?.description)
                        println(" super.onMetadataChanged(metadata)")
                    }

                    override fun onSessionEvent(event: String?, extras: Bundle?) {
                        super.onSessionEvent(event, extras)
                        println(" super.onSessionEvent(metadata)")
                    }

                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        super.onPlaybackStateChanged(state)
                        println((state?.position))
                        println(" /// " + "POSE")
                        println(" super.onPlaybackStateChanged(metadata)")

                    }

                    override fun onQueueTitleChanged(title: CharSequence?) {
                        super.onQueueTitleChanged(title)
                        println("println(\" super.onQueueTitleChanged(metadata)\")")
                    }

                    override fun onSessionDestroyed() {
                        super.onSessionDestroyed()
                        println(" super.onSessionDestroyed(metadata)")
                    }
                })
                val pos = controller?.playbackState?.bufferedPosition
                println("BPOS:::::: " + pos)
                val pos2 = controller?.playbackState?.position
                println("POS:::::: " + pos2)
                println(token.toString())
            } catch (e: Exception) {
                println(e)
            }


        }


    }

    fun launch(videoId: String) {
        val notiIntent = Intent(this, PlaybackService::class.java)
        notiIntent.action = PlaybackService.ACTION_INIT_WITH_ID

        notiIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, videoId)
        notiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        println(videoId)
        startService(notiIntent)
    }

    var token: MediaSession.Token? = null

    fun showHeadsUp() {
        //build notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
//            b.setDefaults(Notification.DEFAULT_VIBRATE or  Notification.DEFAULT_SOUND)
                .setSmallIcon(R.drawable.abc_cab_background_internal_bg)
                .setContentTitle("Ping Notification")
                .setContentText("Tomorrow will be your birthday.")
                // TODO :set sound to void

                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "Launch", null)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "Always Launch", null)

        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, b.build())

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(CHANNEL_ID,
                    "headsup controls",
                    NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "notification option for playback control"
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
//        println("ON NOTIFICATION REMOVED")

        if (sbn.key.contains("chrome") && token != null) {
            val controller = MediaController(this, token)
            var transportControls = controller!!.transportControls

            transportControls?.play()
            println(controller?.queueTitle)
            println("playing again? ")
            println(transportControls)
        }


        if (sbn.key.contains("chrome") && enabled == true) {
            enabled = false
//            YtAccessibilityService.that?.active = true
        }
        // Implement what you want here
    }
}