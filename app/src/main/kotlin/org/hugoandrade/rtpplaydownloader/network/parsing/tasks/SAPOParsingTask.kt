package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException
import java.net.URL

class SAPOParsingTask : ParsingTask() {

    override fun parseMediaFile(url: String): Boolean {

        this.url = url
        mediaUrl = getVideoFile(url) ?: return false
        filename = MediaUtils.getUniqueFilenameAndLock(getMediaFileName(url, mediaUrl))

        try {
            URL(mediaUrl)
        }
        catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    override fun isValid(url: String) : Boolean {

        if (!NetworkUtils.isValidURL(url)) {
            return false
        }

        val isFileType: Boolean = url.contains("videos.sapo.pt")

        if (isFileType) {

            val videoFile: String? = getVideoFile(url)

            return videoFile != null
        }

        return false
    }

    private fun getVideoFile(urlString: String): String? {
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return null
            }

            val playerVideoElements = doc?.getElementsByAttributeValue("id", "player-video")

            if (playerVideoElements != null) {

                for (playerVideoElement in playerVideoElements.iterator()) {

                    val dataVideoLink : String = playerVideoElement.attr("data-video-link")

                    if (dataVideoLink.isEmpty()) continue

                    val location : String = when {
                        urlString.contains("http://") -> "http:"
                        urlString.contains("https://") -> "https:"
                        else -> ""
                    }

                    try {
                        val res : Connection.Response  = Jsoup.connect(location + dataVideoLink)
                                .ignoreContentType(true)
                                .timeout(10000)
                                .execute()

                        val url : String = res.url().toString()

                        if (url.isNotEmpty()) {
                            return url
                        }
                    } catch (e: SocketTimeoutException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return null
    }

    override fun getMediaFileName(url: String, videoFile: String?): String {

        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(url).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return videoFile ?: url
            }


            val titleElements = doc?.getElementsByTag("title")

            if (videoFile != null && titleElements != null && titleElements.size > 0) {

                val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())
                        .replace(".SAPO.Videos", "")

                return "$title.mp4"
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return videoFile?:url
    }
}