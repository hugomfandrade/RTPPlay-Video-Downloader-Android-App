package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.net.SocketTimeoutException
import java.net.URL

open class RTPPlayV2ParsingTask : ParsingTaskBase() {

    override fun parseMediaFile(url: String): Boolean {

        this.url = url
        this.mediaUrl = getM3U8File(url) ?: return false
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

        val isFileType: Boolean = urlString.contains("www.rtp.pt/play")

        if (isFileType) {

            val videoFile: String? = getM3U8File(urlString)

            return videoFile != null
        }

        return false
    }

    private fun getM3U8File(urlString: String): String? {
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(urlString).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return null
            }

            val scriptElements = doc?.getElementsByTag("script")

            if (scriptElements != null) {

                for (scriptElement in scriptElements.iterator()) {

                    for (dataNode: DataNode in scriptElement.dataNodes()) {
                        if (dataNode.wholeData.contains("RTPPlayer({")) {

                            val scriptText: String = dataNode.wholeData

                            try {

                                val rtpPlayerSubString: String = scriptText

                                for (i in arrayOf("file: \"", "file : \"")) {
                                    if (rtpPlayerSubString.indexOf(i) >= 0) {
                                        return rtpPlayerSubString.substring(
                                                indexOfEx(rtpPlayerSubString, i),
                                                indexOfEx(rtpPlayerSubString, i) + rtpPlayerSubString.substring(indexOfEx(rtpPlayerSubString, i)).indexOf("\","))

                                    }
                                }
                            } catch (parsingException: java.lang.Exception) {

                            }
                        }
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

            val titleElements = doc?.getElementsByTag("title")

            if (videoFile != null && titleElements != null && titleElements.size > 0) {

                val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())
                        .replace(".RTP.Play.RTP", "")

                if (videoFile.indexOf(".mp4") >= 0) {  // is video file

                    return "$title.mp4"

                } else if (videoFile.indexOf(".mp3") >= 0) { // is audio file
                    return "$title.mp3"
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

    private fun indexOfEx(string : String, subString: String) : Int {
        if (string.contains(subString)) {
            return string.indexOf(subString) + subString.length
        }
        return 0
    }
}