package org.hugoandrade.rtpplaydownloader.network.download

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.SocketTimeoutException
import java.net.URL

class SICDownloaderTask : DownloaderTaskBase() {

    override fun parseMediaFile(url: String): Boolean {

        this.url = url
        this.mediaUrl = getVideoFile(url) ?: return false
        this.filename = MediaUtils.getUniqueFilenameAndLock(getMediaFileName(url, mediaUrl))
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

    override fun isValid(urlString: String) : Boolean {

        if (!NetworkUtils.isValidURL(urlString)) {
            return false
        }

        val isFileType: Boolean =
                urlString.contains("sicradical.sapo.pt") ||
                urlString.contains("sicradical.pt") ||
                urlString.contains("sicnoticias.sapo.pt") ||
                urlString.contains("sicnoticias.pt") ||
                urlString.contains("sic.sapo.pt") ||
                urlString.contains("sic.pt")

        if (isFileType) {

            val videoFile: String? = getVideoFile(urlString)

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

            val videoElements = doc?.getElementsByTag("video")

            if (videoElements != null) {
                for (videoElement in videoElements.iterator()) {

                    for (sourceElement: Element in videoElement.getElementsByTag("source")) {

                        val src : String = sourceElement.attr("src")
                        val type: String = sourceElement.attr("type")

                        if (src.isEmpty() || type.isEmpty()) continue

                        val location : String = when {
                            urlString.contains("http://") -> "http:"
                            urlString.contains("https://") -> "https:"
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

    override fun getMediaFileName(urlString: String, videoFile: String?): String {

        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return videoFile ?: urlString
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

        return videoFile?:urlString
    }

    private fun getThumbnailPath(urlString: String): String? {
        try {
            val doc: Document

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
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