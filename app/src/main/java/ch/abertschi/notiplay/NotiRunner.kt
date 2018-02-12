package ch.abertschi.notiplay


import android.app.*
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
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

    private var drawer: YoutubePlayer? = null
    private var videoId: String? = null
    private var controller: MediaControllerCompat? = null
    private var session: MediaSessionCompat? = null
    private var wantsPlaybackPosition = false
    private var notificationId: Int = 1
    private var hasError: Boolean = false
    private var notificationStyle: android.support.v4.media.app.NotificationCompat.MediaStyle? = null
    private var channelId = "media_playback_channel"

    private var videoTitle: String = ""
    private var playbackState: NotiObserver.PlayerState = NotiObserver.PlayerState.UNSTARTED

    lateinit var notificationManager: NotificationManager

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
        val ACTION_CONFIRM_FULLSCREEN = "action_confirm_fullscreen"
        val ACTION_REQUEST_FLOATING_WINDOW = "action_request_floating_window"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoId = intent?.getStringExtra(INTENT_VIDEO_ID)
        println("videoId from service: " + videoId)
        println("intent: " + intent?.action)
        handleIntent(intent)

        notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }


        videoId?.run {
            this@NotiRunner.videoId = videoId
            this@NotiRunner.videoTitle = "video " + videoId

            if (drawer == null) {
                drawer = YoutubePlayer(this@NotiRunner)
                drawer?.addEventObserver(this@NotiRunner)
                drawer?.setOnCloseCallback {
                    stopSelf()
                }
                hasError = false
                notificationStyle = null
                notificationBuilder = null
                Toast.makeText(baseContext, "Loading Youtube video ...", Toast.LENGTH_LONG).show()
                drawer?.startWebView()

            }
            val timer = Timer()
            timer.schedule(timerTask {
                drawer?.playVideoById(videoId!!)
                buildNotification(generateAction(R.mipmap.ic_play_arrow_white_48dp, "Play", ACTION_PLAY),
                        "loading ...")

                drawer?.getVideoData()

            }, 3000)
            initMediaSession()

        }

        if (intent == null) {
            controller?.transportControls?.stop()
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
        println(action)

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
        } else if (action.equals(ACTION_REQUEST_FLOATING_WINDOW, ignoreCase = true)) {
            drawer?.requestFloatingWindow()
            drawer?.confirmFloatingWindow()
        } else if (action.equals(ACTION_CONFIRM_FULLSCREEN, ignoreCase = true)) {
            drawer?.confirmFullscreen()
        }
    }

    override fun onPlaybackPosition(seconds: Int) {
        if (wantsPlaybackPosition) {
            wantsPlaybackPosition = false
            var id = if (videoId != null) videoId else ""
            val i = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://youtube.com/watch?v=${id}&t=${seconds}"))
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            controller?.transportControls?.pause()

            startActivity(i)
        }
    }

    private var notificationBuilder: NotificationCompat.Builder? = null

    private fun getNotificationBuilder(): NotificationCompat.Builder {
        val browserIntent = Intent(applicationContext, NotiRunner::class.java)
        browserIntent.action = ACTION_OPEN_IN_BROWSER
        val contentIntent = PendingIntent.getService(applicationContext, 1, browserIntent, 0)

        val cancelIntent = Intent(applicationContext, NotiRunner::class.java)
        browserIntent.action = ACTION_STOP
        val cancelPendingIntent = PendingIntent.getService(applicationContext, 1, cancelIntent, 0)

        val style = android.support.v4.media.app.NotificationCompat.MediaStyle()
        style.setMediaSession(session?.sessionToken)
        style.setShowActionsInCompactView(2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(cancelPendingIntent)

        this.notificationStyle = style

        return NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.abc_ic_clear_material)
                .setStyle(style)
                .setContentTitle(this.videoTitle)
                .setContentText("")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDeleteIntent(cancelPendingIntent)
                .setContentIntent(contentIntent)
//                .setLargeIcon(Bitmap.createBitmap()


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val id = channelId

        // The user-visible name of the channel.
        val name = "Media playback"
        val description = "Media playback controls"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(id, name, importance)

        channel.description = description
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(action: NotificationCompat.Action, subTitle: String = "") {
        if (videoId == null) return

        this.notificationBuilder = getNotificationBuilder()

        notificationBuilder?.addAction(generateAction(R.mipmap.ic_skip_previous_white_48dp, "Previous", ACTION_PREVIOUS))
        notificationBuilder?.addAction(generateAction(R.mipmap.ic_fast_rewind_white_48dp, "Rewind", ACTION_REWIND))
        notificationBuilder?.addAction(action)
        notificationBuilder?.addAction(generateAction(R.mipmap.ic_fast_forward_white_48dp, "Fast Foward", ACTION_FAST_FORWARD))
        notificationBuilder?.addAction(generateAction(R.mipmap.ic_skip_next_white_48dp, "Next", ACTION_NEXT))

        startForeground(notificationId, notificationBuilder?.build())
    }

    fun getNotiRunnable(): NotiRunnable? = this.drawer


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStart(intent: Intent, startId: Int) {
        super.onStart(intent, startId)
        Log.i(TAG, "onStart")
    }

    private fun generateAction(icon: Int, title: String, intentAction: String): NotificationCompat.Action {
        val intent = Intent(applicationContext, NotiRunner::class.java)
        intent.action = intentAction
        val pendingIntent = PendingIntent.getService(applicationContext, 1, intent, 0)
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    private fun initMediaSession() {
        this.session = MediaSessionCompat(applicationContext, "NotiPlay Session")
        this.controller = MediaControllerCompat(applicationContext, session?.sessionToken!!)

        session?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
//                if (hasError) return
                super.onPlay()
                drawer?.playerPlay()
                Log.e("MediaPlayerService", "onPlay")
                buildNotification(generateAction(R.mipmap.ic_pause_white_48dp, "Pause", ACTION_PAUSE))
            }

            override fun onPause() {
//                if (hasError) return
                stopForeground(false)
                super.onPause()
                drawer?.playerPause()
                Log.e("MediaPlayerService", "onPause")
                println("CAN PLAY")
                buildNotification(generateAction(R.mipmap.ic_play_arrow_white_48dp, "Play", ACTION_PLAY))
                stopForeground(false)
            }

            override fun onSkipToNext() {
//                if (hasError) return
                hasError = false
                super.onSkipToNext()
                Log.e("MediaPlayerService", "onSkipToNext")
                drawer?.playerNextVideo()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                hasError = false
                Log.e("MediaPlayerService", "onSkipToPrevious")
                drawer?.playerPreviousVideo()
            }

            override fun onFastForward() {
                super.onFastForward()
                drawer?.seekForward()
                Log.e("MediaPlayerService", "onFastForward")
            }

            override fun onRewind() {
                super.onRewind()
                drawer?.seekBackward()
                Log.e("MediaPlayerService", "onRewind")

            }

            override fun onStop() {
                super.onStop()
                drawer?.playerStop()
                Log.e("MediaPlayerService", "onStop")
            }

        })
    }

    override fun onPlayerReady() {
        println("ready")
    }

    override fun onPlayerStateChange(state: NotiObserver.PlayerState) {
        println(state)
        if (notificationBuilder == null) return

        when (state) {
            NotiObserver.PlayerState.PLAYING -> {
                buildNotification(generateAction(R.mipmap.ic_pause_white_48dp, "Pause", ACTION_PAUSE))
                setSubtitle("playing")
                playbackState = NotiObserver.PlayerState.PLAYING
            }
            NotiObserver.PlayerState.BUFFERING -> {
                setSubtitle("buffering")
                playbackState = NotiObserver.PlayerState.BUFFERING
            }
            NotiObserver.PlayerState.UNSTARTED -> {
                setSubtitle("loading")
                playbackState = NotiObserver.PlayerState.UNSTARTED
            }
            NotiObserver.PlayerState.PAUSED -> {
                setSubtitle("paused")
                playbackState = NotiObserver.PlayerState.PAUSED
                stopForeground(false)
            }
        }
    }

    override fun onPlaybackQualityChange(quality: String) {
        println(quality)
    }

    override fun onPlaybackRateChange(rate: Int) {
        println(rate)

    }

    override fun onErrorCode(code: NotiObserver.ErrorCode) {
        println("error: " + code)
        hasError = true
        notificationBuilder?.run {
            this.setContentText("error " + code.code + " - cannot play video")
//            this.mActions = ArrayList()
//            if (notificationStyle != null) {
//                notificationStyle?.setShowActionsInCompactView()
//            }
            this.setStyle(notificationStyle)
            startForeground(notificationId, this.build())
            stopForeground(false)

        }
    }

    private fun setSubtitle(msg: String) {
        if (hasError) return

        notificationBuilder?.setContentText(msg)
        startForeground(notificationId, notificationBuilder?.build())
    }

    private fun setTitle(msg: String) {
        if (hasError) return
        notificationBuilder?.setContentTitle(msg)
        startForeground(notificationId, notificationBuilder?.build())
    }

    private fun setThumbnail(url: String) {

    }

    private var updatedOnPause: Boolean = false
    override fun onPlaybackPositionUpdate(seconds: Int) {
        val hours: Int = seconds / 3600
        val mins: Int = (seconds - (hours * 60)) / 60
        val seconds: Int = seconds - mins * 60

        var time = ""
        if (hours > 0) {
            time = String.format("$02d:%02d:%02d", hours, mins, seconds)
        } else {
            time = String.format("%02d:%02d", mins, seconds)
        }

        if (playbackState == NotiObserver.PlayerState.PLAYING) {
            setSubtitle("playing " + time)
            updatedOnPause = false
        }

        if (playbackState == NotiObserver.PlayerState.PAUSED) {
            if (updatedOnPause) return
            updatedOnPause = true
            setSubtitle("paused " + time)

            stopForeground(false)
        }
    }

    override fun onVideoData(title: String, thumbailUrl: String,
                             duration: Int, loop: Boolean, videoId: String) {
        println(title + thumbailUrl)
        this.videoTitle = title

        if (notificationBuilder != null) {
            notificationBuilder?.setContentTitle(this.videoTitle)
            startForeground(notificationId, notificationBuilder?.build())
        } else {
            val timer = Timer()
            timer.schedule(timerTask {
                if (notificationBuilder != null) {
                    notificationBuilder?.setContentTitle(videoTitle)
                    startForeground(notificationId, notificationBuilder?.build())
                }
            }, 3000)
        }
    }
}
