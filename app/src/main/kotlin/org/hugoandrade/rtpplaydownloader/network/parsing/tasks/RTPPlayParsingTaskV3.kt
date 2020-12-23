package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

open class RTPPlayParsingTaskV3 : TSParsingTask() {

    override fun isValid(url: String) : Boolean {

        val isFileType: Boolean = url.contains("www.rtp.pt/play")

        return isFileType || super.isValid(url)
    }

    override fun parseMediaFileName(doc: Document): String {
        return RTPPlayUtils.getMediaFileName(doc, url?: null.toString(), mediaUrl)
    }

    override fun parseThumbnailPath(doc: Document): String? {
        return RTPPlayUtils.getThumbnailPath(doc)
    }

    // get playlist url
    override fun parseMediaUrl(doc: Document): String? {

        try {

            val scriptElements = doc.getElementsByTag("script") ?: return null


            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    val scriptText: String = dataNode.wholeData.replace("\\s+".toRegex(), "")

                    if (!scriptText.contains("RTPPlayer({")) continue

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

        return TSPlaylist().add("DEFAULT", tsPlaylist)
    }
}