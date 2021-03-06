package ch.abertschi.notiplay.intercept

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.wtf


/**
 * Created by abertschi on 25.02.18.
 */
class BrowserAccessibilityService : AccessibilityService(), AnkoLogger {

    private var performOneScrol = false
    private var capturing = false
    private val youtubeTimeSeek = Regex("^[0-9]*\\:[0-9]{2}$")

    private val supportedBrowserPackage = "chrome"

    companion object {
        var INSTANCE: BrowserAccessibilityService? = null
    }

    init {
        INSTANCE = this
    }

    override fun onInterrupt() {
    }

    fun setCapturingState(event: AccessibilityEvent?) {
        try {
            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (event.getPackageName() != null && event.getClassName() != null) {
                    Log.i("Foreground App", event.getPackageName().toString())
                    if (event.packageName.toString().contains(supportedBrowserPackage)) {
                        BrowserState.GET.onOriginPlayerInForeground(true, this)
                        capturing = true

                    } else {
                        BrowserState.GET.onOriginPlayerInForeground(false, this)
                        capturing = false
                    }
                }
            }
        } catch (e: Exception) {
        }
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
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun performSwypeUp() {

        info { "--- performing scroll up" }
        try {

            val displayMetrics = resources.displayMetrics

            val middleYValue = (displayMetrics.heightPixels / 2.0).toFloat()
            val topQuarterYValue = (displayMetrics.heightPixels / 10.0).toFloat()
            val middleXValue = (displayMetrics.widthPixels / 2.0).toFloat()

            val gestureBuilder = GestureDescription.Builder()
            val path = Path()
            path.moveTo(middleXValue, middleYValue - topQuarterYValue);
            path.lineTo(middleXValue, middleYValue);

            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            dispatchGesture(gestureBuilder.build(), object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    info("performing swype up gesture")
                    super.onCompleted(gestureDescription)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                }
            }, null)
        } catch (e: Exception) {
            wtf("ouch, something went wrong in browser accessilbiity service", e)
        }
    }

    fun performSwypeUpIfInValidApp() {
        performOneScrol = true
        try {
            performSwypeUp()
            // dont check for capture flag here as it seems to work better without
            performOneScrol = false
        } catch (e: Exception) {
            wtf("Perform swype up failed", e)
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
                val g = youtubeTimeSeek.find(info.text)

                if (g?.groups?.size!! > 0
                        && info.packageName.contains("chrome")) {
                    val c = info.text.split(":")
                    val mins = Integer.valueOf(c[0])
                    val secs = Integer.valueOf(c[1])
                    val res = if (mins == 0) secs else mins * 60 + secs
                    BrowserState.GET.updateSeekPosition(res, this)
                }
            }

            for (i in 0 until info.childCount) {
                val child = info.getChild(i) ?: continue
                dfs(child)
                child?.recycle()
            }
        } catch (e: Throwable) {
            //info(e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            setCapturingState(event)
            readUrl()

            if (capturing) {
                parseSeekPosition(event)
            }

            if (capturing && performOneScrol) {
                performOneScrol = false
                performSwypeUp()

            }
        } finally {
        }
    }
}