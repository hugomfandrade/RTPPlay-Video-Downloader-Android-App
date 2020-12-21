package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

open class SICParsingTaskV3 : SICParsingTaskV2(), TSParsingTask {

    private var playlist: TSPlaylist? = null

    override fun parseMediaFile(doc: Document): Boolean {
        val parsed = super.parseMediaFile(doc)

        playlist = getM3U8Files(mediaUrl)

        return parsed
    }

    override fun getMediaUrl(doc: Document): String? {
        return getM3U8Playlist(doc)
    }

    override fun getTSPlaylist(): TSPlaylist? {
        return playlist
    }

    private fun getM3U8Playlist(doc: Document): String? {
        try {

            val scriptElements = doc.getElementsByTag("script") ?: return null

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

    override fun getMediaFileName(doc: Document): String {
        return RTPPlayUtils.getMediaFileName(doc, url ?: "", mediaUrl)
                .replace("SIC.Noticias.", "")
                .replace("SIC.Radical.", "")
                .replace("SIC.", "") + ".ts"
    }
}