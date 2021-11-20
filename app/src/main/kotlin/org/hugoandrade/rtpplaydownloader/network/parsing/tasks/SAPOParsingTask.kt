package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException

class SAPOParsingTask : ParsingTask {

    override fun isUrlSupported(url: String): Boolean {

        return url.contains("videos.sapo.pt")
    }

    override fun parseMediaUrl(doc: Document): String? {
        try {

            val playerVideoElements = doc.getElementsByAttributeValue("id", "player-video")
            val url = doc.baseUri()

            if (playerVideoElements != null) {

                for (playerVideoElement in playerVideoElements.iterator()) {

                    val dataVideoLink : String = playerVideoElement.attr("data-video-link")

                    if (dataVideoLink.isEmpty()) continue

                    val location : String = when {
                        url.contains("http://") -> "http:"
                        url.contains("https://") -> "https:"
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

    override fun parseMediaFileName(doc: Document, mediaUrl : String): String {

        try {

            val titleElements = doc.getElementsByTag("title")

            if (mediaUrl != null && titleElements != null && titleElements.size > 0) {

                val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())
                        .replace(".SAPO.Videos", "")

                return "$title.mp4"
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return mediaUrl?: doc.baseUri()?: null.toString()
    }
}