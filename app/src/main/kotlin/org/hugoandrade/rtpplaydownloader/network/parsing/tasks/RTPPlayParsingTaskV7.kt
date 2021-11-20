package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.apache.commons.codec.binary.Base64
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

@Deprecated(message = "use a more recent RTPPlay parser")
open class RTPPlayParsingTaskV7 : RTPPlayParsingTaskV6() {

    // get playlist url
    override fun parseMediaUrl(doc: Document): String? {

        val scriptElements = doc.getElementsByTag("script") ?: return null


        for (scriptElement in scriptElements.iterator()) {

            for (dataNode: DataNode in scriptElement.dataNodes()) {

                if (!dataNode.wholeData.contains("RTPPlayer")) continue

                val scriptText: String = dataNode.wholeData.replace("\\s+".toRegex(), "")

                try {
                    System.err.println(scriptText)

                    val rtpPlayerSubString: String = scriptText
                    val from = "hls:atob(decodeURIComponent("
                    val to = ".join(\"\"))"

                    if (rtpPlayerSubString.indexOf(from) >= 0) {
                        val indexFrom = ParsingUtils.indexOfEx(rtpPlayerSubString, from)

                        val fileKeyAsString = rtpPlayerSubString.substring(indexFrom, indexFrom + rtpPlayerSubString.substring(indexFrom).indexOf(to))

                        System.err.println(fileKeyAsString)

                        val jsonArray : JsonArray = JsonParser().parse(fileKeyAsString).asJsonArray ?: continue

                        val link = StringBuilder()

                        for (i in 0 until jsonArray.size()) {
                            val item = jsonArray.get(i).asString

                            link.append(item)
                        }

                        val fullLink = link.toString()

                        return String(Base64.decodeBase64(ParsingUtils.decode(fullLink).toByteArray()))
                    }
                } catch (parsingException: java.lang.Exception) {
                    parsingException.printStackTrace()
                }
            }
        }

        return null
    }
}