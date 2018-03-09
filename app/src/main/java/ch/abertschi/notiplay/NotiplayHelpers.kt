package ch.abertschi.notiplay

/**
 * Created by abertschi on 09.03.18.
 */

fun getVideoIdFromUrl(url: String): String? {
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