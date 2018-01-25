package ch.abertschi.notiplay

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebSettings
import android.webkit.WebView
import ch.abertschi.notiplay.NotiRunner
import ch.abertschi.notiplay.R

// https://medium.com/@oriharel/how-to-run-javascript-code-in-a-background-service-on-android-8ec1a12ebe92
// https://stackoverflow.com/questions/36917469/how-can-i-work-around-youtube-api-embed-restrictions-like-other-websites/36952048#36952048
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        println("starting service")
        startService(Intent(this@MainActivity, NotiRunner::class.java))


    }
}
