package ch.abertschi.notiplay.intercept

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification


/**
 * Created by abertschi on 24.02.18.
 */
class BrowserNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.groupKey.toString().contains("com.android.chrome")) {

            val extras: Bundle? = sbn.notification.extras
            var playbackTile = extras?.get("android.title") as String?
            var key = playbackTile
            if (extras == null) return
            //sbn.
            for(e in extras!!.keySet()) {
                println(e.toString())
                println(extras.get(e))
            }
            if (key == null) return
            try {
                println(key)
                val actions = sbn.notification.actions
                var playPauseAction: Notification.Action?
                if (actions.size == 3) {
                    playPauseAction = sbn.notification.actions[1]
                } else if (actions.size == 5) {
                    playPauseAction = sbn.notification.actions[2]
                } else {
                    throw Exception("what does the fox say? wrong number of notification actions")
                }
                if (playPauseAction.title == "Pause") {
                    BrowserState.GET.onPlaybackStart(key, this)
                    BrowserState.GET.updateVideoTitle(playbackTile, this)
                } else if (playPauseAction.title == "Play") {
                    BrowserState.GET.updateVideoTitle(playbackTile, this)
                    BrowserState.GET.onPlaybackPause(key, this)
                }
            } catch (e: Exception) {
            }
        }
    }

//    override fun onNotificationRemoved(sbn: StatusBarNotification) {
//        if (sbn.key.contains("chrome")) {
//            val extras = sbn.notification.extras
//            var playbackTile = extras.get("android.title") as String
//            var key = playbackTile
//            try {
//                val playPauseAction = sbn.notification.actions[1]
//                } else if (playPauseAction.title == "Play") {
//            } catch (e: Exception) {
//            }
//        }
//    }
}