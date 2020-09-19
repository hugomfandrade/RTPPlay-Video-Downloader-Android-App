package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

@Deprecated(message = "use a more recent SIC parser")
open class SICParsingTaskV2 : SICParsingTask() {

    override fun getVideoFile(url: String): String? {
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(url).timeout(10000).get()
            } catch (ignored: IOException) {
                return null
            }

            val videoElements = doc?.getElementsByTag("video")

            if (videoElements != null) {
                for (videoElement in videoElements.iterator()) {

                    for (sourceElement: Element in videoElement.getElementsByTag("source")) {

                        val src : String = sourceElement.attr("src")
                        val type: String = sourceElement.attr("type")

                        if (src.isEmpty() || type.isEmpty()) continue

                        if (type != "application/vnd.apple.mpegurl") return null

                        val location : String = when {
                            url.contains("http://") -> "http:"
                            url.contains("https://") -> "https:"
                            else -> ""
                        }

                        return location + src
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
        return super.getMediaFileName(url, videoFile) + ".ts"
    }
}