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
import ch.abertschi.notiplay.player.PlaybackService


// https://medium.com/@oriharel/how-to-run-javascript-code-in-a-background-service-on-android-8ec1a12ebe92
// https://stackoverflow.com/questions/36917469/how-can-i-work-around-youtube-api-embed-restrictions-like-other-websites/36952048#36952048
class MainActivity : AppCompatActivity() {

    // todo:
    // accept youtu.be links
    // add media session and notification

    var OVERLAY_PERMISSION_REQ_CODE = 1234

    @RequiresApi(Build.VERSION_CODES.M)
    fun grantPermission() {
        if (!Settings.canDrawOverlays(this)) {
            println("request permission")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
        println(url)
        val g = Regex("[?&]v=([0-9a-zA-Z-_]*)").find(url) // v?=
        val g2 = Regex("youtu.be/([0-9a-zA-Z-_]*)").find(url) // youtu.be links
        val g3 = Regex("youtube.com/embed/([0-9a-zA-Z-_]*)").find(url) // /embed/

        if (g?.groups?.size ?: 0 > 1) {
            val id = g!!.groups[1]!!.value
            println(id)
            return id
        }
        if (g2?.groups?.size ?: 0 > 1) {
            val id = g2!!.groups[1]!!.value
            println(id)
            return id
        }

        if (g3?.groups?.size ?: 0 > 1) {
            val id = g3!!.groups[1]!!.value
            println(id)
            return id
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("starting service")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            grantPermission()
        }

        val notiIntent = Intent(this@MainActivity, PlaybackService::class.java)
        notiIntent.action = PlaybackService.ACTION_INIT_WITH_ID
        println(intent.action)


        if (intent.action == Intent.ACTION_SEND) {
            val videoId: String? = getVideoId(intent)

            if (videoId == null) {
                Toast.makeText(this, "No video found :/", Toast.LENGTH_SHORT).show()
            }
            notiIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, videoId)
            notiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            println(videoId)
            startService(notiIntent)
            ActivityCompat.finishAffinity(this)
            return

        } else {

            notiIntent.putExtra(NotiRunner.INTENT_VIDEO_ID, "-CzBYn7iRSI")
            notiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startService(notiIntent)
            ActivityCompat.finishAffinity(this)

            setContentView(R.layout.activity_main)
        }

    }
}
