package ch.abertschi.notiplay

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import ch.abertschi.notiplay.NotiRunner

// https://medium.com/@oriharel/how-to-run-javascript-code-in-a-background-service-on-android-8ec1a12ebe92
// https://stackoverflow.com/questions/36917469/how-can-i-work-around-youtube-api-embed-restrictions-like-other-websites/36952048#36952048
class MainActivity : AppCompatActivity() {

    // todo:
    // accept youtu.be links
    // add media session and notification


    fun getVideoId(intent: Intent?): String? {
        if (intent == null || intent.action == null)
            return null

        val url = intent.getStringExtra(Intent.EXTRA_TEXT)
        println(url)
        val g = Regex("[?&]v=([0-9a-zA-Z-_]*)").find(url)

        if (g?.groups?.size ?: 0 > 1) {
            val id = g!!.groups[1]!!.value
            println(id)
            return id
        }

        return null
    }

    override fun onNewIntent(intent: Intent?) {
        println("ONNEWINTENT")
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("starting service")

        val notiIntent = Intent(this@MainActivity, NotiRunner::class.java)
        println(intent.action)
        if (intent.action == Intent.ACTION_SEND) {
            val videoId: String? = getVideoId(intent)

            if (videoId == null) {
                Toast.makeText(this, "No video found :/", Toast.LENGTH_SHORT).show()
                return
            }
            notiIntent.putExtra(NotiRunner.INTENT_VIDEO_ID, videoId)
            notiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            println(videoId)
            startService(notiIntent)
            ActivityCompat.finishAffinity(this);

//            val i = Intent(Intent.ACTION_MAIN)
//            i.addCategory(Intent.CATEGORY_HOME)
//            startActivity(i)
//            finish()
            //finishActivity(0)
//            finish()

        } else {
            setContentView(R.layout.activity_main)
        }

    }
}
