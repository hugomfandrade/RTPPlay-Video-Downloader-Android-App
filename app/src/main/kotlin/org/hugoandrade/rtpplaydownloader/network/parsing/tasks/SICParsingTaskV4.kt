package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils.Companion.indexOfEx
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

open class SICParsingTaskV4 : SICParsingTask() {

    override fun parseMediaUrl(doc: Document): String? {
        try {

            val scriptElements = doc.getElementsByTag("script") ?: return null

            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    val scriptText: String = dataNode.wholeData.replace("\\s+".toRegex(), "")

                    if (!scriptText.contains("jwplayer")) continue
                    if (!scriptText.contains("'playlist'")) continue
                    if (!scriptText.contains("'file'")) continue

                    try {

                        val jwPlayerSubString: String = scriptText.substring(indexOfEx(scriptText, "[{"), scriptText.lastIndexOf("}]"))

                        val from = "'file':\""
                        val to = "\","

                        if (jwPlayerSubString.indexOf(from) >= 0) {

                            val startIndex = indexOfEx(jwPlayerSubString, from)

                            val link: String = jwPlayerSubString.substring(
                                    startIndex,
                                    startIndex + jwPlayerSubString.substring(startIndex).indexOf(to))

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

    override fun parseMediaFileName(doc: Document): String {
        return RTPPlayUtils.getMediaFileName(doc, url?: null.toString(), mediaUrl)
    }
}