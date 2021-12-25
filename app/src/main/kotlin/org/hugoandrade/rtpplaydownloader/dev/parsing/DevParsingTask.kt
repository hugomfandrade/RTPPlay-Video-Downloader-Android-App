package org.hugoandrade.rtpplaydownloader.dev.parsing

import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask
import org.jsoup.nodes.Document

class DevParsingTask : ParsingTask {

    companion object {
        const val URL = "dev.com"
    }

    override fun isUrlSupported(url: String): Boolean {
        return URL == url
    }

    override fun isValid(url: String): Boolean {
        return isUrlSupported(url)
    }

    override fun isValid(doc: Document): Boolean {
        val url = doc.baseUri()
        return isUrlSupported(url)
    }

    override fun parseMediaUrl(doc: Document): String {
        return URL
    }

    override fun parseMediaFile(url: String): ParsingData {
        return ParsingData(url, URL, URL, null)
    }

    override fun parseMediaFile(doc: Document): ParsingData {
        return ParsingData(doc.baseUri(), URL, URL, null)
    }
}