package ch.abertschi.notiplay.intercept

import android.app.IntentService
import android.content.Intent
import ch.abertschi.notiplay.PlaybackService
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


/**
 * Created by abertschi on 09.03.18.
 */
class PlayIntentService : IntentService("PlayIntentService"), AnkoLogger {
    companion object {
        val EXTRA_HASH = "extra_hash"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent?.action == PlaybackService.ACTION_INIT_WITH_ID) {
            val notiIntent = Intent(this, PlaybackService::class.java)
            notiIntent.action = PlaybackService.ACTION_INIT_WITH_ID

            var seekPos = intent?.getLongExtra(PlaybackService.EXTRA_SEEK_POS, 0)


            val hash: String? = intent.getStringExtra(EXTRA_HASH)
            info { "hash from bunde: " + hash }
            info { "hash from singleton: " + BrowserState.GET.getCurrentStateHash(this) }
            if (hash == BrowserState.GET.getCurrentStateHash(this)) {
                seekPos = BrowserState.GET.getDuration()
                info { "--- updating seekpos: " + seekPos }
            }


            BrowserState.GET.cleanNotifications(this)

            notiIntent.putExtra(PlaybackService.EXTRA_SHOW_UI, intent.getBooleanExtra(PlaybackService.EXTRA_SHOW_UI, true))

            notiIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID,
                    intent?.getStringExtra(PlaybackService.EXTRA_VIDEO_ID))

            notiIntent.putExtra(PlaybackService.EXTRA_SEEK_POS, seekPos)
            notiIntent.putExtra(intent?.getStringExtra(PlaybackService.EXTRA_PLAYBACK_STATE), "play")
            startService(notiIntent)
        }
    }
}