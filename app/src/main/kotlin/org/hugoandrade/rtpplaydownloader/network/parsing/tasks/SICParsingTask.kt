package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

@Deprecated(message = "use a more recent SIC parser")
open class SICParsingTask : ParsingTask() {

    override fun isUrlSupported(url: String) : Boolean {

        val isFileType: Boolean =
                url.contains("sicradical.sapo.pt") ||
                url.contains("sicradical.pt") ||
                url.contains("sicnoticias.sapo.pt") ||
                url.contains("sicnoticias.pt") ||
                url.contains("sic.sapo.pt") ||
                url.contains("sic.pt")

        return isFileType || super.isUrlSupported(url)
    }

    override fun parseMediaUrl(doc: Document): String? {
        try {

            val videoElements = doc.getElementsByTag("video")
            val url :String = this.url ?: ""

            if (videoElements != null) {
                for (videoElement in videoElements.iterator()) {

                    for (sourceElement: Element in videoElement.getElementsByTag("source")) {

                        val src : String = sourceElement.attr("src")
                        val type: String = sourceElement.attr("type")

                        if (src.isEmpty() || type.isEmpty()) continue

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

        try {

            val videoElements = doc.getElementsByTag("video")
            var type: String? = null

            if (videoElements != null) {
                for (videoElement in videoElements.iterator()) {

                    for (sourceElement: Element in videoElement.getElementsByTag("source")) {

                        val src : String = sourceElement.attr("src")
                        type = sourceElement.attr("type")

                        if (src.isEmpty() || type.isEmpty()) continue
                        break
                    }
                }
            }

            val titleElements = doc.getElementsByTag("title")

            if (mediaUrl != null && titleElements != null && titleElements.size > 0) {

                val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())
                        .replace("SIC.Noticias.", "")
                        .replace("SIC.Radical.", "")
                        .replace("SIC.", "")

                if (type != null && type.contains("video/mp4")) {  // is video file
                    return "$title.mp4"
                }

                return title
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return mediaUrl?:url?:null.toString()
    }
}