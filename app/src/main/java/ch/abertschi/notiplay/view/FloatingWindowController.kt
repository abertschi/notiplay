package ch.abertschi.notiplay.view

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

    var allowMoveOut = false
    var allowPositionCorrection = false

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.run {

            when (intent.action) {
                ACTION_REQUEST_FLOATING_WINDOW -> {
                    requestAndConfirmHalfscreen()
                }
                ACTION_CONFIRM_FULLSCREEN -> {
                    confirmFullscreen()
                }
                else -> {
                }
            }

        }
    }

    fun isFullscreen() = floatingWindow?.stateActive?.state == FloatingWindow.State.FULLSCREEN
    fun isVisible() = floatingWindow!!.isVisible

    //    private var isFullScreen = false
    private var floatingWindow: FloatingWindow? = null
    private var started: Boolean = false


    companion object {
        val ACTION_CONFIRM_FULLSCREEN = "action_confirm_fullscreen"
        val ACTION_REQUEST_FLOATING_WINDOW = "action_request_floating_window"
    }


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
        floatingWindow?.onDoubleTab = this::onDoubleTab
        floatingWindow?.loadLayout(childView)
        setHalfscreen()
        started = true
    }

    fun onDoubleTab() {
        info { "DOUBLE TAB ${floatingWindow?.stateActive?.state}" }

        if (floatingWindow?.stateActive?.state == FloatingWindow.State.SMALL_FLOATING) {
            setHalfscreen()
        } else if (floatingWindow?.stateActive?.state == FloatingWindow.State.HALF_SCREEN) {
            info { "FULLS" }
            requestFullScreen()
        }

    }

    fun stopFloatingWindow() {
        service.unregisterReceiver(this)
        floatingWindow?.stopView()
    }


    private fun requestFullScreen() {
        info { "REQU" }
        if (isFullscreen()) return
        info { "2" }
        if (floatingWindow?.stateActive?.state == FloatingWindow.State.SMALL_FLOATING) {
            floatingWindow?.storeLayoutParams(floatingWindow!!.stateActive)
        }
        info { "3" }


        val dialogIntent = Intent(c, HorizontalFullscreenActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        dialogIntent.action = "request_fullscreen"
        startActivity(c, dialogIntent, null)
        info { "4" }
    }

    private fun quitFullscreen() {
        if (!isFullscreen()) return
        val dialogIntent = Intent(c, HorizontalFullscreenActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        dialogIntent.action = HorizontalFullscreenActivity.ACTION_QUIT_FULLSCREEN
        startActivity(c, dialogIntent, null)

    }

    private fun confirmFullscreen() {
        println("CONFIRM FULLSCREEN")

//        println("confirm fullscreen: " + isFullScreen)
        floatingWindow?.setSizeToFullScreen()
    }

    private fun requestAndConfirmHalfscreen() {
//        if (!isFullscreen()) return
        setHalfscreen()


//        allowPositionCorrection = true
//        allowMoveOut = false
//        println("state is: " + isFullScreen)
//        println("confirm floating window")

//        floatingWindow?.setSizeToFloatingWindow()
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
        floatingWindow?.storeLayoutParams(floatingWindow!!.stateActive)
        floatingWindow?.setSizeToHalfScreen()

    }

    fun setFullscreen(state: Boolean) {
        if (state) {
            requestFullScreen()
        } else {
            requestAndConfirmHalfscreen()
        }
    }


}