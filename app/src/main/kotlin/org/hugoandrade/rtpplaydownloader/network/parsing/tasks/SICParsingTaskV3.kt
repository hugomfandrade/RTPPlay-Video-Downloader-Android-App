package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

open class SICParsingTaskV3 : SICParsingTaskV2(), TSParsingTask {

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
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(url).timeout(10000).get()
            } catch (ignored: IOException) {
                return null
            }

            val scriptElements = doc?.getElementsByTag("script") ?: return null

            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    val scriptText: String = dataNode.wholeData

                    if (!scriptText.contains("jwplayer")) continue
                    if (!scriptText.contains("'playlist'")) continue
                    if (!scriptText.contains("'file'")) continue
                    if (!scriptText.contains(".m3u8")) continue

                    try {

                        val jwPlayerSubString: String = scriptText.substring(ParsingUtils.indexOfEx(scriptText, "[{"), scriptText.lastIndexOf("}]"))

                        val from = "'file': \""
                        val to = "\","

                        if (jwPlayerSubString.indexOf(from) >= 0) {

                            val link: String = jwPlayerSubString.substring(
                                    ParsingUtils.indexOfEx(jwPlayerSubString, from),
                                    ParsingUtils.indexOfEx(jwPlayerSubString, from) + jwPlayerSubString.substring(ParsingUtils.indexOfEx(jwPlayerSubString, from)).indexOf(to))

                            return link
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

        return TSUtils.getCompleteM3U8Playlist(playlistUrl)
    }

    override fun getMediaFileName(url: String, videoFile: String?): String {
        return RTPPlayUtils.getMediaFileName(url, videoFile)
                .replace("SIC.Noticias.", "")
                .replace("SIC.Radical.", "")
                .replace("SIC.", "") + ".ts"
    }
}