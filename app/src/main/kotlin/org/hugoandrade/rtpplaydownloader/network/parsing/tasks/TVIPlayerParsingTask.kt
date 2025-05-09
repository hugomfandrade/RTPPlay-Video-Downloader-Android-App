package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.download.TSUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.hugoandrade.rtpplaydownloader.network.parsing.TSPlaylist
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.jsoup.nodes.Document
import java.io.*
import java.net.URL
import java.nio.charset.Charset

class TVIPlayerParsingTask : TSParsingTask {

    override fun isUrlSupported(url: String): Boolean {

        return url.contains("tviplayer.iol.pt")
    }

    override fun parseMediaUrl(doc: Document): String? {

        try {
            val m3u8Url = getM3U8ChunkUrl(doc) ?: return null
            val jwiol = getJWIOL() ?: return null
            val m3u8 = "$m3u8Url?wmsAuthSign=$jwiol"

            return m3u8
        }
        catch (e: Exception) {
            return null
        }
    }

    override fun parseM3U8Playlist(m3u8: String): TSPlaylist? {
        return TSUtils.getCompleteM3U8Playlist(m3u8)
    }

    override fun parseMediaFileName(doc: Document, mediaUrl: String): String {
        try {
            val titleElements = doc.getElementsByTag("title")

            if (titleElements != null && titleElements.size > 0) {

                val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())


                return "$title.ts"
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return mediaUrl
    }

    override fun parseThumbnailPath(doc: Document): String? {
        try {
            val scriptElements = doc.getElementsByTag("script") ?: return null

            for (element in scriptElements) {
                for (dataNode in element.dataNodes()) {
                    val scriptText = dataNode.wholeData
                    if (scriptText.contains("$('#player').iolplayer({")) continue

                    val scriptTextStart = scriptText.substring(ParsingUtils.indexOfEx(scriptText, "\"cover\":\""))
                    return scriptTextStart.substring(0, scriptTextStart.indexOf("\""))
                }
            }
        } catch (ignored: java.lang.Exception) {
            ignored.printStackTrace()
        }
        return null
    }

    private fun getJWIOL(): String? {
        try {
            val tokenUrl = "https://services.iol.pt/matrix?userId="
            val inputStream: InputStream = URL(tokenUrl).openStream()
            val textBuilder = StringBuilder()
            BufferedReader(InputStreamReader(inputStream, Charset.forName("UTF-8"))).use { reader ->
                var c: Int = reader.read()
                while (c != -1) {
                    textBuilder.append(c.toChar())
                    c = reader.read()
                }
            }
            return textBuilder.toString()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getM3U8ChunkUrl(doc: Document): String? {
        try {
            val scriptElements = doc.getElementsByTag("script") ?: return null

            for (element in scriptElements) {
                for (dataNode in element.dataNodes()) {
                    val scriptText = dataNode.wholeData
                    if (!scriptText.contains("$('#player').iolplayer({")) continue

                    val scriptTextStart = scriptText.substring(ParsingUtils.indexOfEx(scriptText, "\"videoUrl\":\""))
                    return scriptTextStart.substring(0, scriptTextStart.indexOf("\""))
                }
            }
        } catch (ignored: java.lang.Exception) {
            ignored.printStackTrace()
        }
        return null
    }
}