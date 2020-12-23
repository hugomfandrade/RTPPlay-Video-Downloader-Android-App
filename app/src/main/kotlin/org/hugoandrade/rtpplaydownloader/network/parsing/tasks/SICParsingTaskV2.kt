package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

open class SICParsingTaskV2 : SICParsingTask() {

    override fun parseMediaUrl(doc: Document): String? {
        try {

            val videoElements = doc.getElementsByTag("video")
            val url = this.url ?: ""

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

    override fun parseMediaFileName(doc: Document): String {
        return super.parseMediaFileName(doc) + ".ts"
    }
}