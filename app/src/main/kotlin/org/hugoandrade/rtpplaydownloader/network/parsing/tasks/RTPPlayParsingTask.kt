package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException
import java.net.URL

@Deprecated(message = "use RTPPlayParsingTaskV3")
open class RTPPlayParsingTask : ParsingTask() {

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

    override fun isValid(url: String) : Boolean {

        if (!NetworkUtils.isValidURL(url)) {
            return false
        }

        val isFileType: Boolean = url.contains("www.rtp.pt/play")

        if (isFileType) {

            val videoFile: String? = getVideoFile(url)

            return videoFile != null
        }

        return false
    }

    open fun getVideoFile(url: String): String? {
        val doc: Document

        try {
            doc = Jsoup.connect(url).timeout(10000).get()
        } catch (ignored: SocketTimeoutException) {
            return null
        }

        try {
            val scriptElements = doc.getElementsByTag("script")?: return null


            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    val scriptText: String = dataNode.wholeData

                    if (!scriptText.contains("RTPPlayer")) continue


                    try {

                        val rtpPlayerSubString: String = scriptText.substring(ParsingUtils.indexOfEx(scriptText, "RTPPlayer({"), scriptText.lastIndexOf("})"))

                        if (rtpPlayerSubString.indexOf(".mp4") >= 0) {  // is video file

                            if (rtpPlayerSubString.indexOf("fileKey: \"") >= 0) {

                                val link: String = rtpPlayerSubString.substring(
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, "fileKey: \""),
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, "fileKey: \"") + rtpPlayerSubString.substring(ParsingUtils.indexOfEx(rtpPlayerSubString, "fileKey: \"")).indexOf("\","))


                                return "http://cdn-ondemand.rtp.pt$link"
                            }

                        } else if (rtpPlayerSubString.indexOf(".mp3") >= 0) { // is audio file

                            if (rtpPlayerSubString.indexOf("file: \"") >= 0) {

                                return rtpPlayerSubString.substring(
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, "file: \""),
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, "file: \"") + rtpPlayerSubString.substring(ParsingUtils.indexOfEx(rtpPlayerSubString, "file: \"")).indexOf("\","))

                            }
                        }
                    } catch (parsingException: java.lang.Exception) { }
                }
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return null
    }

    override fun getMediaFileName(url: String, videoFile: String?): String {
        return RTPPlayUtils.getMediaFileName(url, videoFile)
    }

    fun getThumbnailPath(url: String): String? {
        return RTPPlayUtils.getThumbnailPath(url)
    }
}