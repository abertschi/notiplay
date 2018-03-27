package ch.abertschi.notiplay.intercept

import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.wtf


/**
 * Created by abertschi on 24.02.18.
 */
class BrowserNotificationListener : NotificationListenerService(), AnkoLogger {

    private fun isChrome(str: String) = str.contains("com.android.chrome")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            isChrome(sbn.groupKey.toString()).run {
                val extras: Bundle = sbn.notification.extras ?: throw Exception("bundle is null")
                var key = extras.get("android.title")?.toString()
                        ?: throw Exception("no android.title")

                val actions = sbn.notification.actions

                var playPauseAction = when {
                    actions.size == 3 -> actions[1]
                    actions.size == 5 -> actions[2]
                    else -> throw Exception("unknown action size")
                }

                BrowserState.GET.onPlaybackStart(key, this@BrowserNotificationListener)
                BrowserState.GET.updateVideoTitle(key, this@BrowserNotificationListener)

                // cant do this: language dependent!
//                if (playPauseAction.title == "Pause") {
//                    BrowserState.GET.onPlaybackStart(key, this@BrowserNotificationListener)
//                    BrowserState.GET.updateVideoTitle(key, this@BrowserNotificationListener)
//
//                } else if (playPauseAction.title == "Play") {
//                    BrowserState.GET.updateVideoTitle(key, this@BrowserNotificationListener)
//                    BrowserState.GET.onPlaybackPause(key, this@BrowserNotificationListener)
//                }

            }
        } catch (e: Exception) {
            wtf("Browser notfication listener failed", e)
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