package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.NetworkUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.Charset

class TVIPlayerParsingTask : ParsingTaskBase() {

    override fun parseMediaFile(url: String): Boolean {

        this.url = url
        this.mediaUrl = getM3U8File(url)?: return false
        this.filename = MediaUtils.getUniqueFilenameAndLock(getMediaFileName(url, mediaUrl))
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

        val isFileType: Boolean = url.contains("tviplayer.iol.pt")

        if (isFileType) {

            val videoFile: String? = getM3U8File(url)

            return videoFile != null
        }

        return false
    }

    private fun getM3U8File(url: String): String? {

        try {
            val jwiol = getJWIOL() ?: return null
            val m3u8Url = getM3U8ChunkUrl(url) ?: return null
            val m3u8 = "$m3u8Url?wmsAuthSign=$jwiol"

            return m3u8
        }
        catch (e: Exception) {
            return null
        }
    }

    override fun getMediaFileName(url: String, videoFile: String?): String {
        try {
            val doc: Document?

            try {
                doc = Jsoup.connect(url).timeout(10000).get()
            } catch (ignored: SocketTimeoutException) {
                return videoFile ?: url
            }

            val titleElements = doc?.getElementsByTag("title")

            if (videoFile != null && titleElements != null && titleElements.size > 0) {

                val title: String = MediaUtils.getTitleAsFilename(titleElements.elementAt(0).text())


                return "$title.ts"
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return videoFile?:url
    }


    private fun getThumbnailPath(url: String): String? {
        try {
            val doc = Jsoup.connect(url).timeout(10000).get()
            val scriptElements = doc.getElementsByTag("script")
            if (scriptElements != null) {
                for (element in scriptElements) {
                    for (dataNode in element.dataNodes()) {
                        val scriptText = dataNode.wholeData
                        if (scriptText.contains("$('#player').iolplayer({")) {
                            val scriptTextStart = scriptText.substring(indexOfEx(scriptText, "\"cover\":\""))
                            return scriptTextStart.substring(0, scriptTextStart.indexOf("\""))
                        }
                    }
                }
            }
        } catch (ignored: java.lang.Exception) {
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

    private fun getM3U8ChunkUrl(url: String): String? {
        try {
            val doc = Jsoup.connect(url).timeout(10000).get()
            val scriptElements = doc.getElementsByTag("script")
            if (scriptElements != null) {
                for (element in scriptElements) {
                    for (dataNode in element.dataNodes()) {
                        val scriptText = dataNode.wholeData
                        if (scriptText.contains("$('#player').iolplayer({")) {
                            val scriptTextStart = scriptText.substring(indexOfEx(scriptText, "\"videoUrl\":\""))
                            return scriptTextStart.substring(0, scriptTextStart.indexOf("\""))
                        }
                    }
                }
            }
        } catch (ignored: java.lang.Exception) {
        }
        return null
    }

    private fun indexOfEx(string : String, subString: String) : Int {
        if (string.contains(subString)) {
            return string.indexOf(subString) + subString.length
        }
        return 0
    }
}