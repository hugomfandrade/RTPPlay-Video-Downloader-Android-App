package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSParsingTask
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

open class SICParsingTaskV3 : TSParsingTask() {

    override fun isUrlSupported(url: String) : Boolean {

        val isFileType: Boolean =
                url.contains("sicradical.sapo.pt") ||
                url.contains("sicradical.pt") ||
                url.contains("sicnoticias.sapo.pt") ||
                url.contains("sicnoticias.pt") ||
                url.contains("sic.sapo.pt") ||
                url.contains("sic.pt")

        return isFileType || super.isUrlSupported(url)
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

                            val startIndex = ParsingUtils.indexOfEx(jwPlayerSubString, from)

                            val link: String = jwPlayerSubString.substring(
                                    startIndex,
                                    startIndex+ jwPlayerSubString.substring(startIndex).indexOf(to))

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
        //
        val m3u8: String = mediaUrl ?: return null

        val playlist = TSUtils.getCompleteM3U8Playlist(m3u8)

        // TODO
        // update mediaUrl fields for now for compatibility reasons
        mediaUrl = playlist?.getTSUrls()?.firstOrNull()?.url ?: mediaUrl

        return playlist
    }

    override fun parseMediaFileName(doc: Document): String {
        return ParsingUtils.getMediaFileName(doc, url ?: "", mediaUrl)
                .replace("SIC.Noticias.", "")
                .replace("SIC.Radical.", "")
                .replace("SIC.", "") + ".ts"
    }

    override fun parseThumbnailPath(doc: Document): String? {
        val filename = super.parseThumbnailPath(doc)

        return if (filename.isNullOrEmpty()) {
            ParsingUtils.getThumbnailFromTwitterMetadata(doc) ?: filename
        } else {
            filename
        }
    }
}