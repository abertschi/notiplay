package ch.abertschi.notiplay.intercept

import android.app.Notification
import android.media.session.MediaController
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import ch.abertschi.notiplay.playback.yt.YoutubeApiWrapper


/**
 * Created by abertschi on 24.02.18.
 */
class YtNotificationListener2 : NotificationListenerService() {

    var youtube: YoutubeApiWrapper = YoutubeApiWrapper()

    private val CHANNEL_ID: String = "channid"

    private var transportControls: MediaController.TransportControls? = null
    var controller: MediaController? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.groupKey.toString().contains("com.android.chrome")) {

            val extras = sbn.notification.extras
            var playbackTile = extras.get("android.title") as String
            var key = playbackTile
            try {
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
                    YtAccessibilityService?.INSTANCE?.performSwypeUpIfInValidApp()
                } else if (playPauseAction.title == "Play") {
                    BrowserState.GET.onPlaybackPause(key, this)
                }
            } catch (e: Exception) {
            }
        }
        return

//        try {
//            token = sbn.notification.extras.get("android.mediaSession") as MediaSession.Token
//            controller = MediaController(this, token)
//            transportControls = controller!!.transportControls
//            controller?.registerCallback(object : MediaController.Callback() {
//                override fun onMetadataChanged(metadata: MediaMetadata?) {
//                    super.onMetadataChanged(metadata)
//                    println(metadata?.description)
//                    println(" super.onMetadataChanged(metadata)")
//                }
//
//                override fun onSessionEvent(event: String?, extras: Bundle?) {
//                    super.onSessionEvent(event, extras)
//                    println(" super.onSessionEvent(metadata)")
//                }
//
//                override fun onPlaybackStateChanged(state: PlaybackState?) {
//                    super.onPlaybackStateChanged(state)
//                    println((state?.position))
//                    println(" /// " + "POSE")
//                    println(" super.onPlaybackStateChanged(metadata)")
//
//                }
//
//                override fun onQueueTitleChanged(title: CharSequence?) {
//                    super.onQueueTitleChanged(title)
//                    println("println(\" super.onQueueTitleChanged(metadata)\")")
//                }
//
//                override fun onSessionDestroyed() {
//                    super.onSessionDestroyed()
//                    println(" super.onSessionDestroyed(metadata)")
//                }
//            })
//            val pos = controller?.playbackState?.bufferedPosition
//            println("BPOS:::::: " + pos)
//            val pos2 = controller?.playbackState?.position
//            println("POS:::::: " + pos2)
//            println(token.toString())
//        } catch (e: Exception) {
//            println(e)
//        }

    }


    override fun onNotificationRemoved(sbn: StatusBarNotification) {


//        println("ON NOTIFICATION REMOVED")

        if (sbn.key.contains("chrome")) {
            val extras = sbn.notification.extras
            var playbackTile = extras.get("android.title") as String
            var key = playbackTile
            try {
                val playPauseAction = sbn.notification.actions[1]
                println("PLAYER KEY: " + playPauseAction.title)
//                if (playPauseAction.title == "Pause") {
//                    BrowserState.GET.onPlaybackStart(key, this)
//                } else if (playPauseAction.title == "Play") {
            } catch (e: Exception) {
            }
        }
//            val controller = MediaController(this, token)

//            var transportControls = controller!!.transportControls
//
//            transportControls?.play()
//            println(controller?.queueTitle)
//            println("playing again? ")
//            println(transportControls)
//        }

//        if (sbn.key.contains("chrome") && enabled == true) {
//
////            YtAccessibilityService.that?.active = true
//        }
        // Implement what you want here
    }
}