package ch.abertschi.notiplay.intercept

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import ch.abertschi.notiplay.R
import ch.abertschi.notiplay.playback.yt.YoutubeApiWrapper
import org.jetbrains.anko.notificationManager


/**
 * Created by abertschi on 24.02.18.
 */
class YtNotificationListener : NotificationListenerService() {


    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    var youtube: YoutubeApiWrapper = YoutubeApiWrapper()

    var enabled = false
    private val CHANNEL_ID: String = "channid"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        println("ON NOTIFICATION RECEIVED")
        println("status" + sbn.groupKey)

        if (sbn.groupKey.toString().contains("com.android.chrome")) {
            val extras = sbn.notification.extras
            var playbackTile = extras.get("android.title")
            var playbackUsername = extras.get("android.text")

            for(key: String in sbn.notification.extras.keySet()) {
                println(key + " -> " + sbn.notification.extras.get(key))
            }

            try {
                val token = sbn.notification.extras.get("android.mediaSession") as MediaSession.Token
                val controller = MediaController(this, token)
                val transportControls = controller!!.transportControls
                transportControls?.pause()
                println("playing")
                println(token.toString())
            } catch (e: Exception) {
                println(e)
            }


        }




    }

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
        println("ON NOTIFICATION REMOVED")
        if (sbn.key.contains("chrome") && enabled == true) {
            enabled = false
//            YtAccessibilityService.that?.active = true
        }
        // Implement what you want here
    }
}