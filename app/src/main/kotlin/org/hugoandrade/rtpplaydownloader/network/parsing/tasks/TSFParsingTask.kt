package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.network.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.IOException
import java.net.URL

class TSFParsingTask : ParsingTask() {

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
                url.contains("tsf.pt")

        if (isFileType) {

            val videoFile: String? = getVideoFile(url)

            return videoFile != null
        }

        return false
    }

    protected fun getVideoFile(url: String): String? {
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(url).timeout(10000).get()
            } catch (ignored: IOException) {
                return null
            }

            val scriptElements = doc.getElementsByTag("script")?: return null

            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    val scriptText: String = dataNode.wholeData

                    if (!scriptText.contains("@context")) continue
                    if (!scriptText.contains("@type")) continue
                    if (!scriptText.contains("VideoObject")) continue
                    if (!scriptText.contains("contentUrl")) continue


                    try {

                        val from = "\"contentUrl\": \""
                        val to = "\""

                        val link: String = scriptText.substring(
                                ParsingUtils.indexOfEx(scriptText, from),
                                ParsingUtils.indexOfEx(scriptText, from) + scriptText.substring(ParsingUtils.indexOfEx(scriptText, from)).indexOf(to))

                        return link
                    } catch (parsingException: java.lang.Exception) {
                        parsingException.printStackTrace()
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
        return RTPPlayUtils.getMediaFileName(url, videoFile)
                .replace(".RTP.Play.RTP", "")
    }
}