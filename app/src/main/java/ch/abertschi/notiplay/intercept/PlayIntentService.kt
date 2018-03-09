package ch.abertschi.notiplay.intercept

import android.app.IntentService
import android.content.Intent
import android.util.Log
import ch.abertschi.notiplay.PlaybackService


/**
 * Created by abertschi on 09.03.18.
 */
class PlayIntentService : IntentService("PlayIntentService") {
    override fun onHandleIntent(intent: Intent?) {
        Log.i("myapp", "I got this awesome intent and will now do stuff in the background!")
        // .... do what you like

        val notiIntent = Intent(this, PlaybackService::class.java)
        notiIntent.action = PlaybackService.ACTION_INIT_WITH_ID
        notiIntent.putExtra(PlaybackService.EXTRA_SEEK_POS,
                intent?.getLongExtra(PlaybackService.EXTRA_SEEK_POS, 0))
        notiIntent.putExtra(intent?.getStringExtra(PlaybackService.EXTRA_PLAYBACK_STATE), "play")
    }

    private fun launch() {

    }
}