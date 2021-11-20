package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.apache.commons.codec.binary.Base64
import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

open class RTPPlayParsingTaskV8 : RTPPlayTSParsingTask() {

    // get playlist url
    override fun parseMediaUrl(doc: Document): String? {

        val scriptElements = doc.getElementsByTag("script") ?: return null

        val availables = scriptElements.stream()
                .map { scriptElement -> scriptElement.dataNodes() }
                .flatMap { dataNode -> dataNode.stream() }
                .map { dataNode -> dataNode.wholeData }
                .filter { scriptText -> scriptText.contains("RTPPlayer") }

        for (scriptElement in scriptElements.iterator()) {

            for (dataNode: DataNode in scriptElement.dataNodes()) {

                if (!dataNode.wholeData.contains("RTPPlayer")) continue

                var scriptText: String = dataNode.wholeData.replace("\\s+".toRegex(), "")

                try {

                    scriptText = scriptText.substring(scriptText.lastIndexOf("varf={hls"))

                    val rtpPlayerSubString: String = scriptText
                    val from = "hls:atob(decodeURIComponent("
                    val to = ".join(\"\"))"

                    if (rtpPlayerSubString.indexOf(from) < 0) continue

                    val indexFrom = ParsingUtils.indexOfEx(rtpPlayerSubString, from)

                    val fileKeyAsString = rtpPlayerSubString.substring(indexFrom, indexFrom + rtpPlayerSubString.substring(indexFrom).indexOf(to))

                    val jsonArray : JsonArray = JsonParser().parse(fileKeyAsString).asJsonArray ?: continue

                    val link = StringBuilder()

                    for (i in 0 until jsonArray.size()) {
                        val item = jsonArray.get(i).asString

                        link.append(item)
                    }

                    val fullLink = link.toString()

                    return String(Base64.decodeBase64(ParsingUtils.decode(fullLink).toByteArray()))

                } catch (parsingException: java.lang.Exception) {
                    parsingException.printStackTrace()
                }
            }
        }

        return null
    }

    override fun parseM3U8Playlist(): TSPlaylist? {
        //
        val m3u8: String = mediaUrl ?: return null

        val playlist : TSPlaylist? = TSUtils.getCompleteM3U8PlaylistWithoutBaseUrl(m3u8)

        playlist?.getTSUrls()?.forEach{ tsUrl ->
            tsUrl.url = m3u8.substringBeforeLast("/") + "/" + tsUrl.url
        }

        // TODO
        // update mediaUrl fields for now for compatibility reasons
        mediaUrl = playlist?.getTSUrls()?.firstOrNull()?.url ?: mediaUrl

        return playlist
    }
}