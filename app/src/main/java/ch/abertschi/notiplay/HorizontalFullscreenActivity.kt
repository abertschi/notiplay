package ch.abertschi.notiplay

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import ch.abertschi.notiplay.NotiRunner.Companion.ACTION_QUIT_FULLSCREEN

/**
 * Created by abertschi on 04.02.18.
 */
class HorizontalFullscreenActivity: Activity() {

    companion object {
        val ACTION_QUIT_ACTIVITY = "action_quit_horizontal_fullscreen_activity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("oncreate")
        quitByIntent = false
        setContentView(R.layout.activity_fullscreen)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    }

    fun quitFullScreen() {
        if (!quitByIntent) {
            val intent = Intent(applicationContext, NotiRunner::class.java)
            intent.action = ACTION_QUIT_FULLSCREEN
            this.startService(intent)
        }
        this.finish()
    }

    private var quitByIntent = false

    override fun onNewIntent(intent: Intent) {
        val extras = intent.extras
        if (intent.action.equals(ACTION_QUIT_ACTIVITY)) {
            quitByIntent = true
            this.finish()
        }
    }

    override fun onResume() {
        super.onResume()
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    }

    override fun onBackPressed() {
        super.onBackPressed()
        println("back pressed")
        quitFullScreen()
    }

    override fun onPause() {
        super.onPause()
        println("on pause")
        quitFullScreen()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // ignore orientation/keyboard change
        super.onConfigurationChanged(newConfig)
    }
}