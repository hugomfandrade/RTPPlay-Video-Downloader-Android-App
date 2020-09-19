package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.io.IOException

open class RTPPlayParsingTaskV3 : RTPPlayParsingTask() {

    override fun getVideoFile(url: String): String? {
        return getM3U8File(url)
    }

    private fun getM3U8File(url: String): String? {
        val doc: Document

        try {
            doc = Jsoup.connect(url).timeout(10000).get()
        } catch (ignored: IOException) {
            return null
        }

        try {

            val scriptElements = doc.getElementsByTag("script") ?: return null


            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    val scriptText: String = dataNode.wholeData

                    if (!scriptText.contains("RTPPlayer({")) continue

                    try {

                        val rtpPlayerSubString: String = scriptText

                        for (i in arrayOf("file: \"", "file : \"")) {
                            if (rtpPlayerSubString.indexOf(i) >= 0) {
                                return rtpPlayerSubString.substring(
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, i),
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, i) + rtpPlayerSubString.substring(ParsingUtils.indexOfEx(rtpPlayerSubString, i)).indexOf("\","))

                            }
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
}