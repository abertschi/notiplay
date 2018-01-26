package ch.abertschi.notiplay

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.net.Uri
import android.os.IBinder
import android.util.Log
import java.util.*
import kotlin.concurrent.timerTask


/**
 * Created by abertschi on 07.01.18.
 * api doc: https://developers.google.com/youtube/iframe_api_reference#seekTo
 * use media session and intent api
 * http://www.binpress.com/tutorial/using-android-media-style-notifications-with-media-session-controls/165
 * https://stackoverflow.com/questions/11095122/how-to-make-my-android-app-appear-in-the-share-list-of-another-specific-app
 */

class NotiRunner : Service(), NotiObserver {


    private var drawer: WebViewDrawer? = null
    private var videoId: String? = null
    private var controller: MediaController? = null
    private var session: MediaSession? = null
    private var wantsPlaybackPosition = false
    private var notificationBuilder: Notification.Builder? = null

    companion object {
        val INTENT_VIDEO_ID: String = "video_id"
        val ACTION_OPEN_IN_BROWSER = "action_open_in_browser"
        val ACTION_PLAY = "action_play"
        val ACTION_PAUSE = "action_pause"
        val ACTION_REWIND = "action_rewind"
        val ACTION_FAST_FORWARD = "action_fast_foward"
        val ACTION_NEXT = "action_next"
        val ACTION_PREVIOUS = "action_previous"
        val ACTION_STOP = "action_stop"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoId = intent?.getStringExtra(INTENT_VIDEO_ID)
        println("videoId from service: " + videoId)
        println("intent: " + intent?.action)
        handleIntent(intent)


        videoId?.run {
            this@NotiRunner.videoId = videoId
            if (drawer == null) {
                drawer = WebViewDrawer(this@NotiRunner)
                drawer?.addEventObserver(this@NotiRunner)
                drawer?.setOnCloseCallback {
                    stopSelf()
                }
                drawer?.loadWebView()

            }
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE))

            val timer = Timer()
            timer.schedule(timerTask { drawer?.playVideoById(videoId!!) }, 3000)
            initMediaSession()

        }

        if (intent == null) {
            return super.onStartCommand(Intent(), flags, startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.action == null)
            return

        val action = intent.action

        if (action.equals(ACTION_PLAY, ignoreCase = true)) {
            controller?.transportControls?.play()
        } else if (action.equals(ACTION_PAUSE, ignoreCase = true)) {
            controller?.transportControls?.pause()
        } else if (action.equals(ACTION_FAST_FORWARD, ignoreCase = true)) {
            controller?.transportControls?.fastForward()
        } else if (action.equals(ACTION_REWIND, ignoreCase = true)) {
            controller?.transportControls?.rewind()
        } else if (action.equals(ACTION_PREVIOUS, ignoreCase = true)) {
            controller?.transportControls?.skipToPrevious()
        } else if (action.equals(ACTION_NEXT, ignoreCase = true)) {
            controller?.transportControls?.skipToNext()
        } else if (action.equals(ACTION_STOP, ignoreCase = true)) {
            controller?.transportControls?.stop()
        } else if (action.equals(ACTION_OPEN_IN_BROWSER, ignoreCase = true)) {
            wantsPlaybackPosition = true
            drawer?.getPlaybackPosition()
        }
    }

    override fun onPlaybackPosition(seconds: Int) {
        if (wantsPlaybackPosition) {
            wantsPlaybackPosition = false
            var id = if (videoId != null) videoId else ""
            val i = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://youtube.com/watch?v=${id}&t=${seconds}"))
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i)
        }
    }


    private fun buildNotification(action: Notification.Action) {
        if (videoId == null) return

        val style = Notification.MediaStyle().setMediaSession(session?.sessionToken)

        val browserIntent = Intent(applicationContext, NotiRunner::class.java)
        browserIntent.action = ACTION_OPEN_IN_BROWSER
        val contentIntent = PendingIntent.getService(applicationContext, 1, browserIntent, 0)

        val cancelIntent = Intent(applicationContext, NotiRunner::class.java)
        browserIntent.action = ACTION_STOP
        val cancelPendingIntent = PendingIntent.getService(applicationContext, 1, cancelIntent, 0)

//        val cancelIntent = PendingIntent.getService(applicationContext, 1, intent, 0)
        val builder = Notification.Builder(this)
                .setSmallIcon(R.drawable.abc_ic_clear_material)
                .setContentTitle(videoId)
                .setContentText("")
                .setDeleteIntent(cancelPendingIntent)
                .setStyle(style)
                .setOngoing(true)
                .setContentIntent(contentIntent)

        // pending implicit intent to view url


//        val pending = PendingIntent.getActivity(this, 0, resultIntent, )
//        builder.setContentIntent(pending)


        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS))
        builder.addAction(generateAction(android.R.drawable.ic_media_rew, "Rewind", ACTION_REWIND))
        builder.addAction(action)
        builder.addAction(generateAction(android.R.drawable.ic_media_ff, "Fast Foward", ACTION_FAST_FORWARD))
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))
        style.setShowActionsInCompactView(2)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.notify(1, builder.build())
        startForeground(1, builder.build())
    }

    fun getNotiRunnable(): NotiRunnable? = this.drawer


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStart(intent: Intent, startId: Int) {
        super.onStart(intent, startId)
        Log.i(TAG, "onStart")
    }

    private fun generateAction(icon: Int, title: String, intentAction: String): Notification.Action {
        val intent = Intent(applicationContext, NotiRunner::class.java)
        intent.action = intentAction
        val pendingIntent = PendingIntent.getService(applicationContext, 1, intent, 0)
        return Notification.Action.Builder(icon, title, pendingIntent).build()
    }

    fun initMediaSession() {
        this.session = MediaSession(applicationContext, "NotiPlay Session")
        this.controller = MediaController(applicationContext, session?.sessionToken)

        session?.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                super.onPlay()
                drawer?.playerPlay()
                Log.e("MediaPlayerService", "onPlay")
                buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE))
            }

            override fun onPause() {
                super.onPause()
                drawer?.playerPause()
                Log.e("MediaPlayerService", "onPause")
                buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY))
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                Log.e("MediaPlayerService", "onSkipToNext")
                //Change media here
                buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE))
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                Log.e("MediaPlayerService", "onSkipToPrevious")
                drawer?.seekToPosition(0)
                //Change media here
                buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE))
            }

            override fun onFastForward() {
                super.onFastForward()
                drawer?.seekForward()
                Log.e("MediaPlayerService", "onFastForward")
                //Manipulate current media here
            }

            override fun onRewind() {
                super.onRewind()
                drawer?.seekBackward()
                Log.e("MediaPlayerService", "onRewind")
                //Manipulate current media here
            }

            override fun onStop() {
                super.onStop()
                drawer?.playerStop()
                Log.e("MediaPlayerService", "onStop")
                //Stop media player here
            }

        })
    }

    override fun onPlayerReady() {
        println("ready")
    }

    override fun onPlayerStateChange(state: NotiObserver.PlayerState) {
        println(state)

//        when(state) {
////            NotiObserver.PlayerState.PLAYING -> buildNotification(generateAction(android.R.drawable.ic_media_play, "Pause", ACTION_PAUSE))
//
//        }
    }

    override fun onPlaybackQualityChange(quality: String) {
        println(quality)
    }

    override fun onPlaybackRateChange(rate: Int) {
        println(rate)

    }

    override fun onErrorCode(code: NotiObserver.ErrorCode) {
        println("error: " + code)
    }
}
