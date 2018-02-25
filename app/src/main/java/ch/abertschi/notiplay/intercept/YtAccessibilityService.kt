package ch.abertschi.notiplay.intercept

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


/**
 * Created by abertschi on 25.02.18.
 */
class YtAccessibilityService : AccessibilityService(), AnkoLogger {

    override fun onInterrupt() {
        info("onInterrupt")

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        info { event }
//        info { "root: " + rootInActiveWindow }
//        if (AccessibilityEvent.eventTypeToString(event!!.getEventType()).contains("WINDOW")) {
//            val nodeInfo = event?.getSource()
//            if (nodeInfo != null)
//                dfs(nodeInfo)
//        }

        if (event!!.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                Log.i("Foreground App", event.getPackageName().toString());

            }
        }


    }


    fun dfs(info: AccessibilityNodeInfo?) {
        if (info == null)
            return
        if (info.text != null && info.text.length > 0)
//            println(info.text.toString() + " class: " + info.className + " " )
        for (i in 0 until info.childCount) {
            val child = info.getChild(i)
            dfs(child)
            child?.recycle()
        }
    }

}