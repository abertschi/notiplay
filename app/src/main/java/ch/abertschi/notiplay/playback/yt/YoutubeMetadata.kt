package ch.abertschi.notiplay.playback.yt

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART
import android.util.Log.wtf
import ch.abertschi.notiplay.player.PlaybackManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.net.URL

/**
 * Created by abertschi on 21.02.18.
 */
class YoutubeMetadata(val metadataListener: PlaybackManager.MetadataListener) : AnkoLogger {

    val metadata = YoutubeApiWrapper()

    private var currentVideoId: String = ""

    fun setVideoId(videoId: String) {
        currentVideoId = videoId
    }

    private fun loadThumbnailFromUrl(url: String): Observable<Bitmap>? {
        return Observable.create<Bitmap> {
            try {
                val url = URL(url)
                info(url)
                val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                it.onNext(bmp)
            } catch (e: Exception) {
                wtf("cannot load thumbnail, ", e)
            }
        }
    }

    fun buildMetadataEvent(m: YoutubeApiWrapper.YoutubeVideoMetadata, bitmap: Bitmap?) {
        val m = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, m.title)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, m.videoId)
//                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "subtitle")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, m.thumbnailUrl)

        if (bitmap != null) {
            m.putBitmap(METADATA_KEY_ALBUM_ART, bitmap)
        } else {
        }
        metadataListener.onMetadataChanged(m.build())
    }

    fun fetchMetadata() {
        metadata.getVideoMetadata(currentVideoId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ m ->
                    run {
                        buildMetadataEvent(m, null)
                        loadThumbnailFromUrl(m.thumbnailUrl)!!
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    buildMetadataEvent(m, it)
                                }
                    }
                }, { err -> wtf(err.message, err) })
    }
}