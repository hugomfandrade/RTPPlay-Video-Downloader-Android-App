package org.hugoandrade.rtpplaydownloader.network.parsing.tasks

import org.jsoup.nodes.Document

class EmptyParsingTask : ParsingTask() {

    override fun parseMediaUrl(doc: Document): String? {
        return null
    }

    override fun isValid(url: String) : Boolean {

        return false
    }
}