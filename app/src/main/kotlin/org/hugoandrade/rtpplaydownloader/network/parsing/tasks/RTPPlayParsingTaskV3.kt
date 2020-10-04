package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.io.IOException

open class RTPPlayParsingTaskV3 : RTPPlayParsingTask(), TSParsingTask {

    private var playlist: TSPlaylist? = null

    override fun parseMediaFile(url: String): Boolean {
        val parsed = super.parseMediaFile(url)

        playlist = getM3U8Files(mediaUrl)

        return parsed
    }

    override fun getVideoFile(url: String): String? {
        return getM3U8Playlist(url)
    }

    override fun getTSPlaylist(): TSPlaylist? {
        return playlist
    }

    private fun getM3U8Playlist(url: String): String? {
        val doc: Document

        try {
            doc = Jsoup.connect(url).timeout(10000).get()
        } catch (ignored: IOException) {
            return null
        }

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

    private fun getM3U8Files(playlistUrl: String?): TSPlaylist? {
        if (playlistUrl == null) return null

        return TSPlaylist().add("DEFAULT", playlistUrl)
    }
}