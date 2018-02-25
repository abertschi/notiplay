package ch.abertschi.notiplay.intercept

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification



/**
 * Created by abertschi on 24.02.18.
 */
class YtNotificationListener: NotificationListenerService() {


    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
//        println("ON NOTIFICATION RECEIVED")
//        println("status" + sbn.groupKey)
        // Implement what you want here
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        println("ON NOTIFICATION REMOVED")
        // Implement what you want here
    }
}