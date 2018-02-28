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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import org.jetbrains.anko.doAsync
import java.util.*




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

    private val RC_AUTHORIZE_CONTACTS =  10

    @TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("RESULT " + requestCode + " " + resultCode + " " + data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                println("not granted")
                // SYSTEM_ALERT_WINDOW permission not granted...
            }
        }
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == 100
                || requestCode == RC_AUTHORIZE_CONTACTS) {
            if (resultCode == RESULT_OK) {
                fetch()
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
        setContentView(R.layout.activity_main)
        println("starting service")

//        YoutubeApiWrapper().getVideoMetadata("-CzBYn7iRSI")

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
            notiIntent.putExtra(PlaybackService.EXTRA_VIDEO_ID, "-CzBYn7iRSI")
            notiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startService(notiIntent)
            ActivityCompat.finishAffinity(this)
            setContentView(R.layout.activity_main)
        }

    }

    fun foo() {
        val SCOPE_READ = Scope("https://www.googleapis.com/auth/youtube.readonly")
        val SCOPE_EMAIL = Scope(Scopes.EMAIL)

        if (!GoogleSignIn.hasPermissions(
                        GoogleSignIn.getLastSignedInAccount(this),
                        SCOPE_READ,
                        SCOPE_EMAIL)) {

            GoogleSignIn.requestPermissions(
                    this,
                    100,
                    GoogleSignIn.getLastSignedInAccount(this),
                    SCOPE_READ,
                    SCOPE_EMAIL)
        } else {
            fetch()
        }
    }

    /** Global instance of the HTTP transport.  */
    private val HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport()
    /** Global instance of the JSON factory.  */
    private val JSON_FACTORY = JacksonFactory.getDefaultInstance()


    fun fetch() {
        val account = GoogleSignIn.getLastSignedInAccount(this)

        account?.let {

            doAsync {
                val credential = GoogleAccountCredential.usingOAuth2(
                        this@MainActivity,
                        Collections.singleton(
                                "https://www.googleapis.com/auth/youtube.readonly")
                )

                credential.selectedAccount = account.account

                val service = YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName("REST API sample")
                        .build()
                val connectionsResponse = service.channels().list("contentDetails").setMine(true).execute()
                println(connectionsResponse)
                var id = connectionsResponse?.items?.get(0)?.get("id") as String?
                id = "HL"
                id?.run {
                    println("ID: " + id)
                    val watchHistory = service.playlistItems().list("snippet").setPlaylistId(id)

                            .execute()
                    println(watchHistory)
                    for (i in watchHistory.items) {
                        println("ID " + i.get("id"))
                    }
                    println("DONEEE")
                }



                runOnUiThread {

                }
            }
        }

    }
}
