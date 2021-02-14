package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document

// still useful for audio files
open class RTPPlayParsingTask : ParsingTask() {

    override fun isUrlSupported(url: String) : Boolean {

        val isFileType: Boolean = url.contains("www.rtp.pt/play")

        return isFileType || super.isUrlSupported(url)
    }

    override fun parseMediaUrl(doc: Document): String? {

        try {
            val scriptElements = doc.getElementsByTag("script")?: return null


            for (scriptElement in scriptElements.iterator()) {

                for (dataNode: DataNode in scriptElement.dataNodes()) {

                    if (!dataNode.wholeData.contains("RTPPlayer")) continue

                    val scriptText: String = dataNode.wholeData.replace("\\s+".toRegex(), "")

                    try {

                        val rtpPlayerSubString: String = scriptText.substring(ParsingUtils.indexOfEx(scriptText, "RTPPlayer({"), scriptText.lastIndexOf("})"))

                        if (rtpPlayerSubString.indexOf(".mp4") >= 0) {  // is video file

                            if (rtpPlayerSubString.indexOf("fileKey:\"") >= 0) {

                                val link: String = rtpPlayerSubString.substring(
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, "fileKey:\""),
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, "fileKey:\"") + rtpPlayerSubString.substring(ParsingUtils.indexOfEx(rtpPlayerSubString, "fileKey:\"")).indexOf("\","))


                                return "http://cdn-ondemand.rtp.pt$link"
                            }

                        } else if (rtpPlayerSubString.indexOf(".mp3") >= 0) { // is audio file

                            if (rtpPlayerSubString.indexOf("file:\"") >= 0) {

                                return rtpPlayerSubString.substring(
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, "file:\""),
                                        ParsingUtils.indexOfEx(rtpPlayerSubString, "file:\"") + rtpPlayerSubString.substring(ParsingUtils.indexOfEx(rtpPlayerSubString, "file:\"")).indexOf("\","))

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

    override fun parseThumbnailPath(doc: Document): String? {
        return ParsingUtils.getThumbnailPath(doc)
    }

    override fun parseMediaFileName(doc: Document): String {
        return super.parseMediaFileName(doc)
                .replace(".RTP.Play.RTP", "")
    }
}