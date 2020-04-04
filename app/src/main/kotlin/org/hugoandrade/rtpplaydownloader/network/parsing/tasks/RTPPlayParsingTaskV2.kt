package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException
import java.net.URL

open class RTPPlayParsingTaskV2 : RTPPlayParsingTask() {

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

    private fun getM3U8File(url: String): String? {
        val doc: Document

        try {
            doc = Jsoup.connect(url).timeout(10000).get()
        } catch (ignored: SocketTimeoutException) {
            return null
        }

        try {

            val scriptElements = doc.getElementsByTag("script") ?: return null


            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    val scriptText: String = dataNode.wholeData

                    if (!scriptText.contains("RTPPlayer({")) continue

                    try {

                        val rtpPlayerSubString: String = scriptText

                        for (i in arrayOf("file: \"", "file : \"")) {
                            if (rtpPlayerSubString.indexOf(i) >= 0) {
                                return rtpPlayerSubString.substring(
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, i),
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, i) + rtpPlayerSubString.substring(ParsingUtils.indexOfEx(rtpPlayerSubString, i)).indexOf("\","))

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
}