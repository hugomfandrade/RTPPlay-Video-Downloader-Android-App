package org.hugoandrade.rtpplaydownloader.network.parsing

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

class ParsingUtils

/**
 * Ensure this class is only used as a utility.
 */
private constructor() {

    init {
        throw AssertionError()
    }

    companion object {

        fun indexOfEx(string: String, subString: String): Int {
            if (string.contains(subString)) {
                return string.indexOf(subString) + subString.length
            }
            return 0
        }

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

        fun getMediaFileName(url: String, mediaFileUrl: String?): String {

            try {
                val doc: Document = Jsoup.connect(url).timeout(10000).get()

                return getMediaFileName(doc, url, mediaFileUrl)
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }

            return mediaFileUrl ?: url
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


        fun getThumbnailPath(url: String): String? {

            try {
                val doc = Jsoup.connect(url).timeout(10000).get()
                return getThumbnailPath(doc)
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }

            return null
        }

        fun getThumbnailFromTwitterMetadata(url: String) : String? {
            try {
                val doc = Jsoup.connect(url).timeout(10000).get()
                return getThumbnailFromTwitterMetadata(doc)
            }
            catch (e : java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun getThumbnailFromTwitterMetadata(doc: Document) : String? {
            try {

                val headElements = doc.getElementsByTag("head")?:return null

                for (headElement in headElements.iterator()) {

                    val metaElements = headElement.getElementsByTag("meta")?: Elements()

                    for (metaElement in metaElements.iterator()) {

                        if (!metaElement.hasAttr("name") ||
                            !metaElement.hasAttr("content") ||
                            metaElement.attr("name") != "twitter:image") {
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
}