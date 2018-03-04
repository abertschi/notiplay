package ch.abertschi.notiplay.playback.yt

import android.text.TextUtils
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import io.reactivex.Observable
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.warn
import org.json.JSONObject

/**
 * Created by abertschi on 20.02.18.
 */


class YoutubeApiWrapper : AnkoLogger {

    private val API_Key = "AIzaSyCrT-HRrkCwFSwdg3ETJbCMB-GffhNVJfM" // TODO: change after deploy
    private val YOUTUBE_API = "https://www.googleapis.com/youtube/v3"

    data class YoutubeVideoMetadata(var title: String, var description: String, var videoId: String,
                                    var thumbnailUrl: String)


    fun getChannelByUsername(username: String) =
            Observable.create<String> { sink ->
                "$YOUTUBE_API/channels?part=id&forUsername=$username&key=$API_Key"
                        .httpGet()
                        .responseJson { request, response, result ->
                            //info { "got0 " + result }
                            val (json, error) = result
                            if (error != null) sink.onError(Exception("Error in fetching video by channel/username", error))
                            else {
                                try {
                                    println(json!!.obj().toString())
                                    val items = json!!.obj().getJSONArray("items")
                                    println(items.toString())
                                    val item = items.get(0) as JSONObject
                                    val channelId = item.get("id") as String
                                    sink.onNext(channelId)
                                } catch (e: Exception) {
                                    sink.onError(e)
                                }
                            }
                        }
            }


    fun getVideoIdBy(username: String, videoTitle: String) =
            Observable.create<String> { sink ->
                getChannelByUsername(username)
                        .map { channelId ->
                            ("$YOUTUBE_API/search?part=snippet&q=" + TextUtils.htmlEncode(videoTitle) +
                                    "&channelId=$channelId" +
                                    "&key=$API_Key")
                                    .httpGet()
                                    .responseJson { request, response, result ->
                                        //info { "got " + result }
                                        val (json, error) = result
                                        if (error != null) sink
                                                .onError(Exception("Error in fetching video by channel/username", error))
                                        else {
                                            try {
                                                val items = json!!.obj().getJSONArray("items")
                                                val item = items.get(0) as JSONObject
                                                val id = item.get("id") as JSONObject
                                                val videoId = id.get("videoId") as String
                                                sink.onNext(videoId)
                                            } catch (e: Exception) {
                                                sink.onError(Exception("Error while parsing channel/username", e))
                                            }
                                        }
                                    }

                        }.subscribe()
            }


    fun getRelatedVideos(videoId: String) =
            Observable.create<List<YoutubeVideoMetadata>> { sink ->
                "$YOUTUBE_API/search?part=snippet&relatedToVideoId=$videoId&type=video&key=$API_Key"
                        .httpGet()
                        .responseJson { request, response, result ->
                            val (json, error) = result
                            if (error != null) sink.onError(Exception("Error in fetching related videos"))
                            else {
                                try {
                                    val items = json!!.obj().getJSONArray("items")
                                    val metadata = ArrayList<YoutubeVideoMetadata>()
                                    for (i: Int in 0 until items.length()) {
                                        val item = items.get(i) as JSONObject
                                        val id = item.getJSONObject("id").getString("videoId")
                                        val snippet = item.getJSONObject("snippet") as JSONObject
                                        metadata.add(createMetadataFromSnippet(snippet, id))
                                    }
                                    sink.onNext(metadata)
                                } catch (e: Exception) {
                                    sink.onError(Exception("Error while parsing related video response", e))
                                }
                            }
                        }
            }

    fun getVideoMetadata(vId: String) = Observable.create<YoutubeVideoMetadata> { sink ->
        "${YOUTUBE_API}/videos?part=snippet&id=${vId}&key=${API_Key}"
                .httpGet()
                .responseJson { request, response, result ->
                    val (data, error) = result
                    if (error != null) {
                        sink.onError(Exception("Error in calling Youtube API"))
                    } else {
                        val json = data!!
                        try {
                            //info(json.content)
                            val snippet = json.obj().getJSONArray("items")
                                    .getJSONObject(0)
                                    .getJSONObject("snippet")
                            sink.onNext(createMetadataFromSnippet(snippet, vId))
                        } catch (e: Exception) {
                            sink.onError(Exception("Error in parsing Youtube API response", e))
                            warn(e)
                        }
                    }
                }
    }

    private fun createMetadataFromSnippet(snippet: JSONObject, videoId: String):
            YoutubeVideoMetadata {
        val thumbnails = snippet.get("thumbnails") as JSONObject
        val thumbnailUrl = if (thumbnails.has("maxres")) {
            thumbnails.getJSONObject("maxres").getString("url")
        } else thumbnails.getJSONObject("standard").getString("url")

        return YoutubeVideoMetadata(
                snippet.getString("title"),
                snippet.getString("description"),
                videoId,
                thumbnailUrl)
    }
}