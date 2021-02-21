package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.io.StringReader


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

                        return decode(link.toString())
                    }
                } catch (parsingException: java.lang.Exception) {
                    parsingException.printStackTrace()
                }
            }
        }

        return null
    }

    private fun decode(link : String) : String {

        val decodedLink = StringBuilder()

        var i = 0
        while (i < link.length)  {
            val char = link[i]

            if (char == '%') {
                decodedLink.append(decodeSymbol(link.substring(i, i + 3)))
                i += 3
            }
            else {
                decodedLink.append(char)
                i++
            }
        }

        return decodedLink.toString()


    }

    private fun decodeSymbol(symbol: String): Char {
        return when(symbol) {
            "%21" -> '!'
            "%23" -> '#'
            "%24" -> '$'
            "%26" -> '&'
            "%27" -> '\''
            "%28" -> '('
            "%29" -> ')'
            "%2A" -> '*'
            "%2B" -> '+'
            "%2C" -> ','

            "%2F" -> '/'
            "%3A" -> ':'
            "%3B" -> ';'
            "%3D" -> '='
            "%3F" -> '?'
            "%40" -> '@'
            "%5B" -> '['
            "%5D" -> ']'

            "%0A" -> '\n'
            "%0D" -> '\n'

            "%20" -> ' '
            "%22" -> '"'
            "%25" -> '%'
            "%2D" -> '-'
            "%2E" -> '.'

            "%3C" -> '<'
            "%3E" -> '>'

            "%5C" -> '\\'
            "%5E" -> '^'
            "%5F" -> '_'
            "%60" -> '`'
            "%7B" -> '{'
            "%7C" -> '|'
            "%7D" -> '}'
            "%7E" -> '~'

            else -> ' '
        }
    }
}