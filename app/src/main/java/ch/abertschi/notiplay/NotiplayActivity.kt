package ch.abertschi.notiplay

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast


class NotiplayActivity : AppCompatActivity() {

    var OVERLAY_PERMISSION_REQ_CODE = 1234
    private val RC_AUTHORIZE_CONTACTS = 10

    @RequiresApi(Build.VERSION_CODES.M)
    fun grantOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            println("request permission")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("RESULT " + requestCode + " " + resultCode + " " + data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                println("not granted")
                // SYSTEM_ALERT_WINDOW permission not granted...
            }
        }
    }

    fun getVideoId(intent: Intent?): String? {
        if (intent == null || intent.action == null)
            return null

        val url = intent.getStringExtra(Intent.EXTRA_TEXT)
        return getVideoIdFromUrl(url)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            grantOverlayPermission()
        }
        processIntent()


    }

    fun processIntent() {
        val notiIntent = Intent(this@NotiplayActivity, PlaybackService::class.java)
        notiIntent.action = PlaybackService.ACTION_INIT_WITH_ID

        println(intent.action)

        if (intent.action == Intent.ACTION_SEND) {
            val videoId: String? = getVideoId(intent)

            if (videoId == null) {
                Toast.makeText(this, "No video found :/", Toast.LENGTH_SHORT).show()
            }
            notiIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, videoId)
            notiIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            println("LOADING: " + videoId)
            startService(notiIntent)
            ActivityCompat.finishAffinity(this)
            return

        } else {
            notiIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, "-CzBYn7iRSI")
            notiIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startService(notiIntent)
            ActivityCompat.finishAffinity(this)
            setContentView(R.layout.activity_main)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        println("intent: " + intent?.action)
        this.intent = intent
        processIntent()
    }
}
