package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.jsoup.nodes.Document

abstract class RTPPlayTSParsingTask : TSParsingTask() {

    override fun isUrlSupported(url: String): Boolean {

        return url.contains("www.rtp.pt/play")
    }

    override fun parseThumbnailPath(doc: Document): String? {
        return ParsingUtils.getThumbnailPath(doc)
    }

    override fun parseMediaFileName(doc: Document): String {
        return super.parseMediaFileName(doc)
                .replace(".RTP.Play.RTP", "") + ".ts"
    }
}