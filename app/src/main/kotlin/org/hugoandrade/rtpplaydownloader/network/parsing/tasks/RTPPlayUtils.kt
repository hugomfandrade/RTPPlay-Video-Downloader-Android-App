package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException
import java.net.SocketTimeoutException

class RTPPlayUtils

private constructor() {

    init {
        throw AssertionError()
    }

    companion object {

        fun getMediaFileName(doc: Document, srcUrl: String, mediaFileUrl: String?): String {
            try {

                val titleElements = doc.getElementsByTag("title")

                if (mediaFileUrl != null && titleElements != null && titleElements.size > 0) {

                    val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())

                    if (mediaFileUrl.indexOf(".mp4") >= 0) {  // is video file

                        return "$title.mp4"

                    } else if (mediaFileUrl.indexOf(".mp3") >= 0) { // is audio file
                        return "$title.mp3"
                    }

                    return title
                }
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }

            return mediaFileUrl?:srcUrl
        }

        fun getMediaFileName(srcUrl: String, mediaFileUrl: String?): String {

            try {
                val doc: Document

                try {
                    doc = Jsoup.connect(srcUrl).timeout(10000).get()
                } catch (ignored: IOException) {
                    return mediaFileUrl ?: srcUrl
                }

                return getMediaFileName(doc, srcUrl, mediaFileUrl)
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }

            return mediaFileUrl?:srcUrl
        }

        fun getThumbnailPath(doc: Document): String? {

            try {

                val headElements = doc.getElementsByTag("head")?:return null

                for (headElement in headElements.iterator()) {

                    val metaElements = headElement.getElementsByTag("meta")?: Elements()

                    for (metaElement in metaElements.iterator()) {

                        if (!metaElement.hasAttr("property") ||
                                !metaElement.hasAttr("content") ||
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

        fun getThumbnailFromTwitterMetadata(url: String) : String? {
            try {
                val doc: Document

                try {
                    doc = Jsoup.connect(url).timeout(10000).get()
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

        fun getThumbnailPath(srcUrl: String): String? {

            try {
                val doc: Document

                try {
                    doc = Jsoup.connect(srcUrl).timeout(10000).get()
                } catch (ignored: IOException) {
                    return null
                }

                return getThumbnailPath(doc)
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }

            return null
        }
    }
}