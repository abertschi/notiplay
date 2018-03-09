package ch.abertschi.notiplay.intercept

import android.app.IntentService
import android.content.Intent
import ch.abertschi.notiplay.PlaybackService


/**
 * Created by abertschi on 09.03.18.
 */
class PlayIntentService : IntentService("PlayIntentService") {
    companion object {
        val EXTRA_HASH = "extra_hash"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent?.action == PlaybackService.ACTION_INIT_WITH_ID) {
            val notiIntent = Intent(this, PlaybackService::class.java)

            var seekPos = intent?.getLongExtra(PlaybackService.EXTRA_SEEK_POS, 0)

            val hash: String? = intent.getStringExtra(EXTRA_HASH)
            if (hash == BrowserState.GET.getCurrentStateHash(this)) {
                seekPos = BrowserState.GET.getDuration()
            }


            notiIntent.putExtra(PlaybackService.EXTRA_SHOW_UI, intent.getBooleanExtra(PlaybackService.EXTRA_SHOW_UI, true))
            notiIntent.action = PlaybackService.ACTION_INIT_WITH_ID
            notiIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID,
                    intent?.getStringExtra(PlaybackService.EXTRA_VIDEO_ID))
            notiIntent.putExtra(PlaybackService.EXTRA_SEEK_POS, seekPos)
            notiIntent.putExtra(intent?.getStringExtra(PlaybackService.EXTRA_PLAYBACK_STATE), "play")
            startService(notiIntent)
        }
    }
}