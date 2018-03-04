package ch.abertschi.notiplay.view

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.ContextCompat.startActivity
import android.view.View
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * Created by abertschi on 23.02.18.
 */
class FloatingWindowController(val c: Context, val service: Service) : BroadcastReceiver(),
        AnkoLogger {

    enum class WindowSizeState {
        FLOATING, HALFSCREEN, FULLSCREEN
    }

    var allowMoveOut = false
    var allowPositionCorrection = false

    private var windowSizeState: WindowSizeState = WindowSizeState.FLOATING

    override fun onReceive(context: Context?, intent: Intent?) {
        info { "ON RECEIVE WITH NULL INTENT" }
        intent?.run {
            info { "#####$$$$$#### from floatingwindow controller: " + intent.action }
            when (intent.action) {
            // called before fullscreen is closed
            // view can store fullscreen state
                ACTION_REQUEST_FLOATING_WINDOW -> {
                    requestAndConfirmFloatingWindow()

                }

            // called after fullscreen closed
                ACTION_CONFIRM_FULLSCREEN -> {
                    confirmFullscreen()
                }
                else -> {

                }
            }

        }
    }

    fun isFullscreen() = isFullScreen
    fun isVisible() = floatingWindow?.showFloatingWindow
    private var isFullScreen = false
    private var floatingWindow: FloatingWindow? = null
    private var started: Boolean = false


    companion object {
        val ACTION_CONFIRM_FULLSCREEN = "action_confirm_fullscreen"
        val ACTION_REQUEST_FLOATING_WINDOW = "action_request_floating_window"

    }

    var intentFullscreenConfirmed: PendingIntent? = null
    var intentRequestFloatingWindow: PendingIntent? = null

    private val REQUEST_CODE: Int = 1

    init {
    }


    fun startFloatingWindow(childView: View) {
        if (started) return
        val filter = IntentFilter()
        filter.addAction(ACTION_CONFIRM_FULLSCREEN)
        filter.addAction(ACTION_REQUEST_FLOATING_WINDOW)
        service.registerReceiver(this, filter)
        floatingWindow = FloatingWindow(this.c, this)
//        floatingWindow?.onFloatingWindowAction = this::requestAndConfirmFloatingWindow
        floatingWindow?.onDoubleTab = this::onDoubleTab
        floatingWindow?.loadLayout(childView)
        setHalfscreen()
        started = true
    }

    fun onDoubleTab() {
        println("double tab: " + isFullScreen)
        if (!isFullscreen()) {
            if (windowSizeState == WindowSizeState.FLOATING) {
                setHalfscreen()
            } else if (windowSizeState == WindowSizeState.HALFSCREEN) {
                requestFullScreen()
            } else {

            }
        }

    }

    fun stopFloatingWindow() {
        service.unregisterReceiver(this)
        floatingWindow?.stopView()
    }


    private fun requestFullScreen() {
        println("request fullscreen: " + isFullScreen)
        if (isFullScreen) return
        isFullScreen = true
        windowSizeState = WindowSizeState.FULLSCREEN
        allowPositionCorrection = false

        if (windowSizeState == WindowSizeState.FLOATING) {
            floatingWindow?.storeLayoutParams(floatingWindow!!.stateActive)
        }

        val dialogIntent = Intent(c, HorizontalFullscreenActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        dialogIntent.action = "request_fullscreen"
        startActivity(c, dialogIntent, null)
    }

    private fun quitFullscreen() {
        println("quit fullscreen: " + isFullScreen)
        if (!isFullScreen) return
        val dialogIntent = Intent(c, HorizontalFullscreenActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        dialogIntent.action = HorizontalFullscreenActivity.ACTION_QUIT_FULLSCREEN
        startActivity(c, dialogIntent, null)

    }

    private fun confirmFullscreen() {
        allowMoveOut = false
        println("confirm fullscreen: " + isFullScreen)
        floatingWindow?.setSizeToFullScreen()
    }

    private fun requestAndConfirmFloatingWindow() {
        println("request and confirm floating : " + isFullScreen)
        if (!isFullScreen) return
        isFullScreen = false
        windowSizeState = WindowSizeState.FLOATING
        allowPositionCorrection  = true
        allowMoveOut = false
        println("state is: " + isFullScreen)
        println("confirm floating window")
        floatingWindow?.setSizeToFloatingWindow()
    }

//    fun confirmFloatingWindow() {
//        floatingWindow?.setSizeToFloatingWindow()
//    }

    fun toggleVisible() {
        if (!isFullscreen()) {
            floatingWindow?.toggleVisible()
        }
    }

    fun setVisible(state: Boolean) {
        if (!isFullscreen()) {
            floatingWindow?.makeVisible(state)
        }
    }

    fun setHalfscreen() {
        allowMoveOut = false
        allowPositionCorrection = false
        floatingWindow?.storeLayoutParams(floatingWindow!!.stateActive)
//        floatingWindow?.storeLayoutParams(floatingWindow?.storeLayoutParams(floatingWindow!!.stateActive))
        floatingWindow?.setSizeToHalfScreen()
        windowSizeState = WindowSizeState.HALFSCREEN
//        floatingWindow?.
    }

    fun setFullscreen(state: Boolean) {
        if (state) {
            requestFullScreen()
        } else {
            requestAndConfirmFloatingWindow()
        }
    }


}