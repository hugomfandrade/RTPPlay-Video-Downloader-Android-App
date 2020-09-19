package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.IOException
import java.net.URL

@Deprecated(message = "use a more recent SIC parser")
open class SICParsingTask : ParsingTask() {

    override fun parseMediaFile(url: String): Boolean {

        this.url = url
        this.mediaUrl = getVideoFile(url) ?: return false
        this.filename = getMediaFileName(url, mediaUrl)
        this.thumbnailUrl = getThumbnailPath(url)

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

        val isFileType: Boolean =
                url.contains("sicradical.sapo.pt") ||
                url.contains("sicradical.pt") ||
                url.contains("sicnoticias.sapo.pt") ||
                url.contains("sicnoticias.pt") ||
                url.contains("sic.sapo.pt") ||
                url.contains("sic.pt")

        if (isFileType) {

            val videoFile: String? = getVideoFile(url)

            return videoFile != null
        }

        return false
    }

    protected open fun getVideoFile(url: String): String? {
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

    override open fun getMediaFileName(url: String, videoFile: String?): String {

        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(url).timeout(10000).get()
            } catch (ignored: IOException) {
                return videoFile ?: url
            }

            val videoElements = doc?.getElementsByTag("video")
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

            val titleElements = doc?.getElementsByTag("title")

            if (videoFile != null && titleElements != null && titleElements.size > 0) {

                val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())
                        .replace("SIC.Noticias.", "")
                        .replace("SIC.Radical.", "")
                        .replace("SIC.", "")

                if (checkNotNull(type?.contains("video/mp4"))) {  // is video file
                    return "$title.mp4"
                }

                return title
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return videoFile?:url
    }

    private fun getThumbnailPath(urlString: String): String? {
        try {
            val doc: Document

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: IOException) {
                return null
            }

            val headElements = doc.getElementsByTag("head")?:return null

            for (headElement in headElements.iterator()) {

                val metaElements = headElement.getElementsByTag("meta")?: Elements()

                for (metaElement in metaElements.iterator()) {

                    if (!metaElement.hasAttr("property") ||
                            !metaElement.hasAttr("name") ||
                            !metaElement.hasAttr("content") ||
                            metaElement.attr("name") != "twitter:image" ||
                            metaElement.attr("property") != "og:image") {
                        continue
                    }

                    val thumbnail = metaElement.attr("content")
                    if (thumbnail.isNullOrEmpty()) {
                        continue
                    }
                    else {
                        return thumbnail
                    }
                }
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return null
    }
}