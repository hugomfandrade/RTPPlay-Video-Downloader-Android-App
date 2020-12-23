package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

open class SICParsingTaskV3 : TSParsingTask() {

    override fun isValid(url: String) : Boolean {

        val isFileType: Boolean =
                url.contains("sicradical.sapo.pt") ||
                url.contains("sicradical.pt") ||
                url.contains("sicnoticias.sapo.pt") ||
                url.contains("sicnoticias.pt") ||
                url.contains("sic.sapo.pt") ||
                url.contains("sic.pt")

        return isFileType || super.isValid(url)
    }

    // get ts playlist
    override fun parseMediaUrl(doc: Document): String? {

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

    override fun parseM3U8Playlist(): TSPlaylist? {
        val playlistUrl = mediaUrl ?: return null

        return TSUtils.getCompleteM3U8Playlist(playlistUrl)
    }

    override fun parseMediaFileName(doc: Document): String {
        return RTPPlayUtils.getMediaFileName(doc, url ?: "", mediaUrl)
                .replace("SIC.Noticias.", "")
                .replace("SIC.Radical.", "")
                .replace("SIC.", "") + ".ts"
    }
}