package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

@Deprecated(message = "use a more recent RTPPlay parser")
open class RTPPlayParsingTaskV3 : TSParsingTask() {

    override fun isUrlSupported(url: String) : Boolean {

        val isFileType: Boolean = url.contains("www.rtp.pt/play")

        return isFileType || super.isUrlSupported(url)
    }

    override fun parseThumbnailPath(doc: Document): String? {
        return ParsingUtils.getThumbnailPath(doc)
    }

    // get playlist url
    override fun parseMediaUrl(doc: Document): String? {

        try {

            val scriptElements = doc.getElementsByTag("script") ?: return null


            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    if (!dataNode.wholeData.contains("RTPPlayer")) continue

                    val scriptText: String = dataNode.wholeData.replace("\\s+".toRegex(), "")

                    try {

                        val rtpPlayerSubString: String = scriptText
                        val from = "file:\""
                        val to = "\","

                        if (rtpPlayerSubString.indexOf(from) >= 0) {
                            val indexFrom = ParsingUtils.indexOfEx(rtpPlayerSubString, from)

                            return rtpPlayerSubString.substring(indexFrom, indexFrom + rtpPlayerSubString.substring(indexFrom).indexOf(to))

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

    override fun parseM3U8Playlist(): TSPlaylist? {
        val tsPlaylist = mediaUrl ?: return null

        if (!TSUtils.getUrlWithoutParameters(tsPlaylist).endsWith(".m3u8")) return null

        return TSPlaylist().add("DEFAULT", tsPlaylist)
    }

    override fun parseMediaFileName(doc: Document): String {
        return super.parseMediaFileName(doc)
                .replace(".RTP.Play.RTP", "") + ".ts"
    }
}