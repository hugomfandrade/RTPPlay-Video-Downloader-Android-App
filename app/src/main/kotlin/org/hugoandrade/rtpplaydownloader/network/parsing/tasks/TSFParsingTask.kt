package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

class TSFParsingTask : ParsingTask() {

    override fun isValid(url: String) : Boolean {

        val isFileType: Boolean =
                url.contains("tsf.pt")

        return isFileType || super.isValid(url)
    }

    override fun parseMediaUrl(doc: Document): String? {
        try {

            val scriptElements = doc.getElementsByTag("script")?: return null

            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    val scriptText: String = dataNode.wholeData

                    if (!scriptText.contains("@context")) continue
                    if (!scriptText.contains("@type")) continue
                    if (!scriptText.contains("VideoObject")) continue
                    if (!scriptText.contains("contentUrl")) continue


                    try {

                        val from = "\"contentUrl\": \""
                        val to = "\""

                        val link: String = scriptText.substring(
                                ParsingUtils.indexOfEx(scriptText, from),
                                ParsingUtils.indexOfEx(scriptText, from) + scriptText.substring(ParsingUtils.indexOfEx(scriptText, from)).indexOf(to))

                        return link
                    } catch (parsingException: java.lang.Exception) {
                        parsingException.printStackTrace()
                    }
                }
            }
        }
        catch (e : java.lang.Exception) {
            e.printStackTrace()
        }

        return null
    }
}