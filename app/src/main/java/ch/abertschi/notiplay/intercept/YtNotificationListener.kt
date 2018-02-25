package ch.abertschi.notiplay.intercept

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import ch.abertschi.notiplay.R
import org.jetbrains.anko.notificationManager


/**
 * Created by abertschi on 24.02.18.
 */
class YtNotificationListener : NotificationListenerService() {


    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    var enabled = false
    private val CHANNEL_ID: String = "channid"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        println("ON NOTIFICATION RECEIVED")
        println("status" + sbn.groupKey)
        // Implement what you want here

        if (sbn.key.contains("chrome") && enabled == false) {
            enabled = true
            YtAccessibilityService.that?.active = true
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

                .setPriority(NotificationCompat.PRIORITY_MAX) //must give priority to High, Max which will considered as heads-up notification
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "Launch", null)
                .addAction(R.drawable.common_google_signin_btn_icon_dark,
                        "Always Launch", null)

        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//to post your notification to the notification bar with a id. If a notification with same id already exists, it will get replaced with updated information.
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