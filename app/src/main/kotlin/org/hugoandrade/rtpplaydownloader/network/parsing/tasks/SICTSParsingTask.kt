package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingUtils
import org.jsoup.nodes.Document

abstract class SICTSParsingTask : TSParsingTask() {

    override fun isUrlSupported(url: String): Boolean {

        return url.contains("sicradical.sapo.pt") ||
                url.contains("sicradical.pt") ||
                url.contains("sicnoticias.sapo.pt") ||
                url.contains("sicnoticias.pt") ||
                url.contains("sic.sapo.pt") ||
                url.contains("sic.pt")
    }

    override fun parseMediaFileName(doc: Document): String {
        return ParsingUtils.getMediaFileName(doc, url ?: "", mediaUrl)
                .replace("SIC.Noticias.", "")
                .replace("SIC.Radical.", "")
                .replace("SIC.", "") + ".ts"
    }

    override fun parseThumbnailPath(doc: Document): String? {
        val filename = super.parseThumbnailPath(doc)

        return if (filename.isNullOrEmpty()) {
            ParsingUtils.getThumbnailFromTwitterMetadata(doc) ?: filename
        } else {
            filename
        }
    }
}