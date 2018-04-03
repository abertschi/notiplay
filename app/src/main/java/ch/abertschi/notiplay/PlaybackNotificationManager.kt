package ch.abertschi.notiplay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Build
import android.os.RemoteException
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.Html
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

    private enum class PlayPauseAction {
        PLAY, PAUSE, BUFFER
    }

    private var noConnectivity = false
    private var currentPlayPauseAction: PlayPauseAction = PlayPauseAction.PLAY
    private var currentMetadata: MediaMetadataCompat? = null
    private var currentPlaybackState: PlaybackStateCompat? = null


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
    var showPersistentNotification = true

    private var sessionToken: MediaSessionCompat.Token? = null

    private var controller: MediaControllerCompat? = null

    // set #showPersistentNotification to true to start notification on going
    private fun castNewNotification() {
        if (!started) return
        createNotification()?.run {
            if (showPersistentNotification) {
                service.startForeground(NOTIFICATION_ID, this)
            } else {
                 notificationManager?.notify(NOTIFICATION_ID, this)
                service.stopForeground(false)

            }
        }

    }

    private var mediaControllerCallback: MediaControllerCompat.Callback =
            object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    super.onPlaybackStateChanged(state)
                    currentPlaybackState = state

                    state?.run {
                        when (this.state) {
                            PlaybackStateCompat.STATE_PAUSED -> {
                                currentPlayPauseAction = PlayPauseAction.PLAY
                            }
                            PlaybackStateCompat.STATE_PLAYING -> {
                                currentPlayPauseAction = PlayPauseAction.PAUSE
                            }
                            PlaybackStateCompat.STATE_BUFFERING -> {
                                currentPlayPauseAction = PlayPauseAction.BUFFER
                            }
                        }
                        castNewNotification()
                    }
                }

                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                    super.onMetadataChanged(metadata)
                    currentMetadata = metadata
                    castNewNotification()
                }

                override fun onSessionDestroyed() {
                    super.onSessionDestroyed()
                    updateSessionToken()
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
        showPersistentNotification = true
        castNewNotification()
    }

    fun stopNotifications() {
        showPersistentNotification = false
        if (!started) return
        started = false
        showPersistentNotification = false
        controller?.unregisterCallback(mediaControllerCallback)
        notificationManager.cancel(NOTIFICATION_ID)
        service.unregisterReceiver(this)
        service.stopForeground(true)
    }

    override fun onReceive(contet: Context?, intent: Intent?) {
        intent?.run {
            val action = intent.getAction()
            info("Received intent with action " + action)

            var showNotification = true
            when (action) {
                ACTION_PAUSE -> {
                    transportControls?.pause()
                    currentPlayPauseAction = PlayPauseAction.PLAY
//                    service.stopForeground(true)
                    showPersistentNotification = false
                }
                ACTION_PLAY -> {
                    showPersistentNotification = true
                    transportControls?.play()
                    currentPlayPauseAction = PlayPauseAction.PAUSE
                }
                ACTION_NEXT -> {
                    transportControls?.skipToNext()
                }
                ACTION_STOP -> {
                    transportControls?.stop()
                    showPersistentNotification = false
                    stopNotifications()
                    service.shutdownService()
                }
                ACTION_PREVIOUS -> transportControls?.skipToPrevious()

                ACTION_SHOW_VIDEO_PLAYER -> {
                    showPersistentNotification = false
                    transportControls?.sendCustomAction(ACTION_SHOW_VIDEO_PLAYER, null)
                }
                ACTION_SHOW_IN_SOURCE_APP -> {
                    showPersistentNotification = false
                    transportControls?.sendCustomAction(ACTION_SHOW_IN_SOURCE_APP, null)

                }
                else -> {
                    error("No intent found with action: " + action)
                }
            }
            castNewNotification()
        }
    }

    @Throws(RemoteException::class)
    private fun updateSessionToken() {
        val newToken = service.getSessionToken()

        if (sessionToken == null && newToken != null || // first run
                sessionToken != null && sessionToken!! != newToken) { // token changed

            controller?.run { this.unregisterCallback(mediaControllerCallback) }
            sessionToken = newToken
            sessionToken?.run {
                if (sessionToken != null) {
                    controller = MediaControllerCompat(service, sessionToken!!)
                    transportControls = controller!!.transportControls
                    if (started) {
                        controller!!.registerCallback(mediaControllerCallback)
                    }
                }
            }
        }
    }


    private fun createNotification(): Notification? {
//        if (currentMetadata != null || currentPlayPauseAction == null) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val b = NotificationCompat.Builder(service, CHANNEL_ID)
        b.setDefaults(Notification.DEFAULT_ALL)
        addActions(b)

        val title = currentMetadata?.description?.title?.toString() ?: "loading title ..."
        b.setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1, 2, 3)
                .setShowCancelButton(true)
                .setCancelButtonIntent(stopIntent)
                .setMediaSession(sessionToken))
                .setDeleteIntent(stopIntent)
                .setShowWhen(false)
                .setNumber(3)
//                .setColor(mNotificationColor)
                .setPriority(100)
                .setOngoing(false)
                .setAutoCancel(false)

                .setSubText(getPlaybackStateText())
//                .setTicker("ticker")

                .setSmallIcon(R.drawable.abc_action_bar_item_background_material)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
//                .setContentIntent(createContentIntent())
                .setContentTitle(Html
                        .fromHtml("<b>$title</b>"))
//                .setContentText(currentMetadata?.description?.subtitle ?: "subtitle")


        val metaArtBitmap = currentMetadata?.getBitmap(METADATA_KEY_ALBUM_ART)
        if (metaArtBitmap == null) {
            b.setLargeIcon(BitmapFactory.decodeResource(service.resources,
                    R.mipmap.ic_launcher))
        } else {
            b.setLargeIcon(metaArtBitmap)
        }
        val n = b.build()
        return n
    }

    private fun createContentIntent(): PendingIntent? {
        return null
    }

    fun showMessageNoConnectivity(status: Boolean) {
        this.noConnectivity = status
    }

    private fun generateAction(icon: Int, title: String, intent: PendingIntent): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(icon, title, intent).build()
    }

    private fun addActions(builder: NotificationCompat.Builder) {
        builder.addAction(generateAction(R.mipmap.ic_picture_in_picture_black_18dp,
                "Video Player", showVideoPlayerIntent))
        builder.addAction(generateAction(R.mipmap.ic_skip_previous_black_36dp, "Previous", previousIntent))
        if (currentPlayPauseAction == PlayPauseAction.PLAY
                || currentPlayPauseAction == PlayPauseAction.BUFFER) {
            builder.addAction(generateAction(R.mipmap.ic_play_arrow_black_36dp, "Play", playIntent))
        } else {
            builder.addAction(generateAction(R.mipmap.ic_pause_black_36dp, "Pause", pauseIntent))
        }
        builder.addAction(generateAction(R.mipmap.ic_skip_next_black_36dp, "Next", nextIntent))
        builder.addAction(generateAction(R.mipmap.ic_subscriptions_black_18dp, "Open Source", showInSourceAppIntent))
    }

    private fun getPlaybackStateText(): String {
        if (noConnectivity) return "no connection"
        if (currentPlaybackState == null) return "loading"
        return when (currentPlaybackState!!.state) {
            PlaybackStateCompat.STATE_PLAYING -> "playing"
            PlaybackStateCompat.STATE_PAUSED -> "paused"
            PlaybackStateCompat.STATE_BUFFERING -> "buffering"
            PlaybackStateCompat.STATE_CONNECTING -> "connecting"
            else -> "buffering"
        }
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