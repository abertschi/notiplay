package ch.abertschi.notiplay.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.RemoteException
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import ch.abertschi.notiplay.R
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


/**
 * Created by abertschi on 10.02.18.
 */
class PlaybackNotificationManager(val service: PlaybackService) : BroadcastReceiver(), AnkoLogger {

    companion object {
        val ACTION_PLAY = "action_play"
        val ACTION_PAUSE = "action_pause"
        val ACTION_NEXT = "action_next"
        val ACTION_PREVIOUS = "action_previous"
        val ACTION_STOP = "action_stop"
        val ACTION_SHOW_IN_SOURCE_APP = "show_in_src_app"
        val ACTION_SHOW_VIDEO_PLAYER = "show_video_player"
        val REQUEST_CODE = 77
        private val NOTIFICATION_ID = 1
        private var CHANNEL_ID = "media_playback_channel"
    }

    val pauseIntent: PendingIntent
    val playIntent: PendingIntent
    val previousIntent: PendingIntent
    val nextIntent: PendingIntent
    val stopIntent: PendingIntent
    val showInSourceAppIntent: PendingIntent
    val showVideoPlayerIntent: PendingIntent

    val notificationManager: NotificationManager =
            service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    var transportControls: MediaControllerCompat.TransportControls? = null

    var started: Boolean = false

    private var sessionToken: MediaSessionCompat.Token? = null

    private var controller: MediaControllerCompat? = null

    private var mediaControllerCallback: MediaControllerCompat.Callback =
            object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    super.onPlaybackStateChanged(state)
                }

                override fun onSessionDestroyed() {
                    super.onSessionDestroyed()
                }

            }

    init {
        pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_PAUSE), PendingIntent.FLAG_CANCEL_CURRENT)
        playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_PLAY), PendingIntent.FLAG_CANCEL_CURRENT)
        previousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_PREVIOUS), PendingIntent.FLAG_CANCEL_CURRENT)
        nextIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_NEXT), PendingIntent.FLAG_CANCEL_CURRENT)
        stopIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_STOP), PendingIntent.FLAG_CANCEL_CURRENT)
        showInSourceAppIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_SHOW_IN_SOURCE_APP), PendingIntent.FLAG_CANCEL_CURRENT)
        showVideoPlayerIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
                Intent(ACTION_SHOW_VIDEO_PLAYER), PendingIntent.FLAG_CANCEL_CURRENT)

        updateSessionToken()
        notificationManager.cancelAll()
    }

    fun startNotifications() {
        if (started) return

        val n = createNotification()
        val filter = IntentFilter()
        controller!!.registerCallback(mediaControllerCallback)
        filter.addAction(ACTION_PAUSE)
        filter.addAction(ACTION_NEXT)
        filter.addAction(ACTION_PLAY)
        filter.addAction(ACTION_PREVIOUS)
        filter.addAction(ACTION_SHOW_IN_SOURCE_APP)
        filter.addAction(ACTION_STOP)
        filter.addAction(ACTION_SHOW_VIDEO_PLAYER)
        service.registerReceiver(this, filter)

        started = true
        service.startForeground(NOTIFICATION_ID, n)
    }

    fun stopNotifications() {
        if (!started) return
        started = false
        controller?.unregisterCallback(mediaControllerCallback)
        notificationManager.cancel(NOTIFICATION_ID)
        service.unregisterReceiver(this)
        service.stopForeground(true)
    }

    override fun onReceive(contet: Context?, intent: Intent?) {
        intent?.run {
            val action = intent.getAction()
            info("Received intent with action " + action)
            when (action) {
                ACTION_PAUSE -> {
                    transportControls?.pause()
                }
                ACTION_PLAY -> {
                    transportControls?.play()
                }
                ACTION_NEXT -> {
                    transportControls?.skipToNext()
                }
                ACTION_STOP -> {
                    transportControls?.stop()
                }
                ACTION_PREVIOUS -> transportControls?.skipToPrevious()

                ACTION_SHOW_VIDEO_PLAYER -> {

                }
                ACTION_SHOW_IN_SOURCE_APP -> {

                }
                else -> {
                    error("No intent found with action: " + action)
                }
            }
        }
    }

    @Throws(RemoteException::class)
    private fun updateSessionToken() {
        sessionToken = service.getSessionToken()
        if (sessionToken != null) {
            controller = MediaControllerCompat(service, sessionToken!!)
            transportControls = controller!!.transportControls
            if (started) {
                controller!!.registerCallback(mediaControllerCallback)
            }
        }
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val b = NotificationCompat.Builder(service, CHANNEL_ID)
        addActions(b)
        b.setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1, 2, 3)
                .setShowCancelButton(true)
                .setCancelButtonIntent(stopIntent)
                .setMediaSession(sessionToken))
                .setDeleteIntent(stopIntent)
//                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.abc_action_bar_item_background_material)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
//                .setContentIntent(createContentIntent(description))
                .setContentTitle("title")//description.getTitle())
                .setContentText("text")
//                .setLargeIcon(art)


        return b.build()
    }

    private fun generateAction(icon: Int, title: String, intent: PendingIntent): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(icon, title, intent).build()
    }

    private fun addActions(builder: NotificationCompat.Builder) {
        builder.addAction(generateAction(R.mipmap.ic_skip_previous_white_48dp, "Video Player", showVideoPlayerIntent))
        builder.addAction(generateAction(R.mipmap.ic_skip_previous_white_48dp, "Previous", previousIntent))
        builder.addAction(generateAction(R.mipmap.ic_skip_previous_white_48dp, "Play", playIntent))
//        builder.addAction(playAction)
        builder.addAction(generateAction(R.mipmap.ic_skip_next_white_48dp, "Next", nextIntent))
        builder.addAction(generateAction(R.mipmap.ic_skip_previous_white_48dp, "Open Source", showInSourceAppIntent))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(CHANNEL_ID,
                    "playback controls",
                    NotificationManager.IMPORTANCE_LOW)
            notificationChannel.description = "notification option for playback control"
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}