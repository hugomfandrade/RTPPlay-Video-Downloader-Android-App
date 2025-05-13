package org.hugoandrade.downloader.parsing.tasks

import org.hugoandrade.downloader.parsing.ParsingUtils
import org.jsoup.nodes.Document

// still useful for audio files
abstract class RTPPlayParsingTask : ParsingTask {

    override fun isUrlSupported(url: String): Boolean {
        return url.contains("www.rtp.pt/play")
    }

    override fun parseThumbnailPath(doc: Document): String? {
        return ParsingUtils.getThumbnailPath(doc)
    }

    override fun parseMediaFileName(doc: Document, mediaUrl: String): String {
        return super.parseMediaFileName(doc, mediaUrl)
                .replace(".RTP.Play.RTP", "")
    }
}