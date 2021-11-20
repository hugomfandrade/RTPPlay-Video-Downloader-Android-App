package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

@Deprecated(message = "use a more recent RTPPlay parser")
open class RTPPlayParsingTaskV6 : RTPPlayParsingTaskV5() {

    // get playlist url
    override fun parseMediaUrl(doc: Document): String? {

        val scriptElements = doc.getElementsByTag("script") ?: return null

        for (scriptElement in scriptElements.iterator()) {

            for (dataNode: DataNode in scriptElement.dataNodes()) {

                if (!dataNode.wholeData.contains("RTPPlayer")) continue

                val scriptText: String = dataNode.wholeData.replace("\\s+".toRegex(), "")

                try {

                    val rtpPlayerSubString: String = scriptText
                    val from = "hls:decodeURIComponent("
                    val to = ".join(\"\"))"

                    if (rtpPlayerSubString.indexOf(from) >= 0) {
                        val indexFrom = ParsingUtils.indexOfEx(rtpPlayerSubString, from)

                        val fileKeyAsString = rtpPlayerSubString.substring(indexFrom, indexFrom + rtpPlayerSubString.substring(indexFrom).indexOf(to))

                        val jsonArray : JsonArray = JsonParser().parse(fileKeyAsString).asJsonArray ?: continue

                        val link = StringBuilder()

                        for (i in 0 until jsonArray.size()) {
                            val item = jsonArray.get(i).asString

                            link.append(item)
                        }

                        return ParsingUtils.decode(link.toString())
                    }
                } catch (parsingException: java.lang.Exception) {
                    parsingException.printStackTrace()
                }
            }
        }

        return null
    }
}