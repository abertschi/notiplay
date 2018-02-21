package ch.abertschi.notiplay

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle

/**
 * Created by abertschi on 04.02.18.
 */
class HorizontalFullscreenActivity : Activity() {

    companion object {
        val ACTION_REQUEST_FULLSCREEN = "action_request_fullscreen"
        val ACTION_QUIT_FULLSCREEN = "action_quit_fullscreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("oncreate")
        setContentView(R.layout.activity_fullscreen)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntent(intent)
    }

    fun confirmFullScreen() {
        val intent = Intent(applicationContext, NotiRunner::class.java)
        intent.action = NotiRunner.ACTION_CONFIRM_FULLSCREEN
        println("CONFIRM FULLSCREEN")
        this.startService(intent)
    }

    fun requestFloatingWindow() {
        val intent = Intent(applicationContext, NotiRunner::class.java)
        intent.action = NotiRunner.ACTION_REQUEST_FLOATING_WINDOW
        println("confirming fullscreen quit")
        this.startService(intent)
        moveTaskToBack(true)
    }

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        println("handle intent: " + intent)

        if (intent.action.equals(ACTION_REQUEST_FULLSCREEN)) {
            confirmFullScreen()
        } else if (intent.action.equals(ACTION_QUIT_FULLSCREEN)) {
            requestFloatingWindow()
        }
    }

    override fun onResume() {
        super.onResume()
        println("onresume")
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        confirmFullScreen()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        println("onBackPRessed")
//        requestFloatingWindow()
    }

    override fun onPause() {
        super.onPause()
        println("onPause")
        requestFloatingWindow()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}