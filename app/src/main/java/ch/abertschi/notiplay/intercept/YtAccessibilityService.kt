package ch.abertschi.notiplay.intercept

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import org.jetbrains.anko.AnkoLogger
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import ch.abertschi.notiplay.NotiplayActivity
import ch.abertschi.notiplay.PlaybackService
import ch.abertschi.notiplay.getVideoIdFromUrl


/**
 * Created by abertschi on 25.02.18.
 */
class YtAccessibilityService : AccessibilityService(), AnkoLogger {

    private var performOneScrol = false
    private var capturing = false
    private var that = this
    private var tryAgain = false
//    private var scrollEnabled = true

    val youtubeTimeSeek = Regex("^[0-9]*\\:[0-9]{2}$")

    private var lastUrl = ""

    companion object {
        var INSTANCE: YtAccessibilityService? = null

    }

    init {
        INSTANCE = this
    }

    override fun onInterrupt() {
    }

    fun setCapturingState(event: AccessibilityEvent?) {
        if (event!!.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                Log.i("Foreground App", event.getPackageName().toString());
                if (event.packageName.toString().contains("chrome")) {
                    BrowserState.GET.onOriginPlayerInForeground(true, this)
                    INSTANCE = this
                    capturing = true

                } else {
                    BrowserState.GET.onOriginPlayerInForeground(false, this)
                    capturing = false
                }
            }
        }
    }

    fun launch(url: String) {
        val notiIntent = Intent(this, PlaybackService::class.java)
        notiIntent.action = PlaybackService.ACTION_INIT_WITH_ID
        notiIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        notiIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, getVideoIdFromUrl(url))
        startService(notiIntent)
    }


    fun readUrl() {
        if (rootInActiveWindow == null) return

        var url: String? = null
        try {
            url = (rootInActiveWindow
                    .findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
                    as List<AccessibilityNodeInfo>)[0].text?.toString()

        } catch (e: Exception) {
        }

        if (url != null) {
            BrowserState.GET.updateVideoUrl(url, this)
            if (lastUrl != url) {
                lastUrl = url
//                scrollEnabled = false
//                launch(url)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun performSwypeUp() {
        val displayMetrics = resources.displayMetrics

        val middleYValue = (displayMetrics.heightPixels / 2.0).toFloat()
        val topQuarterYValue = (displayMetrics.heightPixels / 10.0).toFloat()
        val middleXValue = (displayMetrics.widthPixels / 2.0).toFloat()

        val gestureBuilder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(middleXValue, middleYValue - topQuarterYValue);
        path.lineTo(middleXValue, middleYValue);

        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 100, 300))
        dispatchGesture(gestureBuilder.build(), object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                println("performing swype up gesture")
                super.onCompleted(gestureDescription)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {

                super.onCancelled(gestureDescription)
            }
        }, null)
    }

    fun performSwypeUpIfInValidApp() {
        performOneScrol = true
        try {
            if (capturing) {
                performSwypeUp()
            }
            performOneScrol = false
        } catch (e: Exception) {
        }

    }

    fun parseSeekPosition(event: AccessibilityEvent?) {
        try {
            if (AccessibilityEvent.eventTypeToString(event!!.getEventType()).contains("WINDOW")) {
                val nodeInfo = event?.getSource()
                if (nodeInfo != null)
                    dfs(nodeInfo)
            }
        } finally {

        }
    }

    fun dfs(info: AccessibilityNodeInfo?) {
        try {
            if (info == null)
                return
            if (info.text != null && info.text.length > 0) {
//                println(info.text.toString() + " class: " + info.className + " " + info.viewIdResourceName)
                val g = youtubeTimeSeek.find(info.text)

                if (g?.groups?.size!! > 0
                        && info.packageName.contains("chrome")) {
                    val c = info.text.split(":")
                    val mins = Integer.valueOf(c[0])
                    val secs = Integer.valueOf(c[1])
                    val res = if (mins == 0) secs else mins * 60 + secs

//                    println("FOUND INTERESTING ONE: " + res)
                    BrowserState.GET.updateSeekPosition(res, this)
                }
            }

            for (i in 0 until info.childCount) {
                val child = info.getChild(i)
                dfs(child)
                child?.recycle()
            }
        } catch (e: Exception) {
//            println(e)

        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        setCapturingState(event)
        readUrl()
        if (capturing) {
            parseSeekPosition(event)
        }

        if (capturing && performOneScrol) {
            performOneScrol = false
            performSwypeUp()

        }
    }
}