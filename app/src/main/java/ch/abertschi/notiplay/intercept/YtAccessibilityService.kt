package ch.abertschi.notiplay.intercept

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.*


/**
 * Created by abertschi on 25.02.18.
 */
class YtAccessibilityService : AccessibilityService(), AnkoLogger {

    override fun onInterrupt() {
        info("onInterrupt")

    }

    var active = false
    var done = false

    init {
        that = this
    }

    companion object {
        var that: YtAccessibilityService? = null

    }

    var capture = false

    fun scrollView(nodeInfo: AccessibilityNodeInfo?): Boolean {
        if (nodeInfo == null) return false

        if (nodeInfo.isScrollable()) {
            return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }

        var i = 0
        while (i < nodeInfo.getChildCount()) {
            if (scrollView(nodeInfo.getChild(i)))
                return true
            i--
            nodeInfo.recycle()
        }
        return false
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val deque = ArrayDeque<AccessibilityNodeInfo>()
        deque.add(root)
        while (!deque.isEmpty()) {
            val node = deque.removeFirst()
            if (node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP)) {
                return node
            }
            for (i in 0 until node.getChildCount()) {
                deque.addLast(node.getChild(i))
            }
        }
        return null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

//        info { "root: " + rootInActiveWindow }
//        if (AccessibilityEvent.eventTypeToString(event!!.getEventType()).contains("WINDOW")) {
//            val nodeInfo = event?.getSource()
//            if (nodeInfo != null)
//                dfs(nodeInfo)
//        }


        if (rootInActiveWindow != null && active && capture) {
            info { event }
            capture = false
            println("SCROLLING UPE")
            active = false
            val scrollable = findScrollableNode(rootInActiveWindow)
            scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id)
        }

//        performGlobalAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)


        if (event!!.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                Log.i("Foreground App", event.getPackageName().toString());
                if (event.packageName.toString().contains("chrome")) {
                    capture = true
                } else {

                }
//

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