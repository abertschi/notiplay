package ch.abertschi.notiplay

/**
 * Created by abertschi on 25.01.18.
 */
interface NotiObserver {

    enum class PlayerState(val value: Int) {
        UNSTARTED(-1),
        ENDED(0),
        PLAYING(1),
        PAUSED(2),
        BUFFERING(3),
        VIDEO_CUED(5);

        companion object {
            fun toPlayerState(value: Int): PlayerState {
                return when {
                    value == UNSTARTED.value -> UNSTARTED
                    value == PLAYING.value -> PLAYING
                    value == PAUSED.value -> PAUSED
                    value == BUFFERING.value -> BUFFERING
                    value == VIDEO_CUED.value -> VIDEO_CUED
                    else -> ENDED
                }
            }
        }
    }


    enum class ErrorCode(val code: Int) {
        // java 2 – The request contains an invalid parameter value.
        // java For example, this error occurs if you specify a video ID that
        // does not have 11 characters,
        // or if the video ID contains invalid characters, such as exclamation points or asterisks.
        INVALID_REQUEST_PARAM(2),

        // 5 – The requested content cannot be played in an HTML5 player or another error related
        // to the HTML5 player has occurred.
        PLAYER_ISSUE(5),

        //   100 – The video requested was not found. This error occurs when a video has been
        // removed (for any reason) or has been marked as private.
        VIDEO_REMOVED(100),

        // 101 – The owner of the requested video does not allow it to be played in embedded players.
        PLAYBACK_FORBIDDEN_101(101),

        // 150 – This error is the same as 101. It's just a 101 error in disguise!
        PLAYBACK_FORBIDDEN_150(101);


        companion object {
            fun toErrorEnum(value: Int): ErrorCode {
                return when (value) {
                    INVALID_REQUEST_PARAM.code -> INVALID_REQUEST_PARAM
                    PLAYER_ISSUE.code -> PLAYER_ISSUE
                    VIDEO_REMOVED.code -> VIDEO_REMOVED
                    PLAYBACK_FORBIDDEN_101.code -> PLAYBACK_FORBIDDEN_101
                    else -> PLAYBACK_FORBIDDEN_150
                }
            }
        }
    }

    fun onPlayerReady(): Unit

    fun onPlayerStateChange(state: PlayerState): Unit

    fun onPlaybackQualityChange(quality: String): Unit

    fun onPlaybackRateChange(rate: Int): Unit

    fun onErrorCode(code: ErrorCode)

    fun onPlaybackPosition(seconds: Int)

    fun onPlaybackPositionUpdate(seconds: Int)

    fun onVideoData(title: String, thumbail: String, duration: Int, loop: Boolean, videoId: String)


}
