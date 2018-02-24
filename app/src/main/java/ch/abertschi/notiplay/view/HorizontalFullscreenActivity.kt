package ch.abertschi.notiplay.view

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle

import ch.abertschi.notiplay.R

/**
 * Created by abertschi on 04.02.18.
 */
class HorizontalFullscreenActivity : Activity() {

    companion object {
        val ACTION_REQUEST_FULLSCREEN = "action_request_fullscreen"
        val ACTION_QUIT_FULLSCREEN = "action_quit_fullscreen"
        val REQUEST_CODE = 77
    }

    var intentRequestFullscreen: PendingIntent? = null
    var intentQuitFullscreen: PendingIntent? = null

    init {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("oncreate")
        setContentView(R.layout.activity_fullscreen)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;



        handleIntent(intent)
    }


    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleIntent(intent)
    }

    fun confirmFullScreen() {
        val intent = Intent() //applicationContext, FloatingWindowController::class.java)
        intent.action = FloatingWindowController.ACTION_CONFIRM_FULLSCREEN
        println("CONFIRM FULLSCREEN")
        this.sendBroadcast(intent)
        println("SEND")
    }

    fun quitFullScreen() {
        val intent = Intent()
        intent.action = FloatingWindowController.ACTION_REQUEST_FLOATING_WINDOW
        println("confirming fullscreen quit")
        this.sendBroadcast(intent)
        moveTaskToBack(true)
    }

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        println("handle intent: " + intent)

        if (intent.action.equals(ACTION_REQUEST_FULLSCREEN)) {
            confirmFullScreen()
        } else if (intent.action.equals(ACTION_QUIT_FULLSCREEN)) {
            quitFullScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        println("onresume")
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        confirmFullScreen()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        println("onBackPRessed")
//        quitFullScreen()
    }

    override fun onPause() {
        super.onPause()
        println("onPause")
        quitFullScreen()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}